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
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;

public class ModBlocks {

    public static final SilentTripwireHookBlock INVISIBLE_TRIPWIRE_HOOK = registerBlock("invisible_tripwire_hook",
            new SilentTripwireHookBlock(FabricBlockSettings.copyOf(Blocks.TRIPWIRE_HOOK).nonOpaque()));

    public static final SilentTripwireBlock INVISIBLE_TRIPWIRE = registerBlock("invisible_tripwire",
            new SilentTripwireBlock(INVISIBLE_TRIPWIRE_HOOK, FabricBlockSettings.copyOf(Blocks.TRIPWIRE).nonOpaque()));

    public static final InvisiblePlayerPressurePlateBlock INVISIBLE_PLAYER_PRESSURE_PLATE = registerBlock("invisible_player_pressure_plate",
            new InvisiblePlayerPressurePlateBlock(FabricBlockSettings.copyOf(Blocks.OAK_PRESSURE_PLATE).nonOpaque()));

    public static final InvisibleCheckpointPressurePlateBlock INVISIBLE_CHECKPOINT_PRESSURE_PLATE = registerBlock("invisible_checkpoint_pressure_plate",
            new InvisibleCheckpointPressurePlateBlock(FabricBlockSettings.copyOf(Blocks.OAK_PRESSURE_PLATE).nonOpaque()));

    public static final SilentSculkSensorBlock SILENT_INVISIBLE_SCULK_SENSOR = registerBlock("silent_invisible_sculk_sensor",
            new SilentSculkSensorBlock(FabricBlockSettings.copyOf(Blocks.SCULK_SENSOR).nonOpaque()));

    public static final BlockVisualizerItem BLOCK_VISUALIZER = registerItem("block_visualizer",
            new BlockVisualizerItem(new Item.Settings().maxCount(1)));

    private static <T extends Block> T registerBlock(String name, T block) {
        Registry.register(Registries.BLOCK, new Identifier(Mapmakerblocks.MOD_ID, name), block);
        Registry.register(Registries.ITEM, new Identifier(Mapmakerblocks.MOD_ID, name), new BlockItem(block, new Item.Settings()));
        return block;
    }

    private static <T extends Item> T registerItem(String name, T item) {
        return Registry.register(Registries.ITEM, new Identifier(Mapmakerblocks.MOD_ID, name), item);
    }

    public static void registerModBlocks() {
        Set<Block> blocks = new HashSet<>(((BlockEntityTypeAccessor) BlockEntityType.SCULK_SENSOR).getBlocks());
        blocks.add(SILENT_INVISIBLE_SCULK_SENSOR);
        ((BlockEntityTypeAccessor) BlockEntityType.SCULK_SENSOR).setBlocks(blocks);
    }
}
