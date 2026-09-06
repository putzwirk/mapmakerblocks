package com.putzwirk.mapmakerblocks.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.SculkSensorBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.SculkSensorPhase;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.event.Vibrations;
import net.minecraft.world.event.listener.Vibration;
import org.jetbrains.annotations.Nullable;

public class SilentSculkSensorBlock extends SculkSensorBlock {

    public SilentSculkSensorBlock(Settings settings) {
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
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
    }

    @Override
    public net.minecraft.util.shape.VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, net.minecraft.block.ShapeContext context) {
        if (context instanceof net.minecraft.block.EntityShapeContext entityContext && entityContext.getEntity() instanceof net.minecraft.entity.player.PlayerEntity player) {
            if (player.isCreative()) {
                return super.getOutlineShape(state, world, pos, context);
            }
        }
        return net.minecraft.util.shape.VoxelShapes.empty();
    }

    @Override
    public void setActive(@Nullable Entity entity, World world, BlockPos pos, BlockState state, int power, int frequency) {
        world.setBlockState(pos, state.with(SCULK_SENSOR_PHASE, SculkSensorPhase.ACTIVE).with(POWER, power), Block.NOTIFY_ALL);
        world.scheduleBlockTick(pos, state.getBlock(), this.getCooldownTime());
        updateNeighbors(world, pos, state);
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        SculkSensorPhase phase = getPhase(state);
        if (phase == SculkSensorPhase.ACTIVE) {
            setCooldown(world, pos, state);
        } else if (phase == SculkSensorPhase.COOLDOWN) {
            world.setBlockState(pos, state.with(SCULK_SENSOR_PHASE, SculkSensorPhase.INACTIVE).with(POWER, 0), Block.NOTIFY_ALL);
            updateNeighbors(world, pos, state);
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return !world.isClient ? checkType(type, BlockEntityType.SCULK_SENSOR, (worldx, pos, statex, blockEntity) -> {
            tickSilentVibrations(worldx, blockEntity.getVibrationListenerData(), blockEntity.getVibrationCallback());
        }) : null;
    }

    private static void tickSilentVibrations(World world, Vibrations.ListenerData listenerData, Vibrations.Callback callback) {
        if (world instanceof ServerWorld serverWorld) {
            if (listenerData.getVibration() == null) {
                listenerData.getSelector()
                        .getVibrationToTick(serverWorld.getTime())
                        .ifPresent(vibration -> {
                            listenerData.setVibration(vibration);
                            listenerData.setDelay(callback.getDelay(vibration.distance()));
                            callback.onListen();
                            listenerData.getSelector().clear();
                        });
            }

            if (listenerData.getVibration() != null) {
                boolean bl = listenerData.getDelay() > 0;
                listenerData.setSpawnParticle(false);
                listenerData.tickDelay();
                if (listenerData.getDelay() <= 0) {
                    bl = acceptSilentVibration(serverWorld, listenerData, callback, listenerData.getVibration());
                }

                if (bl) {
                    callback.onListen();
                }
            }
        }
    }

    private static boolean acceptSilentVibration(
            ServerWorld world,
            Vibrations.ListenerData listenerData,
            Vibrations.Callback callback,
            Vibration vibration
    ) {
        BlockPos blockPos = BlockPos.ofFloored(vibration.pos());
        BlockPos blockPos2 = callback.getPositionSource().getPos(world).map(BlockPos::ofFloored).orElse(blockPos);
        if (callback.requiresTickingChunksAround() && !areChunksTickingAround(world, blockPos2)) {
            return false;
        }

        callback.accept(
                world,
                blockPos,
                vibration.gameEvent(),
                vibration.getEntity(world).orElse(null),
                vibration.getOwner(world).orElse(null),
                Vibrations.VibrationListener.getTravelDelay(blockPos, blockPos2)
        );
        listenerData.setVibration(null);
        return true;
    }

    private static boolean areChunksTickingAround(World world, BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        for (int i = chunkPos.x - 1; i <= chunkPos.x + 1; ++i) {
            for (int j = chunkPos.z - 1; j <= chunkPos.z + 1; ++j) {
                Chunk chunk = world.getChunkManager().getWorldChunk(i, j);
                if (chunk == null || !world.shouldTickBlocksInChunk(chunk.getPos().toLong())) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void updateNeighbors(World world, BlockPos pos, BlockState state) {
        for (Direction direction : Direction.values()) {
            world.updateNeighborsAlways(pos.offset(direction), state.getBlock());
        }
        world.updateNeighborsAlways(pos, state.getBlock());
    }
}
