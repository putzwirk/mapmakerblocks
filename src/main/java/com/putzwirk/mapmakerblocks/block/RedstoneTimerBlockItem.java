package com.putzwirk.mapmakerblocks.block;

import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RedstoneTimerBlockItem extends BlockItem {

    public RedstoneTimerBlockItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.mmblocks.redstone_timer.1"));
        tooltip.add(Text.translatable("tooltip.mmblocks.redstone_timer.2"));
        tooltip.add(Text.translatable("tooltip.mmblocks.redstone_timer.credit").formatted(Formatting.GRAY));

        super.appendTooltip(stack, world, tooltip, context);
    }
}
