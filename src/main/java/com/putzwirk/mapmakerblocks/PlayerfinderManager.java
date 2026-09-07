package com.putzwirk.mapmakerblocks;

import com.putzwirk.mapmakerblocks.block.PlayerfinderBlock;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class PlayerfinderManager {

    private static PlayerfinderManager INSTANCE = new PlayerfinderManager();

    private static final Direction[] AXES = Direction.values();

    private final Map<ServerWorld, Set<BlockPos>> poweredBlocks = new HashMap<>();

    private PlayerfinderManager() {}

    public static PlayerfinderManager get() {
        return INSTANCE;
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> INSTANCE = new PlayerfinderManager());

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            INSTANCE.tickPlayerDeparture(server);
        });
    }

    private static final int ONE_TIME_PULSE_TICKS = 2;
    private final Map<ServerWorld, Map<BlockPos, Long>> removalScheduled = new HashMap<>();

    private void tickPlayerDeparture(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            long now = world.getTime();

            Map<BlockPos, Long> worldRemoval = removalScheduled.get(world);
            if (worldRemoval != null && !worldRemoval.isEmpty()) {
                worldRemoval.entrySet().removeIf(entry -> {
                    if (now >= entry.getValue()) {
                        BlockPos pos = entry.getKey();
                        if (world.getBlockState(pos).isOf(ModBlocks.ONE_TIME_PLAYERFINDER)) {
                            world.removeBlock(pos, false);
                        }
                        return true;
                    }
                    return false;
                });
            }

            Set<BlockPos> occupiedBlocks = new HashSet<>();

            for (ServerPlayerEntity player : world.getPlayers()) {
                if (player.isRemoved() || player.isSpectator()) continue;
                Box playerBox = player.getBoundingBox();
                int minX = (int) Math.floor(playerBox.minX);
                int maxX = (int) Math.floor(playerBox.maxX);
                int minY = (int) Math.floor(playerBox.minY);
                int maxY = (int) Math.floor(playerBox.maxY);
                int minZ = (int) Math.floor(playerBox.minZ);
                int maxZ = (int) Math.floor(playerBox.maxZ);

                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            BlockPos pos = new BlockPos(x, y, z);
                            BlockState state = world.getBlockState(pos);
                            if (isFinderBlock(state)) {
                                Box blockBox = new Box(pos);
                                if (playerBox.intersects(blockBox)) {
                                    occupiedBlocks.add(pos);
                                }
                            }
                        }
                    }
                }
            }

            Set<BlockPos> targetPowered = new HashSet<>();
            Set<BlockPos> visited = new HashSet<>();

            for (BlockPos pos : occupiedBlocks) {
                if (!visited.contains(pos)) {
                    BlockState state = world.getBlockState(pos);
                    net.minecraft.block.Block blockType = state.getBlock();
                    Set<BlockPos> net = buildNetwork(world, pos, blockType);
                    visited.addAll(net);
                    targetPowered.addAll(net);

                    if (blockType == ModBlocks.ONE_TIME_PLAYERFINDER) {
                        Map<BlockPos, Long> remMap = removalScheduled.computeIfAbsent(world, w -> new HashMap<>());
                        for (BlockPos netPos : net) {
                            remMap.putIfAbsent(netPos, now + ONE_TIME_PULSE_TICKS);
                        }
                    }
                }
            }

            if (worldRemoval != null) {
                for (Map.Entry<BlockPos, Long> entry : worldRemoval.entrySet()) {
                    if (now < entry.getValue() && world.getBlockState(entry.getKey()).isOf(ModBlocks.ONE_TIME_PLAYERFINDER)) {
                        targetPowered.add(entry.getKey());
                    }
                }
            }

            Set<BlockPos> currentlyPowered = poweredBlocks.get(world);
            if (currentlyPowered == null) {
                currentlyPowered = new HashSet<>();
                poweredBlocks.put(world, currentlyPowered);
            }

            Set<BlockPos> toDeactivate = new HashSet<>(currentlyPowered);
            toDeactivate.removeAll(targetPowered);

            Set<BlockPos> toActivate = new HashSet<>(targetPowered);
            toActivate.removeAll(currentlyPowered);

            for (BlockPos pos : toDeactivate) {
                currentlyPowered.remove(pos);
                updateAllNeighbors(world, pos);
            }

            for (BlockPos pos : toActivate) {
                currentlyPowered.add(pos);
                updateAllNeighbors(world, pos);
            }
        }
    }

    public void onCollide(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
    }

    public boolean isPowered(ServerWorld world, BlockPos pos) {
        Set<BlockPos> set = poweredBlocks.get(world);
        return set != null && set.contains(pos);
    }

    private static boolean isFinderBlock(BlockState state) {
        return state.isOf(ModBlocks.PLAYERFINDER) || state.isOf(ModBlocks.ONE_TIME_PLAYERFINDER);
    }

    private static void updateAllNeighbors(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        net.minecraft.block.Block block = isFinderBlock(state) ? state.getBlock() : ModBlocks.PLAYERFINDER;
        world.updateNeighborsAlways(pos, block);
        for (Direction direction : Direction.values()) {
            world.updateNeighborsAlways(pos.offset(direction), block);
        }
    }

    private static Set<BlockPos> buildNetwork(ServerWorld world, BlockPos origin, net.minecraft.block.Block type) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        visited.add(origin);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (Direction dir : AXES) {
                BlockPos neighbor = current.offset(dir);
                if (!visited.contains(neighbor) && world.getBlockState(neighbor).isOf(type)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        return visited;
    }
}
