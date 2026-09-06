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

    private static final Direction[] AXES = {Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST};
    private static final int POWER_DURATION_TICKS = 2;

    private final WeakHashMap<ServerPlayerEntity, NetworkEntry> playerState = new WeakHashMap<>();
    private final Map<ServerWorld, Map<BlockPos, Long>> poweredUntil = new HashMap<>();

    private record NetworkEntry(BlockPos canonical, Set<BlockPos> members, ServerWorld world) {}

    private PlayerfinderManager() {}

    public static PlayerfinderManager get() {
        return INSTANCE;
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> INSTANCE = new PlayerfinderManager());

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            PlayerfinderManager mgr = INSTANCE;
            mgr.tickPowerOff(server);
            mgr.tickPlayerDeparture(server);
        });
    }

    private void tickPowerOff(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            Map<BlockPos, Long> worldMap = poweredUntil.get(world);
            if (worldMap == null || worldMap.isEmpty()) continue;
            long now = world.getTime();
            worldMap.entrySet().removeIf(entry -> {
                if (now >= entry.getValue()) {
                    BlockPos pos = entry.getKey();
                    updateAllNeighbors(world, pos);
                    return true;
                }
                return false;
            });
        }
    }

    private void tickPlayerDeparture(MinecraftServer server) {
        if (server.getTicks() % 2 != 0) return;
        playerState.entrySet().removeIf(entry -> {
            ServerPlayerEntity player = entry.getKey();
            NetworkEntry ne = entry.getValue();
            return player.isRemoved() || !isPlayerInNetwork(player, ne.members(), ne.world());
        });
    }

    public void onCollide(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        NetworkEntry current = playerState.get(player);

        if (current != null && current.world() == world && current.members().contains(pos)) {
            return;
        }

        Set<BlockPos> network = buildNetwork(world, pos);
        BlockPos canonical = network.stream()
                .min(Comparator.comparingLong(BlockPos::asLong))
                .orElse(pos);

        if (current != null && current.world() == world && current.canonical().equals(canonical)) {
            return;
        }

        playerState.put(player, new NetworkEntry(canonical, network, world));
        activateNetwork(world, network);
    }

    public boolean isPowered(ServerWorld world, BlockPos pos) {
        Map<BlockPos, Long> worldMap = poweredUntil.get(world);
        if (worldMap == null) return false;
        Long until = worldMap.get(pos);
        return until != null && world.getTime() < until;
    }

    private void activateNetwork(ServerWorld world, Set<BlockPos> network) {
        long deadline = world.getTime() + POWER_DURATION_TICKS;
        Map<BlockPos, Long> worldMap = poweredUntil.computeIfAbsent(world, w -> new HashMap<>());
        for (BlockPos pos : network) {
            worldMap.put(pos, deadline);
            updateAllNeighbors(world, pos);
        }
    }

    private static void updateAllNeighbors(ServerWorld world, BlockPos pos) {
        world.updateNeighborsAlways(pos, ModBlocks.PLAYERFINDER);
        for (Direction direction : Direction.values()) {
            world.updateNeighborsAlways(pos.offset(direction), ModBlocks.PLAYERFINDER);
        }
    }

    private static Set<BlockPos> buildNetwork(ServerWorld world, BlockPos origin) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        visited.add(origin);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (Direction dir : AXES) {
                BlockPos neighbor = current.offset(dir);
                if (!visited.contains(neighbor) && world.getBlockState(neighbor).isOf(ModBlocks.PLAYERFINDER)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        return visited;
    }

    private static boolean isPlayerInNetwork(ServerPlayerEntity player, Set<BlockPos> network, ServerWorld world) {
        if (player.getWorld() != world) return false;
        Box playerBox = player.getBoundingBox();
        for (BlockPos netPos : network) {
            if (!world.getBlockState(netPos).isOf(ModBlocks.PLAYERFINDER)) continue;
            Box blockBox = new Box(netPos);
            if (playerBox.intersects(blockBox)) {
                return true;
            }
        }
        return false;
    }
}
