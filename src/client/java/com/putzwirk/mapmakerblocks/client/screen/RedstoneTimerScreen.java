package com.putzwirk.mapmakerblocks.client.screen;

import com.putzwirk.mapmakerblocks.Mapmakerblocks;
import com.putzwirk.mapmakerblocks.block.entity.RedstoneTimerBlockEntity;
import com.putzwirk.mapmakerblocks.networking.RedstoneTimerUpdatePayload;
import com.putzwirk.mapmakerblocks.screen.RedstoneTimerScreenHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class RedstoneTimerScreen extends HandledScreen<RedstoneTimerScreenHandler> {

    private static final Identifier GUI_TEXTURE =
            new Identifier(Mapmakerblocks.MOD_ID, "textures/gui/redstone_timer_gui.png");

    private TextFieldWidget secondsField;
    private TextFieldWidget randomDelayField;
    private TextFieldWidget onSecondsField;
    private TextFieldWidget randomOnSecondsField;

    private ButtonWidget loopButton;
    private ButtonWidget enabledButton;
    private ButtonWidget autonomousStartButton;

    private boolean loop;
    private boolean enabled;
    private boolean autonomousStart;

    private int applyFeedbackTicks = 0;

    public RedstoneTimerScreen(RedstoneTimerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);

        this.backgroundWidth = 230;
        this.backgroundHeight = 214;

        this.loop = handler.isLoop();
        this.enabled = handler.isEnabled();
        this.autonomousStart = handler.isAutonomousStart();
    }

    private static String formatFloat(float value) {
        if (value == (long) value) {
            return String.format(java.util.Locale.ROOT, "%d", (long) value);
        } else {
            return String.format(java.util.Locale.ROOT, "%s", value);
        }
    }

    private static boolean isValidFloatInput(String text) {
        if (text.isEmpty()) {
            return true;
        }
        return text.matches("^\\d*(\\.\\d*)?$");
    }

    @Override
    protected void init() {
        super.init();

        this.titleX = 8;
        this.titleY = 6;

        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;

        int fieldX = x + 125;
        int fieldWidth = 40;
        int fieldHeight = 18;

        this.secondsField = new TextFieldWidget(
                this.textRenderer,
                fieldX,
                y + 24,
                fieldWidth,
                fieldHeight,
                Text.translatable("screen.mmblocks.redstone_timer.delay")
        );
        this.secondsField.setText(formatFloat(this.handler.getSeconds()));
        this.secondsField.setMaxLength(8);
        this.secondsField.setTextPredicate(RedstoneTimerScreen::isValidFloatInput);
        this.addDrawableChild(this.secondsField);

        this.randomDelayField = new TextFieldWidget(
                this.textRenderer,
                fieldX,
                y + 47,
                fieldWidth,
                fieldHeight,
                Text.translatable("screen.mmblocks.redstone_timer.random_delay")
        );
        this.randomDelayField.setText(formatFloat(this.handler.getRandomDelay()));
        this.randomDelayField.setMaxLength(8);
        this.randomDelayField.setTextPredicate(RedstoneTimerScreen::isValidFloatInput);
        this.addDrawableChild(this.randomDelayField);

        this.onSecondsField = new TextFieldWidget(
                this.textRenderer,
                fieldX,
                y + 70,
                fieldWidth,
                fieldHeight,
                Text.translatable("screen.mmblocks.redstone_timer.on_time")
        );
        this.onSecondsField.setText(formatFloat(this.handler.getOnSeconds()));
        this.onSecondsField.setMaxLength(8);
        this.onSecondsField.setTextPredicate(RedstoneTimerScreen::isValidFloatInput);
        this.addDrawableChild(this.onSecondsField);

        this.randomOnSecondsField = new TextFieldWidget(
                this.textRenderer,
                fieldX,
                y + 93,
                fieldWidth,
                fieldHeight,
                Text.translatable("screen.mmblocks.redstone_timer.random_on_time")
        );
        this.randomOnSecondsField.setText(formatFloat(this.handler.getRandomOnSeconds()));
        this.randomOnSecondsField.setMaxLength(8);
        this.randomOnSecondsField.setTextPredicate(RedstoneTimerScreen::isValidFloatInput);
        this.addDrawableChild(this.randomOnSecondsField);

        this.loopButton = ButtonWidget.builder(getLoopText(), button -> {
            this.loop = !this.loop;
            this.loopButton.setMessage(getLoopText());
        }).dimensions(fieldX, y + 116, 24, 20).build();

        this.addDrawableChild(this.loopButton);

        this.enabledButton = ButtonWidget.builder(getEnabledText(), button -> {
            this.enabled = !this.enabled;
            this.enabledButton.setMessage(getEnabledText());
        }).dimensions(fieldX, y + 140, 40, 20).build();

        this.addDrawableChild(this.enabledButton);

        this.autonomousStartButton = ButtonWidget.builder(getAutonomousStartText(), button -> {
            this.autonomousStart = !this.autonomousStart;
            this.autonomousStartButton.setMessage(getAutonomousStartText());
        }).dimensions(x + 8, y + 164, 214, 18).build();

        this.addDrawableChild(this.autonomousStartButton);

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("screen.mmblocks.redstone_timer.apply"), button -> {

            String secondsText = this.secondsField.getText().trim();
            String randomDelayText = this.randomDelayField.getText().trim();
            String onSecondsText = this.onSecondsField.getText().trim();
            String randomOnSecondsText = this.randomOnSecondsField.getText().trim();

            if (!secondsText.isEmpty() && !onSecondsText.isEmpty()) {
                try {
                    float seconds = Math.min(
                            RedstoneTimerBlockEntity.MAX_SECONDS,
                            Math.max(0.0f, Float.parseFloat(secondsText))
                    );

                    float randomDelay = 0.0f;
                    if (!randomDelayText.isEmpty()) {
                        randomDelay = Math.min(
                                RedstoneTimerBlockEntity.MAX_SECONDS,
                                Math.max(0.0f, Float.parseFloat(randomDelayText))
                        );
                    }

                    float onSeconds = Math.min(
                            RedstoneTimerBlockEntity.MAX_SECONDS,
                            Math.max(0.05f, Float.parseFloat(onSecondsText))
                    );

                    float randomOnSeconds = 0.0f;
                    if (!randomOnSecondsText.isEmpty()) {
                        randomOnSeconds = Math.min(
                                RedstoneTimerBlockEntity.MAX_SECONDS,
                                Math.max(0.0f, Float.parseFloat(randomOnSecondsText))
                        );
                    }

                    this.secondsField.setText(formatFloat(seconds));
                    this.randomDelayField.setText(formatFloat(randomDelay));
                    this.onSecondsField.setText(formatFloat(onSeconds));
                    this.randomOnSecondsField.setText(formatFloat(randomOnSeconds));

                    PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                    new RedstoneTimerUpdatePayload(
                            this.handler.getPos(),
                            seconds,
                            randomDelay,
                            onSeconds,
                            randomOnSeconds,
                            this.loop,
                            this.enabled,
                            this.autonomousStart
                    ).write(buf);
                    ClientPlayNetworking.send(RedstoneTimerUpdatePayload.ID, buf);

                    this.applyFeedbackTicks = 40;
                } catch (NumberFormatException ignored) {
                }
            }

        }).dimensions(x + 8, y + 187, 214, 18).build());
    }

    private Text getLoopText() {
        return Text.literal(this.loop ? "[X]" : "[ ]");
    }

    private Text getEnabledText() {
        return Text.translatable(this.enabled ? "screen.mmblocks.redstone_timer.on" : "screen.mmblocks.redstone_timer.off");
    }

    private MutableText getAutonomousStartText() {
        return Text.translatable("screen.mmblocks.redstone_timer.autonomous_start")
                .formatted(this.autonomousStart ? Formatting.GREEN : Formatting.WHITE);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;

        context.drawTexture(
                GUI_TEXTURE,
                x,
                y,
                0,
                0,
                backgroundWidth,
                backgroundHeight,
                backgroundWidth,
                backgroundHeight
        );
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 0x404040, false);

        context.drawText(this.textRenderer, Text.translatable("screen.mmblocks.redstone_timer.delay_label"), 8, 28, 0x404040, false);
        context.drawText(this.textRenderer, Text.translatable("screen.mmblocks.redstone_timer.random_delay_label"), 8, 51, 0x404040, false);
        context.drawText(this.textRenderer, Text.translatable("screen.mmblocks.redstone_timer.on_time_label"), 8, 74, 0x404040, false);
        context.drawText(this.textRenderer, Text.translatable("screen.mmblocks.redstone_timer.random_on_time_label"), 8, 97, 0x404040, false);
        context.drawText(this.textRenderer, Text.translatable("screen.mmblocks.redstone_timer.loop_label"), 8, 120, 0x404040, false);
        context.drawText(this.textRenderer, Text.translatable("screen.mmblocks.redstone_timer.enabled_label"), 8, 144, 0x404040, false);

        int maxLabelX = 170;
        context.drawText(
                this.textRenderer,
                Text.translatable("screen.mmblocks.redstone_timer.max", (int) RedstoneTimerBlockEntity.MAX_SECONDS),
                maxLabelX,
                29,
                0x606060,
                false
        );

        context.drawText(
                this.textRenderer,
                Text.translatable("screen.mmblocks.redstone_timer.max", (int) RedstoneTimerBlockEntity.MAX_SECONDS),
                maxLabelX,
                52,
                0x606060,
                false
        );

        context.drawText(
                this.textRenderer,
                Text.translatable("screen.mmblocks.redstone_timer.max", (int) RedstoneTimerBlockEntity.MAX_SECONDS),
                maxLabelX,
                75,
                0x606060,
                false
        );

        context.drawText(
                this.textRenderer,
                Text.translatable("screen.mmblocks.redstone_timer.max", (int) RedstoneTimerBlockEntity.MAX_SECONDS),
                maxLabelX,
                98,
                0x606060,
                false
        );

        if (this.applyFeedbackTicks > 0) {

            Text appliedText = Text.translatable("screen.mmblocks.redstone_timer.applied")
                    .formatted(Formatting.GREEN);

            int textWidth = this.textRenderer.getWidth(appliedText);
            int x = (this.backgroundWidth / 2) - (textWidth / 2);

            int y = this.backgroundHeight + 8;

            context.drawText(
                    this.textRenderer,
                    appliedText,
                    x,
                    y,
                    0x00FF00,
                    true
            );
        }
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();

        if (this.applyFeedbackTicks > 0) {
            this.applyFeedbackTicks--;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);

        super.render(context, mouseX, mouseY, delta);

        this.secondsField.render(context, mouseX, mouseY, delta);
        this.randomDelayField.render(context, mouseX, mouseY, delta);
        this.onSecondsField.render(context, mouseX, mouseY, delta);
        this.randomOnSecondsField.render(context, mouseX, mouseY, delta);

        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
