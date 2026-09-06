package com.putzwirk.mapmakerblocks.block;

import com.putzwirk.mapmakerblocks.PlayerfinderManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class PlayerfinderBlock extends Block {

    private static final VoxelShape OUTLINE_SHAPE = Block.createCuboidShape(0, 0, 0, 16, 16, 16);

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
        if (context instanceof net.minecraft.block.EntityShapeContext entityContext && entityContext.getEntity() instanceof net.minecraft.entity.player.PlayerEntity player) {
            if (player.isCreative()) {
                return OUTLINE_SHAPE;
            }
        }
        return VoxelShapes.empty();
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (world.isClient || !(entity instanceof ServerPlayerEntity player)) return;
        PlayerfinderManager.get().onCollide(player, (ServerWorld) world, pos);
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        if (world instanceof ServerWorld sw) {
            return PlayerfinderManager.get().isPowered(sw, pos) ? 15 : 0;
        }
        return 0;
    }

    @Override
    public int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return getWeakRedstonePower(state, world, pos, direction);
    }

    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return true;
    }

    @Override
    public void appendTooltip(net.minecraft.item.ItemStack stack, @org.jetbrains.annotations.Nullable BlockView world, java.util.List<net.minecraft.text.Text> tooltip, net.minecraft.client.item.TooltipContext options) {
        tooltip.add(net.minecraft.text.Text.translatable("item.mmblocks.playerfinder.desc").formatted(net.minecraft.util.Formatting.GRAY));
    }
}
