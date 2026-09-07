package com.putzwirk.mapmakerblocks.block.entity;

import com.putzwirk.mapmakerblocks.Mapmakerblocks;
import com.putzwirk.mapmakerblocks.ModBlocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {

    public static final BlockEntityType<RedstoneTimerBlockEntity> REDSTONE_TIMER_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    new Identifier(Mapmakerblocks.MOD_ID, "redstone_timer"),
                    BlockEntityType.Builder.create(
                            RedstoneTimerBlockEntity::new,
                            ModBlocks.REDSTONE_TIMER
                    ).build(null)
            );

    public static void registerBlockEntities() {
        Mapmakerblocks.LOGGER.info("Registering Block Entities for " + Mapmakerblocks.MOD_ID);
    }
}
