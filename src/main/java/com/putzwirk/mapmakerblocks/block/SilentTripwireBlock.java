package com.putzwirk.mapmakerblocks.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.TripwireBlock;
import net.minecraft.block.TripwireHookBlock;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.List;

public class SilentTripwireBlock extends TripwireBlock {

    public SilentTripwireBlock(SilentTripwireHookBlock hook, Settings settings) {
        super(hook, settings);
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
    public boolean shouldConnectTo(BlockState state, Direction facing) {
        if (state.getBlock() instanceof TripwireHookBlock) {
            return state.get(TripwireHookBlock.FACING) == facing.getOpposite();
        }
        return state.getBlock() instanceof TripwireBlock;
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.isOf(state.getBlock())) {
            this.update(world, pos, state);
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!moved && !state.isOf(newState.getBlock())) {
            this.update(world, pos, state.with(POWERED, true));
        }
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient) {
            if (!state.get(POWERED)) {
                this.updatePowered(world, pos);
            }
        }
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (world.getBlockState(pos).get(POWERED)) {
            this.updatePowered(world, pos);
        }
    }

    private void update(World world, BlockPos pos, BlockState state) {
        for (Direction direction : new Direction[]{Direction.SOUTH, Direction.WEST}) {
            for (int i = 1; i < 42; ++i) {
                BlockPos blockPos = pos.offset(direction, i);
                BlockState blockState = world.getBlockState(blockPos);
                if (blockState.getBlock() instanceof TripwireHookBlock hook) {
                    if (blockState.get(TripwireHookBlock.FACING) == direction.getOpposite()) {
                        hook.update(world, blockPos, blockState, false, true, i, state);
                    }
                    break;
                }

                if (!(blockState.getBlock() instanceof TripwireBlock)) {
                    break;
                }
            }
        }
    }

    private void updatePowered(World world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        boolean wasPowered = blockState.get(POWERED);
        boolean isPowered = false;
        List<? extends Entity> list = world.getOtherEntities(null, blockState.getOutlineShape(world, pos).getBoundingBox().offset(pos));
        if (!list.isEmpty()) {
            for (Entity entity : list) {
                if (!entity.canAvoidTraps()) {
                    isPowered = true;
                    break;
                }
            }
        }

        if (isPowered != wasPowered) {
            blockState = blockState.with(POWERED, isPowered);
            world.setBlockState(pos, blockState, Block.NOTIFY_ALL);
            this.update(world, pos, blockState);
        }

        if (isPowered) {
            world.scheduleBlockTick(new BlockPos(pos), this, 10);
        }
    }
}
