package com.putzwirk.mapmakerblocks;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import com.putzwirk.mapmakerblocks.block.entity.ModBlockEntities;
import com.putzwirk.mapmakerblocks.networking.ModMessages;
import com.putzwirk.mapmakerblocks.networking.WirelessSignPackets;
import com.putzwirk.mapmakerblocks.screen.ModScreenHandlers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mapmakerblocks implements ModInitializer {
    public static final String MOD_ID = "mmblocks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final ItemGroup MM_TAB = Registry.register(
            Registries.ITEM_GROUP,
            new Identifier(MOD_ID, "mapmaker_tab"),
            FabricItemGroup.builder()
                    .displayName(Text.literal("MapMaker Blocks"))
                    .icon(() -> new ItemStack(ModBlocks.PLAYERFINDER))
                    .entries((displayContext, entries) -> {
                        entries.add(ModBlocks.BLOCK_VISUALIZER);
                        entries.add(ModBlocks.PLAYERFINDER);
                        entries.add(ModBlocks.ONE_TIME_PLAYERFINDER);
                        entries.add(ModBlocks.SILENT_INVISIBLE_SCULK_SENSOR);
                        entries.add(ModBlocks.WIRELESS_REDSTONE_SIGN);
                        entries.add(ModBlocks.REDSTONE_TIMER);
                    })
                    .build()
    );

    @Override
    public void onInitialize() {
        ModBlocks.registerModBlocks();
        ModBlockEntities.registerBlockEntities();
        ModScreenHandlers.registerScreenHandlers();
        ModMessages.registerC2SPackets();
        PlayerfinderManager.register();
        WirelessSignManager.register();
        WirelessSignPackets.registerServer();
    }
}
