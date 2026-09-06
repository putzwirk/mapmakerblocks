package com.putzwirk.mapmakerblocks.client;

import com.putzwirk.mapmakerblocks.ModBlocks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PressurePlateBlock;
import net.minecraft.block.SculkSensorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class MapmakerblocksClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(),
                ModBlocks.INVISIBLE_PLAYER_PRESSURE_PLATE,
                ModBlocks.INVISIBLE_CHECKPOINT_PRESSURE_PLATE,
                ModBlocks.SILENT_INVISIBLE_SCULK_SENSOR,
                ModBlocks.PLAYERFINDER
        );

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientWorld world = client.world;
            ClientPlayerEntity player = client.player;
            if (world == null || player == null) {
                return;
            }

            if (!player.getMainHandStack().isOf(ModBlocks.BLOCK_VISUALIZER) && !player.getOffHandStack().isOf(ModBlocks.BLOCK_VISUALIZER)) {
                return;
            }

            Camera camera = context.camera();
            Vec3d cameraPos = camera.getPos();
            BlockPos center = BlockPos.ofFloored(cameraPos);
            int radius = 24;

            MatrixStack matrices = context.matrixStack();
            VertexConsumerProvider consumers = context.consumers();
            if (consumers == null) {
                return;
            }

            BlockRenderManager renderManager = client.getBlockRenderManager();
            BlockPos.Mutable mutable = new BlockPos.Mutable();

            int minY = Math.max(world.getBottomY(), center.getY() - radius);
            int maxY = Math.min(world.getTopY() - 1, center.getY() + radius);

            for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                        mutable.set(x, y, z);
                        BlockState state = world.getBlockState(mutable);
                        BlockState visualState = getVisualState(state);
                        if (visualState != null) {
                            matrices.push();
                            matrices.translate(x - cameraPos.x, y - cameraPos.y, z - cameraPos.z);
                            BakedModel model = renderManager.getModel(visualState);
                            int light = WorldRenderer.getLightmapCoordinates(world, state, mutable);
                            renderManager.getModelRenderer().render(
                                    matrices.peek(),
                                    consumers.getBuffer(RenderLayers.getEntityBlockLayer(visualState, false)),
                                    visualState,
                                    model,
                                    1.0F, 1.0F, 1.0F,
                                    light,
                                    OverlayTexture.DEFAULT_UV
                            );
                            matrices.pop();
                        }
                    }
                }
            }
        });
    }

    private static BlockState getVisualState(BlockState state) {
        Block block = state.getBlock();
        if (block == ModBlocks.INVISIBLE_PLAYER_PRESSURE_PLATE) {
            return Blocks.OAK_PRESSURE_PLATE.getDefaultState().with(PressurePlateBlock.POWERED, state.get(PressurePlateBlock.POWERED));
        } else if (block == ModBlocks.INVISIBLE_CHECKPOINT_PRESSURE_PLATE) {
            return Blocks.WARPED_PRESSURE_PLATE.getDefaultState().with(PressurePlateBlock.POWERED, state.get(PressurePlateBlock.POWERED));
        } else if (block == ModBlocks.SILENT_INVISIBLE_SCULK_SENSOR) {
            return Blocks.SCULK_SENSOR.getDefaultState()
                    .with(SculkSensorBlock.SCULK_SENSOR_PHASE, state.get(SculkSensorBlock.SCULK_SENSOR_PHASE))
                    .with(SculkSensorBlock.POWER, state.get(SculkSensorBlock.POWER))
                    .with(SculkSensorBlock.WATERLOGGED, state.get(SculkSensorBlock.WATERLOGGED));
        } else if (block == ModBlocks.PLAYERFINDER) {
            return Blocks.SLIME_BLOCK.getDefaultState();
        }
        return null;
    }
}
