package com.putzwirk.mapmakerblocks;

import com.putzwirk.mapmakerblocks.block.*;
import com.putzwirk.mapmakerblocks.item.BlockVisualizerItem;
import com.putzwirk.mapmakerblocks.mixin.BlockEntityTypeAccessor;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;

public class ModBlocks {

    public static final SilentSculkSensorBlock SILENT_INVISIBLE_SCULK_SENSOR = registerBlock("silent_invisible_sculk_sensor",
            new SilentSculkSensorBlock(FabricBlockSettings.copyOf(Blocks.SCULK_SENSOR).nonOpaque().luminance(state -> 0)));

    public static final PlayerfinderBlock PLAYERFINDER = registerBlock("playerfinder",
            new PlayerfinderBlock(FabricBlockSettings.copyOf(Blocks.SLIME_BLOCK).nonOpaque().noCollision()));

    public static final OneTimePlayerfinderBlock ONE_TIME_PLAYERFINDER = registerBlock("one_time_playerfinder",
            new OneTimePlayerfinderBlock(FabricBlockSettings.copyOf(Blocks.HONEY_BLOCK).nonOpaque().noCollision()));

    public static final BlockVisualizerItem BLOCK_VISUALIZER = registerItem("block_visualizer",
            new BlockVisualizerItem(new Item.Settings().maxCount(1)));

    public static final WirelessRedstoneSignBlock WIRELESS_REDSTONE_SIGN = registerBlock("wireless_redstone_sign",
            new WirelessRedstoneSignBlock(FabricBlockSettings.copyOf(Blocks.MANGROVE_SIGN)));

    public static final RedstoneTimerBlock REDSTONE_TIMER = registerBlockWithItem("redstone_timer",
            new RedstoneTimerBlock(FabricBlockSettings.create().strength(3f).sounds(BlockSoundGroup.STONE).nonOpaque()),
            RedstoneTimerBlockItem::new);

    private static <T extends Block> T registerBlock(String name, T block) {
        Registry.register(Registries.BLOCK, new Identifier(Mapmakerblocks.MOD_ID, name), block);
        Registry.register(Registries.ITEM, new Identifier(Mapmakerblocks.MOD_ID, name), new BlockItem(block, new Item.Settings()));
        return block;
    }

    private static <T extends Block> T registerBlockWithItem(String name, T block, BiFunction<T, Item.Settings, BlockItem> itemFactory) {
        Registry.register(Registries.BLOCK, new Identifier(Mapmakerblocks.MOD_ID, name), block);
        Registry.register(Registries.ITEM, new Identifier(Mapmakerblocks.MOD_ID, name), itemFactory.apply(block, new Item.Settings()));
        return block;
    }

    private static <T extends Item> T registerItem(String name, T item) {
        return Registry.register(Registries.ITEM, new Identifier(Mapmakerblocks.MOD_ID, name), item);
    }

    public static void registerModBlocks() {
        Set<Block> sculkBlocks = new HashSet<>(((BlockEntityTypeAccessor) BlockEntityType.SCULK_SENSOR).getBlocks());
        sculkBlocks.add(SILENT_INVISIBLE_SCULK_SENSOR);
        ((BlockEntityTypeAccessor) BlockEntityType.SCULK_SENSOR).setBlocks(sculkBlocks);

        Set<Block> signBlocks = new HashSet<>(((BlockEntityTypeAccessor) BlockEntityType.SIGN).getBlocks());
        signBlocks.add(WIRELESS_REDSTONE_SIGN);
        ((BlockEntityTypeAccessor) BlockEntityType.SIGN).setBlocks(signBlocks);
    }
}
