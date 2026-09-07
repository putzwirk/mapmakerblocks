package com.putzwirk.mapmakerblocks.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;

public class RedstoneTimerScreenHandler extends ScreenHandler {

    private final BlockPos pos;
    private final float seconds;
    private final float randomDelay;
    private final float onSeconds;
    private final float randomOnSeconds;
    private final boolean loop;
    private final boolean enabled;
    private final boolean autonomousStart;

    public RedstoneTimerScreenHandler(int syncId, PlayerInventory playerInventory, RedstoneTimerScreenData data) {
        super(ModScreenHandlers.REDSTONE_TIMER_SCREEN_HANDLER, syncId);

        this.pos = data.pos();
        this.seconds = data.seconds();
        this.randomDelay = data.randomDelay();
        this.onSeconds = data.onSeconds();
        this.randomOnSeconds = data.randomOnSeconds();
        this.loop = data.loop();
        this.enabled = data.enabled();
        this.autonomousStart = data.autonomousStart();
    }

    public RedstoneTimerScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        this(syncId, playerInventory, RedstoneTimerScreenData.read(buf));
    }

    public BlockPos getPos() {
        return pos;
    }

    public float getSeconds() {
        return seconds;
    }

    public float getRandomDelay() {
        return randomDelay;
    }

    public float getOnSeconds() {
        return onSeconds;
    }

    public float getRandomOnSeconds() {
        return randomOnSeconds;
    }

    public boolean isLoop() {
        return loop;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAutonomousStart() {
        return autonomousStart;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }
}
