package com.putzwirk.mapmakerblocks.block;

import com.putzwirk.mapmakerblocks.block.entity.RedstoneTimerBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class RedstoneTimerBlock extends BlockWithEntity {

    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty OUTPUT_POWERED = BooleanProperty.of("output_powered");

    private static final int CLICK_INTERVAL_TICKS = 5;

    public RedstoneTimerBlock(Settings settings) {
        super(settings);

        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(OUTPUT_POWERED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, OUTPUT_POWERED);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new RedstoneTimerBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState()
                .with(FACING, ctx.getHorizontalPlayerFacing().getOpposite())
                .with(OUTPUT_POWERED, false);
    }

    private Direction getInputSide(BlockState state) {
        return state.get(FACING);
    }

    private Direction getOutputSide(BlockState state) {
        return state.get(FACING).getOpposite();
    }

    private BlockPos getInputPos(BlockState state, BlockPos pos) {
        return pos.offset(getInputSide(state));
    }

    private BlockPos getOutputPos(BlockState state, BlockPos pos) {
        return pos.offset(getOutputSide(state));
    }

    private boolean isReceivingFrontRedstone(World world, BlockPos pos, BlockState state) {
        BlockPos inputPos = getInputPos(state, pos);
        BlockState inputState = world.getBlockState(inputPos);

        if (inputState.getBlock() instanceof RedstoneTimerBlock) {
            return false;
        }

        if (inputState.contains(Properties.POWER)
                && inputState.get(Properties.POWER) > 0) {
            return true;
        }

        if (inputState.contains(Properties.POWERED)
                && inputState.get(Properties.POWERED)) {
            return true;
        }

        return world.getEmittedRedstonePower(inputPos, getInputSide(state)) > 0
                || world.getEmittedRedstonePower(inputPos, getInputSide(state).getOpposite()) > 0;
    }

    private void updateOutputNeighbors(World world, BlockPos pos, BlockState state) {
        world.updateNeighbors(pos, this);
        world.updateNeighbors(getOutputPos(state, pos), this);
    }

    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return true;
    }

    @Override
    public int getWeakRedstonePower(
            BlockState state,
            BlockView world,
            BlockPos pos,
            Direction direction
    ) {
        if (!state.get(OUTPUT_POWERED)) {
            return 0;
        }

        return direction == getOutputSide(state).getOpposite() ? 15 : 0;
    }

    @Override
    public int getStrongRedstonePower(
            BlockState state,
            BlockView world,
            BlockPos pos,
            Direction direction
    ) {
        return 0;
    }

    @Override
    public boolean isTransparent(BlockState state, BlockView world, BlockPos pos) {
        return true;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient && world.getBlockEntity(pos) instanceof RedstoneTimerBlockEntity blockEntity) {
            player.openHandledScreen(blockEntity);
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (world.isClient) {
            return;
        }

        if (!sourcePos.equals(getInputPos(state, pos))) {
            return;
        }

        if (isReceivingFrontRedstone(world, pos, state)) {
            startTimer(world, pos, state);
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            if (world.getBlockEntity(pos) instanceof RedstoneTimerBlockEntity blockEntity) {
                blockEntity.resetRuntime();
            }

            if (state.get(OUTPUT_POWERED)) {
                BlockState offState = state.with(OUTPUT_POWERED, false);
                updateOutputNeighbors(world, pos, offState);
            }
        }

        super.onStateReplaced(state, world, pos, newState, moved);
    }

    public void startTimer(World world, BlockPos pos, BlockState state) {
        if (!(world.getBlockEntity(pos) instanceof RedstoneTimerBlockEntity blockEntity)) {
            return;
        }

        if (!blockEntity.isEnabled()) {
            return;
        }

        if (blockEntity.isRunning()) {
            return;
        }

        if (state.get(OUTPUT_POWERED)) {
            return;
        }

        blockEntity.setRunning(true);

        float delaySec = blockEntity.getSeconds();
        float randSec = blockEntity.getRandomDelay();
        if (randSec > 0.0f) {
            delaySec += (float) (world.random.nextDouble() * randSec);
        }

        blockEntity.setDelayTicksRemaining(Math.round(delaySec * 20.0f));
        blockEntity.setOnTicksRemaining(0);

        world.scheduleBlockTick(pos, this, 1);
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (!(world.getBlockEntity(pos) instanceof RedstoneTimerBlockEntity blockEntity)) {
            return;
        }

        if (!blockEntity.isEnabled()) {
            blockEntity.resetRuntime();

            if (state.get(OUTPUT_POWERED)) {
                BlockState unpoweredState = state.with(OUTPUT_POWERED, false);
                world.setBlockState(pos, unpoweredState, Block.NOTIFY_ALL);
                updateOutputNeighbors(world, pos, unpoweredState);
            }

            return;
        }

        boolean autonomous = blockEntity.isAutonomousStart();

        if (!blockEntity.isRunning() && !state.get(OUTPUT_POWERED)) {
            return;
        }

        if (!state.get(OUTPUT_POWERED)) {
            if (blockEntity.getDelayTicksRemaining() > 0) {
                blockEntity.decreaseDelayTicksRemaining(CLICK_INTERVAL_TICKS);
                world.scheduleBlockTick(pos, this, CLICK_INTERVAL_TICKS);
                return;
            }

            BlockState poweredState = state.with(OUTPUT_POWERED, true);

            world.setBlockState(pos, poweredState, Block.NOTIFY_ALL);
            updateOutputNeighbors(world, pos, poweredState);

            float onSec = blockEntity.getOnSeconds();
            float randOnSec = blockEntity.getRandomOnSeconds();
            if (randOnSec > 0.0f) {
                onSec += (float) (world.random.nextDouble() * randOnSec);
            }

            blockEntity.setOnTicksRemaining(Math.max(1, Math.round(onSec * 20.0f)));

            world.scheduleBlockTick(pos, this, CLICK_INTERVAL_TICKS);
            return;
        }

        blockEntity.decreaseOnTicksRemaining(CLICK_INTERVAL_TICKS);

        if (blockEntity.getOnTicksRemaining() <= 0) {
            BlockState unpoweredState = state.with(OUTPUT_POWERED, false);

            world.setBlockState(pos, unpoweredState, Block.NOTIFY_ALL);
            updateOutputNeighbors(world, pos, unpoweredState);

            blockEntity.resetRuntime();

            if (blockEntity.isLoop() && blockEntity.isEnabled()) {
                if (autonomous || isReceivingFrontRedstone(world, pos, world.getBlockState(pos))) {
                    startTimer(world, pos, world.getBlockState(pos));
                }
            } else if (autonomous && blockEntity.isEnabled()) {
                blockEntity.setAutonomousStart(false);
            }

            return;
        }

        world.scheduleBlockTick(pos, this, CLICK_INTERVAL_TICKS);
    }
}
