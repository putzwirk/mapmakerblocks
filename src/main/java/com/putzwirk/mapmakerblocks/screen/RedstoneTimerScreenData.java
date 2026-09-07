package com.putzwirk.mapmakerblocks.screen;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

public record RedstoneTimerScreenData(
        BlockPos pos,
        float seconds,
        float randomDelay,
        float onSeconds,
        float randomOnSeconds,
        boolean loop,
        boolean enabled,
        boolean autonomousStart
) {

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

    public static RedstoneTimerScreenData read(PacketByteBuf buf) {
        return new RedstoneTimerScreenData(
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
