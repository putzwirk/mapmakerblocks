package com.putzwirk.mapmakerblocks.screen;

import com.putzwirk.mapmakerblocks.Mapmakerblocks;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class ModScreenHandlers {

    public static final ScreenHandlerType<RedstoneTimerScreenHandler> REDSTONE_TIMER_SCREEN_HANDLER =
            Registry.register(
                    Registries.SCREEN_HANDLER,
                    new Identifier(Mapmakerblocks.MOD_ID, "redstone_timer"),
                    new ExtendedScreenHandlerType<>(RedstoneTimerScreenHandler::new)
            );

    public static void registerScreenHandlers() {
        Mapmakerblocks.LOGGER.info("Registering Screen Handlers for " + Mapmakerblocks.MOD_ID);
    }
}
