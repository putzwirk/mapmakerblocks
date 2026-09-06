package com.putzwirk.mapmakerblocks.block;

import com.putzwirk.mapmakerblocks.Mapmakerblocks;
import net.fabricmc.fabric.api.object.builder.v1.block.type.BlockSetTypeBuilder;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockSetType;
import net.minecraft.block.BlockState;
import net.minecraft.block.PressurePlateBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class InvisiblePlayerPressurePlateBlock extends PressurePlateBlock {

    public static final BlockSetType SILENT_BLOCK_SET_TYPE = BlockSetTypeBuilder.copyOf(BlockSetType.OAK)
            .pressurePlateClickOnSound(SoundEvents.INTENTIONALLY_EMPTY)
            .pressurePlateClickOffSound(SoundEvents.INTENTIONALLY_EMPTY)
            .register(new Identifier(Mapmakerblocks.MOD_ID, "silent_pressure_plate"));

    public InvisiblePlayerPressurePlateBlock(Settings settings) {
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
    protected int getRedstoneOutput(World world, BlockPos pos) {
        return getEntityCount(world, BOX.offset(pos), PlayerEntity.class) > 0 ? 15 : 0;
    }
}
