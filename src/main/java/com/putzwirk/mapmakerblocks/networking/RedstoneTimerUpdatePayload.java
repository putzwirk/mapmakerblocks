package com.putzwirk.mapmakerblocks.networking;

import com.putzwirk.mapmakerblocks.Mapmakerblocks;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record RedstoneTimerUpdatePayload(
        BlockPos pos,
        float seconds,
        float randomDelay,
        float onSeconds,
        float randomOnSeconds,
        boolean loop,
        boolean enabled,
        boolean autonomousStart
) {

    public static final Identifier ID = new Identifier(Mapmakerblocks.MOD_ID, "update_timer");

    public void write(PacketByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeFloat(seconds);
        buf.writeFloat(randomDelay);
        buf.writeFloat(onSeconds);
        buf.writeFloat(randomOnSeconds);
        buf.writeBoolean(loop);
        buf.writeBoolean(enabled);
        buf.writeBoolean(autonomousStart);
    }

    public static RedstoneTimerUpdatePayload read(PacketByteBuf buf) {
        return new RedstoneTimerUpdatePayload(
                buf.readBlockPos(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean()
        );
    }
}
