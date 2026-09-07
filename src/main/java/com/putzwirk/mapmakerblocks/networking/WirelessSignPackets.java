package com.putzwirk.mapmakerblocks.networking;

import com.putzwirk.mapmakerblocks.Mapmakerblocks;
import com.putzwirk.mapmakerblocks.WirelessSignManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class WirelessSignPackets {

    public static final Identifier SET_MODE = new Identifier(Mapmakerblocks.MOD_ID, "wireless_sign_set_mode");

    private WirelessSignPackets() {}

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(SET_MODE, (server, player, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            boolean input = buf.readBoolean();
            server.execute(() -> WirelessSignManager.get().setMode(player.getServerWorld(), pos, input));
        });
    }
}