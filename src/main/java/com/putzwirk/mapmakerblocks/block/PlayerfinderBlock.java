package com.putzwirk.mapmakerblocks.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import java.util.*;

public class PlayerfinderBlock extends Block {

    private static final Map<BlockPos, Set<BlockPos>> networkMap = new HashMap<>();
    private static final Map<UUID, Long> playerLastSignalTick = new HashMap<>();
    private static final Map<UUID, Boolean> playerInsideNetwork = new HashMap<>();

    public PlayerfinderBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    @Override
    public float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public boolean isTransparent(BlockState state, BlockView world, BlockPos pos) {
        return true;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, net.minecraft.block.ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, net.minecraft.block.ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!world.isClient) {
            rebuildNetwork(world, pos);
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!world.isClient && !newState.isOf(this)) {
            Set<BlockPos> oldNetwork = networkMap.remove(pos);
            if (oldNetwork != null) {
                oldNetwork.remove(pos);
                for (BlockPos neighbor : oldNetwork) {
                    rebuildNetwork(world, neighbor);
                }
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    private void rebuildNetwork(World world, BlockPos origin) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(origin);
        visited.add(origin);

        Direction[] axes = {Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST};

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (Direction dir : axes) {
                BlockPos neighbor = current.offset(dir);
                if (!visited.contains(neighbor) && world.getBlockState(neighbor).getBlock() instanceof PlayerfinderBlock) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        for (BlockPos member : visited) {
            networkMap.put(member, visited);
        }
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (world.isClient || !(entity instanceof ServerPlayerEntity player)) return;

        UUID uuid = player.getUuid();
        long currentTick = world.getTime();
        Set<BlockPos> network = networkMap.getOrDefault(pos, new HashSet<>(Set.of(pos)));

        boolean isInsideAny = isPlayerInsideNetwork(player, network, world);

        boolean wasInside = playerInsideNetwork.getOrDefault(uuid, false);

        if (isInsideAny && !wasInside) {
            playerInsideNetwork.put(uuid, true);
            if (playerLastSignalTick.getOrDefault(uuid, -100L) != currentTick) {
                playerLastSignalTick.put(uuid, currentTick);
                emitNetworkSignal(world, network, currentTick);
            }
        } else if (!isInsideAny) {
            playerInsideNetwork.put(uuid, false);
        }
    }

    private boolean isPlayerInsideNetwork(ServerPlayerEntity player, Set<BlockPos> network, World world) {
        Vec3d playerPos = player.getPos();
        for (BlockPos netPos : network) {
            double dx = playerPos.x - (netPos.getX() + 0.5);
            double dy = playerPos.y - (netPos.getY() + 0.5);
            double dz = playerPos.z - (netPos.getZ() + 0.5);
            if (Math.abs(dx) <= 0.5 && Math.abs(dy) <= 1.0 && Math.abs(dz) <= 0.5) {
                return true;
            }
        }
        return false;
    }

    private void emitNetworkSignal(World world, Set<BlockPos> network, long tick) {
        for (BlockPos netPos : network) {
            world.updateNeighborsAlways(netPos, this);
            world.scheduleBlockTick(netPos, this, 1);
        }
    }

    @Override
    public void scheduledTick(BlockState state, net.minecraft.server.world.ServerWorld world, BlockPos pos, net.minecraft.util.math.random.Random random) {
        world.updateNeighborsAlways(pos, this);
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return 15;
    }

    @Override
    public int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return 15;
    }

    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return true;
    }
}
