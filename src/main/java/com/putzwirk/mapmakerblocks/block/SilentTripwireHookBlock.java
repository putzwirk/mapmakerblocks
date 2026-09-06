package com.putzwirk.mapmakerblocks.block;

import com.google.common.base.MoreObjects;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.TripwireBlock;
import net.minecraft.block.TripwireHookBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class SilentTripwireHookBlock extends TripwireHookBlock {

    public SilentTripwireHookBlock(Settings settings) {
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
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        this.update(world, pos, state, false, false, -1, null);
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        this.update(world, pos, state, false, true, -1, null);
    }

    @Override
    public void update(World world, BlockPos pos, BlockState state, boolean beingRemoved, boolean bl, int i, @Nullable BlockState blockState) {
        Direction direction = state.get(FACING);
        boolean attached = state.get(ATTACHED);
        boolean powered = state.get(POWERED);
        boolean willBeAttached = !beingRemoved;
        boolean willBePowered = false;
        int otherHookDistance = 0;
        BlockState[] lineStates = new BlockState[42];

        for (int k = 1; k < 42; ++k) {
            BlockPos currentPos = pos.offset(direction, k);
            BlockState currentState = world.getBlockState(currentPos);
            if (currentState.getBlock() instanceof TripwireHookBlock) {
                if (currentState.get(FACING) == direction.getOpposite()) {
                    otherHookDistance = k;
                }
                break;
            }

            if (!(currentState.getBlock() instanceof TripwireBlock) && k != i) {
                lineStates[k] = null;
                willBeAttached = false;
            } else {
                if (k == i) {
                    currentState = MoreObjects.firstNonNull(blockState, currentState);
                }

                boolean armed = !currentState.get(TripwireBlock.DISARMED);
                boolean wirePowered = currentState.get(TripwireBlock.POWERED);
                willBePowered |= armed && wirePowered;
                lineStates[k] = currentState;
                if (k == i) {
                    world.scheduleBlockTick(pos, this, 10);
                    willBeAttached &= armed;
                }
            }
        }

        willBeAttached &= otherHookDistance > 1;
        willBePowered &= willBeAttached;
        BlockState newHookState = this.getDefaultState().with(ATTACHED, willBeAttached).with(POWERED, willBePowered);
        if (otherHookDistance > 0) {
            BlockPos otherHookPos = pos.offset(direction, otherHookDistance);
            Direction otherDirection = direction.getOpposite();
            BlockState otherCurrentState = world.getBlockState(otherHookPos);
            BlockState otherNewState = (otherCurrentState.getBlock() instanceof TripwireHookBlock ? otherCurrentState.getBlock().getDefaultState() : newHookState)
                    .with(ATTACHED, willBeAttached).with(POWERED, willBePowered).with(FACING, otherDirection);
            world.setBlockState(otherHookPos, otherNewState, Block.NOTIFY_ALL);
            this.updateNeighborsOnAxis(world, otherHookPos, otherDirection);
            this.playSound(world, otherHookPos, willBeAttached, willBePowered, attached, powered);
        }

        this.playSound(world, pos, willBeAttached, willBePowered, attached, powered);
        if (!beingRemoved) {
            world.setBlockState(pos, newHookState.with(FACING, direction), Block.NOTIFY_ALL);
            if (bl) {
                this.updateNeighborsOnAxis(world, pos, direction);
            }
        }

        if (attached != willBeAttached) {
            for (int l = 1; l < otherHookDistance; ++l) {
                BlockPos wirePos = pos.offset(direction, l);
                BlockState wireState = lineStates[l];
                if (wireState != null) {
                    world.setBlockState(wirePos, wireState.with(ATTACHED, willBeAttached), Block.NOTIFY_ALL);
                    if (!world.getBlockState(wirePos).isAir()) {
                    }
                }
            }
        }
    }

    private void updateNeighborsOnAxis(World world, BlockPos pos, Direction direction) {
        world.updateNeighborsAlways(pos, this);
        world.updateNeighborsAlways(pos.offset(direction.getOpposite()), this);
    }

    private void playSound(World world, BlockPos pos, boolean attached, boolean on, boolean detached, boolean off) {
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!moved && !state.isOf(newState.getBlock())) {
            boolean attached = state.get(ATTACHED);
            boolean powered = state.get(POWERED);
            if (attached || powered) {
                this.update(world, pos, state, true, false, -1, null);
            }

            if (powered) {
                world.updateNeighborsAlways(pos, this);
                world.updateNeighborsAlways(pos.offset(state.get(FACING).getOpposite()), this);
            }

            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }
}
