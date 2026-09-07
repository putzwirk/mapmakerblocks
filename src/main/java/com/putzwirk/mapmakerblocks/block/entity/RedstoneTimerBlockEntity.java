package com.putzwirk.mapmakerblocks.block.entity;

import com.putzwirk.mapmakerblocks.networking.RedstoneTimerUpdatePayload;
import com.putzwirk.mapmakerblocks.screen.RedstoneTimerScreenData;
import com.putzwirk.mapmakerblocks.screen.RedstoneTimerScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class RedstoneTimerBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory {

    private float seconds = 0.0f;
    private float randomDelay = 0.0f;
    private float onSeconds = 1.0f;
    private float randomOnSeconds = 0.0f;

    public static final float MAX_SECONDS = 3600.0f;

    private boolean loop = false;
    private boolean enabled = true;
    private boolean running = false;
    private boolean autonomousStart = false;

    private int delayTicksRemaining = 0;
    private int onTicksRemaining = 0;

    public RedstoneTimerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REDSTONE_TIMER_BLOCK_ENTITY, pos, state);
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeFloat(this.seconds);
        buf.writeFloat(this.randomDelay);
        buf.writeFloat(this.onSeconds);
        buf.writeFloat(this.randomOnSeconds);
        buf.writeBoolean(this.loop);
        buf.writeBoolean(this.enabled);
        buf.writeBoolean(this.autonomousStart);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.mmblocks.redstone_timer");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new RedstoneTimerScreenHandler(
                syncId,
                playerInventory,
                new RedstoneTimerScreenData(this.pos, this.seconds, this.randomDelay, this.onSeconds, this.randomOnSeconds, this.loop, this.enabled, this.autonomousStart)
        );
    }

    public float getSeconds() {
        return seconds;
    }

    public void setSeconds(float seconds) {
        this.seconds = Math.min(MAX_SECONDS, Math.max(0.0f, seconds));
        markDirty();
    }

    public float getRandomDelay() {
        return randomDelay;
    }

    public void setRandomDelay(float randomDelay) {
        this.randomDelay = Math.min(MAX_SECONDS, Math.max(0.0f, randomDelay));
        markDirty();
    }

    public float getOnSeconds() {
        return onSeconds;
    }

    public void setOnSeconds(float onSeconds) {
        this.onSeconds = Math.min(MAX_SECONDS, Math.max(0.05f, onSeconds));
        markDirty();
    }

    public float getRandomOnSeconds() {
        return randomOnSeconds;
    }

    public void setRandomOnSeconds(float randomOnSeconds) {
        this.randomOnSeconds = Math.min(MAX_SECONDS, Math.max(0.0f, randomOnSeconds));
        markDirty();
    }

    public int getDelayTicksRemaining() {
        return delayTicksRemaining;
    }

    public void setDelayTicksRemaining(int delayTicksRemaining) {
        this.delayTicksRemaining = Math.max(0, delayTicksRemaining);
        markDirty();
    }

    public void decreaseDelayTicksRemaining(int ticks) {
        this.delayTicksRemaining = Math.max(0, this.delayTicksRemaining - ticks);
        markDirty();
    }

    public int getOnTicksRemaining() {
        return onTicksRemaining;
    }

    public void setOnTicksRemaining(int onTicksRemaining) {
        this.onTicksRemaining = Math.max(0, onTicksRemaining);
        markDirty();
    }

    public void decreaseOnTicksRemaining(int ticks) {
        this.onTicksRemaining = Math.max(0, this.onTicksRemaining - ticks);
        markDirty();
    }

    public boolean isLoop() {
        return loop;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
        markDirty();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        markDirty();
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
        markDirty();
    }

    public boolean isAutonomousStart() {
        return autonomousStart;
    }

    public void setAutonomousStart(boolean autonomousStart) {
        this.autonomousStart = autonomousStart;
        markDirty();
    }

    public void resetRuntime() {
        this.running = false;
        this.delayTicksRemaining = 0;
        this.onTicksRemaining = 0;
        markDirty();
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);

        nbt.putFloat("Seconds", seconds);
        nbt.putFloat("RandomDelay", randomDelay);
        nbt.putFloat("OnSeconds", onSeconds);
        nbt.putFloat("RandomOnSeconds", randomOnSeconds);
        nbt.putInt("DelayTicksRemaining", delayTicksRemaining);
        nbt.putInt("OnTicksRemaining", onTicksRemaining);

        nbt.putBoolean("Loop", loop);
        nbt.putBoolean("Enabled", enabled);
        nbt.putBoolean("Running", running);
        nbt.putBoolean("AutonomousStart", autonomousStart);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);

        seconds = nbt.getFloat("Seconds");
        randomDelay = nbt.contains("RandomDelay") ? nbt.getFloat("RandomDelay") : 0.0f;
        onSeconds = nbt.contains("OnSeconds") ? nbt.getFloat("OnSeconds") : 1.0f;
        randomOnSeconds = nbt.contains("RandomOnSeconds") ? nbt.getFloat("RandomOnSeconds") : 0.0f;
        delayTicksRemaining = nbt.getInt("DelayTicksRemaining");
        onTicksRemaining = nbt.getInt("OnTicksRemaining");

        loop = nbt.getBoolean("Loop");
        enabled = !nbt.contains("Enabled") || nbt.getBoolean("Enabled");
        running = nbt.getBoolean("Running");
        autonomousStart = nbt.getBoolean("AutonomousStart");

        if (seconds < 0.0f) {
            seconds = 0.0f;
        }

        if (seconds > MAX_SECONDS) {
            seconds = MAX_SECONDS;
        }

        if (randomDelay < 0.0f) {
            randomDelay = 0.0f;
        }

        if (randomDelay > MAX_SECONDS) {
            randomDelay = MAX_SECONDS;
        }

        if (onSeconds <= 0.0f) {
            onSeconds = 1.0f;
        }

        if (onSeconds > MAX_SECONDS) {
            onSeconds = MAX_SECONDS;
        }

        if (randomOnSeconds < 0.0f) {
            randomOnSeconds = 0.0f;
        }

        if (randomOnSeconds > MAX_SECONDS) {
            randomOnSeconds = MAX_SECONDS;
        }
    }
}
