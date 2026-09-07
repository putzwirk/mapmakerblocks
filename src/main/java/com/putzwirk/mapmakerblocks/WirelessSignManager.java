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
import net.minecraft.block.entity.SignText;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.Collections;
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

        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> INSTANCE.onChunkLoad(world, chunk));
        ServerChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> INSTANCE.onChunkUnload(world, chunk));

        ServerWorldEvents.LOAD.register((server, world) -> INSTANCE.onWorldLoad(world));

        ServerTickEvents.END_SERVER_TICK.register(server -> INSTANCE.tick(server));
    }

    private final Map<ServerWorld, Set<BlockPos>> knownSigns = new HashMap<>();

    private final Map<ServerWorld, Set<BlockPos>> poweredSigns = new HashMap<>();

    private final Set<ServerWorld> worldsNeedingUpdate = new HashSet<>();
    private boolean isRecalculating = false;

    private static final int RECALC_INTERVAL = 5;

    public void onNeighborUpdate(ServerWorld world, BlockPos pos) {
        if (isWirelessSign(world, pos)) {
            knownSigns.computeIfAbsent(world, w -> new HashSet<>()).add(pos.toImmutable());
            if (!isRecalculating) {
                worldsNeedingUpdate.add(world);
            }
        }
    }

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

            List<BlockPos> positions = new ArrayList<>(known);

            for (BlockPos pos : positions) {
                ensureGlowing(world, pos);
            }

            Set<String> activeLines = new HashSet<>();
            for (BlockPos pos : positions) {
                if (isInputSign(world, pos) && hasRedstoneInput(world, pos)) {
                    activeLines.addAll(getChannels(world, pos));
                }
            }

            Set<BlockPos> targetPowered = new HashSet<>();
            if (!activeLines.isEmpty()) {
                for (BlockPos pos : positions) {
                    if (!isInputSign(world, pos) && !Collections.disjoint(activeLines, getChannels(world, pos))) {
                        targetPowered.add(pos);
                    }
                }
            }

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

    private static void ensureGlowing(ServerWorld world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof SignBlockEntity signBE)) return;
        if (signBE.getFrontText().isGlowing() && signBE.getBackText().isGlowing()) return;
        signBE.setText(signBE.getFrontText().withGlowing(true), true);
        signBE.setText(signBE.getBackText().withGlowing(true), false);
    }

    private static boolean isInputSign(ServerWorld world, BlockPos pos) {
        return world.getBlockState(pos).get(WirelessRedstoneSignBlock.INPUT);
    }

    private static Set<String> getChannels(ServerWorld world, BlockPos pos) {
        Set<String> channels = new HashSet<>();
        if (world.getBlockEntity(pos) instanceof SignBlockEntity signBE) {
            collectChannels(signBE.getFrontText(), channels);
            collectChannels(signBE.getBackText(), channels);
        }
        return channels;
    }

    private static void collectChannels(SignText text, Set<String> into) {
        for (int i = 0; i < 4; i++) {
            String line = text.getMessage(i, false).getString().strip();
            if (!line.isEmpty()) {
                into.add(line);
            }
        }
    }

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
                return true;
            }
        }
        return false;
    }

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