package com.putzwirk.mapmakerblocks;

import com.putzwirk.mapmakerblocks.block.WirelessRedstoneSignBlock;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.*;

public class WirelessSignManager {

    private static WirelessSignManager INSTANCE = new WirelessSignManager();

    private WirelessSignManager() {}

    public static WirelessSignManager get() {
        return INSTANCE;
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> INSTANCE = new WirelessSignManager());

        ServerTickEvents.END_SERVER_TICK.register(server -> INSTANCE.tick(server));
    }

    private final Map<ServerWorld, Set<BlockPos>> knownSigns = new HashMap<>();

    private final Map<ServerWorld, Set<BlockPos>> poweredSigns = new HashMap<>();

    public void onNeighborUpdate(ServerWorld world, BlockPos pos) {
        if (isWirelessSign(world, pos)) {
            knownSigns.computeIfAbsent(world, w -> new HashSet<>()).add(pos.toImmutable());
        }
    }

    public boolean isPowered(ServerWorld world, BlockPos pos) {
        Set<BlockPos> set = poweredSigns.get(world);
        return set != null && set.contains(pos);
    }

    private void tick(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            tickWorld(world);
        }
    }

    private void tickWorld(ServerWorld world) {
        Set<BlockPos> known = knownSigns.computeIfAbsent(world, w -> new HashSet<>());

        known.removeIf(pos -> !isWirelessSign(world, pos));

        // Silence every sign the world currently shows as broadcasting, based on the
        // real block state rather than our own bookkeeping. This is the important
        // part: any two (or more) signs that feed each other through redstone dust
        // form a feedback loop, and evaluating that loop correctly requires starting
        // from a known-zero baseline built from the *actual* propagated signal, not
        // from a cache that could disagree with the world (after a restart, after
        // discovery order differences, etc). Using our own cache here was the bug -
        // if it ever drifted from reality, a still-broadcasting sign wouldn't get
        // silenced, and its own leftover output would loop back as "real" input on
        // the very next check, permanently confirming itself.
        for (BlockPos pos : known) {
            if (world.getBlockState(pos).get(WirelessRedstoneSignBlock.POWERED)) {
                setPoweredState(world, pos, false);
            }
        }

        Set<String> activeChannels = new HashSet<>();
        for (BlockPos pos : known) {
            if (hasDirectRedstonePower(world, pos)) {
                String channel = getChannel(world, pos);
                if (!channel.isEmpty()) {
                    activeChannels.add(channel);
                }
            }
        }

        Set<BlockPos> targetPowered = new HashSet<>();
        if (!activeChannels.isEmpty()) {
            for (BlockPos pos : known) {
                String channel = getChannel(world, pos);
                if (activeChannels.contains(channel)) {
                    targetPowered.add(pos);
                }
            }
        }

        Set<BlockPos> poweredSet = poweredSigns.computeIfAbsent(world, w -> new HashSet<>());
        poweredSet.clear();
        for (BlockPos pos : targetPowered) {
            poweredSet.add(pos);
            setPoweredState(world, pos, true);
        }
    }

    private static boolean isWirelessSign(ServerWorld world, BlockPos pos) {
        return world.getBlockState(pos).isOf(ModBlocks.WIRELESS_REDSTONE_SIGN);
    }

    private static boolean hasDirectRedstonePower(ServerWorld world, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbour = pos.offset(dir);
            if (world.getEmittedRedstonePower(neighbour, dir) > 0) {
                return true;
            }
        }
        return false;
    }

    private static String getChannel(ServerWorld world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof SignBlockEntity signBE)) {
            return "";
        }
        SignText frontText = signBE.getFrontText();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            Text line = frontText.getMessage(i, false);
            sb.append(line.getString().strip());
        }
        return sb.toString();
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