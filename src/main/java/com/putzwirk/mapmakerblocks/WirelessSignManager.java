package com.putzwirk.mapmakerblocks;

import com.putzwirk.mapmakerblocks.block.WirelessRedstoneSignBlock;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.impl.event.lifecycle.LoadedChunksCache;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WirelessSignManager {

    private static WirelessSignManager INSTANCE = new WirelessSignManager();

    private WirelessSignManager() {}

    public static WirelessSignManager get() {
        return INSTANCE;
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> INSTANCE = new WirelessSignManager());

        // Chunks loaded from disk (including when a server starts) never fire
        // onBlockAdded/neighborUpdate, so rediscover signs by scanning chunks as they load.
        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> INSTANCE.onChunkLoad(world, chunk));
        ServerChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> INSTANCE.onChunkUnload(world, chunk));

        // Safety net: re-scan whatever a world already has loaded the moment it comes up
        // (spawn region), in case its chunks were loaded before the chunk events fired.
        ServerWorldEvents.LOAD.register((server, world) -> INSTANCE.onWorldLoad(world));

        ServerTickEvents.END_SERVER_TICK.register(server -> INSTANCE.tick(server));
    }

    private final Map<ServerWorld, Set<BlockPos>> knownSigns = new HashMap<>();

    private final Map<ServerWorld, Set<BlockPos>> poweredSigns = new HashMap<>();

    private final Set<ServerWorld> worldsNeedingUpdate = new HashSet<>();
    private boolean isRecalculating = false;

    // Recalculate regularly even when no redstone event reached a sign. While an Input
    // (receiver) sign emits, its own output keeps the adjacent dust lit, which masks the
    // neighbor update that would otherwise tell us a transmitter lost its lever signal.
    private static final int RECALC_INTERVAL = 5;

    public void onNeighborUpdate(ServerWorld world, BlockPos pos) {
        if (isWirelessSign(world, pos)) {
            knownSigns.computeIfAbsent(world, w -> new HashSet<>()).add(pos.toImmutable());
            if (!isRecalculating) {
                worldsNeedingUpdate.add(world);
            }
        }
    }

    /** Called on the server when a player picks a mode in the sign GUI. */
    public void setMode(ServerWorld world, BlockPos pos, boolean input) {
        if (!isWirelessSign(world, pos)) return;
        BlockState state = world.getBlockState(pos);
        if (state.get(WirelessRedstoneSignBlock.INPUT) == input) return;

        world.setBlockState(pos, state.with(WirelessRedstoneSignBlock.INPUT, input));
        world.updateNeighborsAlways(pos, ModBlocks.WIRELESS_REDSTONE_SIGN);
        onNeighborUpdate(world, pos);
    }

    private void onWorldLoad(ServerWorld world) {
        for (WorldChunk chunk : ((LoadedChunksCache) world).fabric_getLoadedChunks()) {
            onChunkLoad(world, chunk);
        }
    }

    private void onChunkLoad(ServerWorld world, WorldChunk chunk) {
        boolean found = false;
        for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
            // Query the state straight off the chunk, NOT via world.getBlockState: that
            // routes through ServerChunkManager.getChunkFutureMainThread and would block the
            // server thread waiting on the very chunk full-future still being promoted
            // here, deadlocking during world load.
            if (entry.getValue() instanceof SignBlockEntity
                    && chunk.getBlockState(entry.getKey()).isOf(ModBlocks.WIRELESS_REDSTONE_SIGN)) {
                knownSigns.computeIfAbsent(world, w -> new HashSet<>()).add(entry.getKey().toImmutable());
                found = true;
            }
        }
        if (found && !isRecalculating) {
            worldsNeedingUpdate.add(world);
        }
    }

    private void onChunkUnload(ServerWorld world, WorldChunk chunk) {
        Set<BlockPos> known = knownSigns.get(world);
        if (known == null) return;
        for (BlockPos pos : chunk.getBlockEntities().keySet()) {
            known.remove(pos);
        }
    }

    public boolean isPowered(ServerWorld world, BlockPos pos) {
        Set<BlockPos> set = poweredSigns.get(world);
        return set != null && set.contains(pos);
    }

    private void tick(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            Set<BlockPos> known = knownSigns.get(world);
            if (known != null && !known.isEmpty()
                    && (worldsNeedingUpdate.contains(world) || world.getTime() % RECALC_INTERVAL == 0)) {
                tickWorld(world);
            }
        }
        worldsNeedingUpdate.clear();
    }

    private void tickWorld(ServerWorld world) {
        isRecalculating = true;
        try {
            Set<BlockPos> known = knownSigns.computeIfAbsent(world, w -> new HashSet<>());

            known.removeIf(pos -> !isWirelessSign(world, pos));

            // Snapshot the set: setPoweredState below triggers neighbor updates, and a sign
            // reactivated by that redstone signal calls onNeighborUpdate, which must not mutate
            // the collection we are iterating.
            List<BlockPos> positions = new ArrayList<>(known);

            // An Input (detector) sign broadcasts its channel (sign text) while it receives
            // a physical redstone signal.
            Set<String> activeChannels = new HashSet<>();
            for (BlockPos pos : positions) {
                if (isInputSign(world, pos) && hasRedstoneInput(world, pos)) {
                    activeChannels.add(getChannel(world, pos));
                }
            }

            // Output (emitter) signs whose channel matches a broadcasting Input sign are
            // powered and emit strength-15 redstone. Input signs never get POWERED, so only
            // Output signs physically emit redstone.
            Set<BlockPos> targetPowered = new HashSet<>();
            if (!activeChannels.isEmpty()) {
                for (BlockPos pos : positions) {
                    if (!isInputSign(world, pos) && activeChannels.contains(getChannel(world, pos))) {
                        targetPowered.add(pos);
                    }
                }
            }

            // Apply only the deltas against the real block state. This also self-heals a stale
            // POWERED=true left over in chunk data from a previous session.
            for (BlockPos pos : positions) {
                boolean shouldPower = targetPowered.contains(pos);
                boolean isPowered = world.getBlockState(pos).get(WirelessRedstoneSignBlock.POWERED);
                if (shouldPower != isPowered) {
                    setPoweredState(world, pos, shouldPower);
                }
            }

            Set<BlockPos> poweredSet = poweredSigns.computeIfAbsent(world, w -> new HashSet<>());
            poweredSet.clear();
            poweredSet.addAll(targetPowered);
        } finally {
            isRecalculating = false;
        }
    }

    private static boolean isWirelessSign(ServerWorld world, BlockPos pos) {
        return world.getBlockState(pos).isOf(ModBlocks.WIRELESS_REDSTONE_SIGN);
    }

    /** true = INPUT mode: this sign detects a wired redstone signal and broadcasts it. */
    private static boolean isInputSign(ServerWorld world, BlockPos pos) {
        return world.getBlockState(pos).get(WirelessRedstoneSignBlock.INPUT);
    }

    /** The sign's front text acts as the channel ID that pairs senders with receivers. */
    private static String getChannel(ServerWorld world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof SignBlockEntity signBE)) {
            return "";
        }
        String channel = "";
        for (int i = 0; i < 4; i++) {
            channel = channel.concat(signBE.getFrontText().getMessage(i, false).getString().strip());
        }
        return channel;
    }

    /**
     * True when the sign receives a physical redstone signal from a non-wireless-sign source.
     * Wireless signs are always skipped here, so one sign can never power another sign's input
     * through wires - broadcasts only flow through this manager. This is what stops a pair of
     * signs connected by dust from latching permanently on when the lever is removed.
     */
    private static boolean hasRedstoneInput(ServerWorld world, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbour = pos.offset(dir);
            BlockState neighbourState = world.getBlockState(neighbour);

            if (neighbourState.isOf(ModBlocks.WIRELESS_REDSTONE_SIGN)) {
                continue;
            }

            if (neighbourState.isOf(Blocks.REDSTONE_WIRE)) {
                if (dustFedByExternalSource(world, neighbour)) {
                    return true;
                }
            } else if (world.getEmittedRedstonePower(neighbour, dir.getOpposite()) > 0) {
                // Direct non-wire power sources (levers, repeaters, torches, powered blocks).
                return true;
            }
        }
        return false;
    }

    /**
     * A dust block only counts as a real input when some non-wireless-sign block feeds power
     * into it (a lever, torch, repeater or another dust). Dust lit solely by wireless signs -
     * i.e. a receiver's output - is ignored.
     */
    private static boolean dustFedByExternalSource(ServerWorld world, BlockPos dustPos) {
        for (Direction dir : Direction.values()) {
            BlockPos source = dustPos.offset(dir);
            if (isWirelessSign(world, source)) {
                continue;
            }
            if (world.getEmittedRedstonePower(source, dir.getOpposite()) > 0) {
                return true;
            }
        }
        return false;
    }

    private static void setPoweredState(ServerWorld world, BlockPos pos, boolean powered) {
        BlockState state = world.getBlockState(pos);
        if (!state.isOf(ModBlocks.WIRELESS_REDSTONE_SIGN)) return;
        if (state.get(WirelessRedstoneSignBlock.POWERED) == powered) return;

        world.setBlockState(pos, state.with(WirelessRedstoneSignBlock.POWERED, powered));
        world.updateNeighborsAlways(pos, ModBlocks.WIRELESS_REDSTONE_SIGN);
        for (Direction dir : Direction.values()) {
            world.updateNeighborsAlways(pos.offset(dir), ModBlocks.WIRELESS_REDSTONE_SIGN);
        }
    }
}