package com.putzwirk.mapmakerblocks.block;

import com.putzwirk.mapmakerblocks.Mapmakerblocks;
import net.fabricmc.fabric.api.object.builder.v1.block.type.BlockSetTypeBuilder;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockSetType;
import net.minecraft.block.BlockState;
import net.minecraft.block.PressurePlateBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class InvisibleCheckpointPressurePlateBlock extends PressurePlateBlock {

    public static final BlockSetType SILENT_BLOCK_SET_TYPE = BlockSetTypeBuilder.copyOf(BlockSetType.OAK)
            .pressurePlateClickOnSound(SoundEvents.INTENTIONALLY_EMPTY)
            .pressurePlateClickOffSound(SoundEvents.INTENTIONALLY_EMPTY)
            .register(new Identifier(Mapmakerblocks.MOD_ID, "silent_checkpoint_pressure_plate"));

    public InvisibleCheckpointPressurePlateBlock(Settings settings) {
        super(ActivationRule.EVERYTHING, settings, SILENT_BLOCK_SET_TYPE);
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
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        super.onEntityCollision(state, world, pos, entity);
        if (!world.isClient && entity instanceof ServerPlayerEntity player) {
            BlockPos spawnPos = pos.up();
            BlockPos currentSpawn = player.getSpawnPointPosition();
            RegistryKey<World> currentDimension = player.getSpawnPointDimension();
            if (spawnPos.equals(currentSpawn) && world.getRegistryKey().equals(currentDimension)) {
                return;
            }
            player.setSpawnPoint(world.getRegistryKey(), spawnPos, player.getYaw(), true, false);
            world.playSound(null, pos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.BLOCKS, 1.0F, 1.0F);
            player.sendMessage(Text.translatable("block.mapmakerblocks.invisible_checkpoint_pressure_plate.set").formatted(Formatting.AQUA), true);
        }
    }

    @Override
    protected int getRedstoneOutput(World world, BlockPos pos) {
        return getEntityCount(world, BOX.offset(pos), PlayerEntity.class) > 0 ? 15 : 0;
    }
}
