package com.putzwirk.mapmakerblocks;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class Mapmakerblocks implements ModInitializer {
    public static final String MOD_ID = "mapmakerblocks";

    public static final ItemGroup MM_TAB = Registry.register(
            Registries.ITEM_GROUP,
            new Identifier(MOD_ID, "mapmaker_tab"),
            FabricItemGroup.builder()
                    .displayName(Text.literal("MapMaker Blocks"))
                    .icon(() -> new ItemStack(ModBlocks.INVISIBLE_PLAYER_PRESSURE_PLATE))
                    .entries((displayContext, entries) -> {
                        entries.add(ModBlocks.INVISIBLE_PLAYER_PRESSURE_PLATE);
                        entries.add(ModBlocks.INVISIBLE_CHECKPOINT_PRESSURE_PLATE);
                        entries.add(ModBlocks.INVISIBLE_TRIPWIRE_HOOK);
                        entries.add(ModBlocks.INVISIBLE_TRIPWIRE);
                        entries.add(ModBlocks.SILENT_INVISIBLE_SCULK_SENSOR);
                        entries.add(ModBlocks.BLOCK_VISUALIZER);
                    })
                    .build()
    );

    @Override
    public void onInitialize() {
        ModBlocks.registerModBlocks();
    }
}
