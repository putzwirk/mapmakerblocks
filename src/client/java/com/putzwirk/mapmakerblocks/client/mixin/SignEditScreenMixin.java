package com.putzwirk.mapmakerblocks.client.mixin;

import com.putzwirk.mapmakerblocks.ModBlocks;
import com.putzwirk.mapmakerblocks.block.WirelessRedstoneSignBlock;
import com.putzwirk.mapmakerblocks.networking.WirelessSignPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSignEditScreen.class)
public abstract class SignEditScreenMixin {

    @Shadow
    private SignBlockEntity blockEntity;

    @Unique
    private boolean wirelessInputMode = true;

    @Unique
    private ButtonWidget wirelessInputButton;

    @Unique
    private ButtonWidget wirelessOutputButton;

    @Inject(method = "init", at = @At("TAIL"))
    private void mmblocks_addWirelessModeButtons(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) {
            return;
        }

        BlockPos pos = this.blockEntity.getPos();
        if (!client.world.getBlockState(pos).isOf(ModBlocks.WIRELESS_REDSTONE_SIGN)) {
            return;
        }

        this.wirelessInputMode = client.world.getBlockState(pos).get(WirelessRedstoneSignBlock.INPUT);

        int w;
        int h = 20;
        int gap = 10;
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        Text inputLabel = Text.translatable("gui.mmblocks.wireless_sign.mode.input");
        Text outputLabel = Text.translatable("gui.mmblocks.wireless_sign.mode.output");
        w = Math.max(client.textRenderer.getWidth(inputLabel), client.textRenderer.getWidth(outputLabel)) + 20;
        int x = screenWidth / 2 - (w * 2 + gap) / 2;
        int y = Math.min(screenHeight / 4 + 168, screenHeight - h - 4);

        this.wirelessInputButton = ((ScreenInvoker) this).mmblocks_invokeAddDrawableChild(ButtonWidget.builder(
                        inputLabel,
                        b -> mmblocks_setMode(true))
                .dimensions(x, y, w, h)
                .build());
        this.wirelessOutputButton = ((ScreenInvoker) this).mmblocks_invokeAddDrawableChild(ButtonWidget.builder(
                        outputLabel,
                        b -> mmblocks_setMode(false))
                .dimensions(x + w + gap, y, w, h)
                .build());

        mmblocks_refreshButtons();
    }

    @Unique
    private void mmblocks_setMode(boolean input) {
        if (this.wirelessInputMode == input) return;
        this.wirelessInputMode = input;

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(this.blockEntity.getPos());
        buf.writeBoolean(input);
        ClientPlayNetworking.send(WirelessSignPackets.SET_MODE, buf);

        mmblocks_refreshButtons();
    }

    @Unique
    private void mmblocks_refreshButtons() {
        this.wirelessInputButton.setMessage(Text.translatable("gui.mmblocks.wireless_sign.mode.input"));
        this.wirelessOutputButton.setMessage(Text.translatable("gui.mmblocks.wireless_sign.mode.output"));
        ButtonWidget active = this.wirelessInputMode ? this.wirelessInputButton : this.wirelessOutputButton;
        active.setMessage(Text.literal("> ").append(active.getMessage()).append(" <"));
    }
}