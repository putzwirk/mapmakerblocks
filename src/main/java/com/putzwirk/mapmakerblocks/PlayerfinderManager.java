package com.putzwirk.mapmakerblocks;

import com.putzwirk.mapmakerblocks.block.PlayerfinderBlock;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

public class PlayerfinderManager {

    private static PlayerfinderManager INSTANCE = new PlayerfinderManager();

    private final WeakHashMap<ServerPlayerEntity, NetworkEntry> playerState = new WeakHashMap<>();

    private static final Direction[] AXES = {Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST};

    private record NetworkEntry(BlockPos canonical, Set<BlockPos> members) {}

    private PlayerfinderManager() {}

    public static PlayerfinderManager get() {
        return INSTANCE;
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> INSTANCE = new PlayerfinderManager());

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 5 != 0) return;
            PlayerfinderManager mgr = INSTANCE;
            mgr.playerState.entrySet().removeIf(entry -> {
                ServerPlayerEntity player = entry.getKey();
                NetworkEntry ne = entry.getValue();
                World world = player.getServerWorld();
                return !isPlayerInNetwork(player.getPos(), ne.members(), world);
            });
        });
    }

    public void onCollide(ServerPlayerEntity player, World world, BlockPos pos) {
        NetworkEntry current = playerState.get(player);

        if (current != null && current.members().contains(pos)) {
            return;
        }

        Set<BlockPos> network = buildNetwork(world, pos);
        BlockPos canonical = network.stream()
                .min(Comparator.comparingLong(BlockPos::asLong))
                .orElse(pos);

        if (current != null && current.canonical().equals(canonical)) {
            return;
        }

        playerState.put(player, new NetworkEntry(canonical, network));
        activateNetwork(world, network);
    }

    private static Set<BlockPos> buildNetwork(World world, BlockPos origin) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        visited.add(origin);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (Direction dir : AXES) {
                BlockPos neighbor = current.offset(dir);
                if (!visited.contains(neighbor) && world.getBlockState(neighbor).getBlock() instanceof PlayerfinderBlock) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        return visited;
    }

    private static boolean isPlayerInNetwork(Vec3d playerPos, Set<BlockPos> network, World world) {
        for (BlockPos netPos : network) {
            if (world.getBlockState(netPos).getBlock() instanceof PlayerfinderBlock) {
                double dx = playerPos.x - (netPos.getX() + 0.5);
                double dy = playerPos.y - (netPos.getY() + 0.5);
                double dz = playerPos.z - (netPos.getZ() + 0.5);
                if (Math.abs(dx) <= 0.5 && Math.abs(dy) <= 1.0 && Math.abs(dz) <= 0.5) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void activateNetwork(World world, Set<BlockPos> network) {
        for (BlockPos netPos : network) {
            BlockState state = world.getBlockState(netPos);
            if (state.isOf(ModBlocks.PLAYERFINDER) && !state.get(PlayerfinderBlock.POWERED)) {
                world.setBlockState(netPos, state.with(PlayerfinderBlock.POWERED, true), Block.NOTIFY_ALL);
                world.updateNeighborsAlways(netPos, ModBlocks.PLAYERFINDER);
                world.scheduleBlockTick(netPos, ModBlocks.PLAYERFINDER, 2);
            }
        }
    }
}
