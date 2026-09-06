package com.putzwirk.mapmakerblocks;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mapmakerblocks implements ModInitializer {
    public static final String MOD_ID = "mapmakerblocks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("{} loaded", MOD_ID);
    }

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }
}