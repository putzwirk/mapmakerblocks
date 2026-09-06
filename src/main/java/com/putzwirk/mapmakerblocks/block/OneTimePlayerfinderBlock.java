package com.putzwirk.mapmakerblocks.block;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OneTimePlayerfinderBlock extends PlayerfinderBlock {

    public OneTimePlayerfinderBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable BlockView world, List<Text> tooltip, TooltipContext options) {
        tooltip.add(Text.translatable("item.mmblocks.one_time_playerfinder.desc").formatted(Formatting.GRAY));
    }
}
