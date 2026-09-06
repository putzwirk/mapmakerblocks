package com.putzwirk.mapmakerblocks.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.*;

public class PlayerfinderBlock extends Block {

    public static final BooleanProperty POWERED = BooleanProperty.of("powered");

    private static final VoxelShape OUTLINE_SHAPE = Block.createCuboidShape(0, 0, 0, 16, 16, 16);

    private static final Map<BlockPos, Set<BlockPos>> networkMap = new HashMap<>();
    private static final Map<UUID, Boolean> playerInsideNetwork = new HashMap<>();

    public PlayerfinderBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(POWERED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
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
        return OUTLINE_SHAPE;
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
                for (BlockPos neighbor : new HashSet<>(oldNetwork)) {
                    rebuildNetwork(world, neighbor);
                }
            }
            world.updateNeighborsAlways(pos, this);
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
        Set<BlockPos> network = networkMap.getOrDefault(pos, new HashSet<>(Set.of(pos)));

        boolean isInsideAny = isPlayerInsideNetwork(player, network);
        boolean wasInside = playerInsideNetwork.getOrDefault(uuid, false);

        if (isInsideAny && !wasInside) {
            playerInsideNetwork.put(uuid, true);
            activateNetwork(world, network);
        } else if (!isInsideAny) {
            playerInsideNetwork.put(uuid, false);
        }
    }

    private boolean isPlayerInsideNetwork(ServerPlayerEntity player, Set<BlockPos> network) {
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

    private void activateNetwork(World world, Set<BlockPos> network) {
        for (BlockPos netPos : network) {
            BlockState current = world.getBlockState(netPos);
            if (current.isOf(this) && !current.get(POWERED)) {
                world.setBlockState(netPos, current.with(POWERED, true), Block.NOTIFY_ALL);
                world.updateNeighborsAlways(netPos, this);
                world.scheduleBlockTick(netPos, this, 2);
            }
        }
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (state.get(POWERED)) {
            world.setBlockState(pos, state.with(POWERED, false), Block.NOTIFY_ALL);
            world.updateNeighborsAlways(pos, this);
        }
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return state.get(POWERED) ? 15 : 0;
    }

    @Override
    public int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return state.get(POWERED) ? 15 : 0;
    }

    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return true;
    }
}
