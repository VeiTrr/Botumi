package dev.behindthescenery.botumi.minigames.client;

import dev.behindthescenery.botumi.client.ui.MinigameScreen;
import dev.behindthescenery.botumi.minigames.api.MinigameClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class FrequencyMinigameClient extends MinigameClient {
    private final int targetFrequency;
    private final int targetAmplitude;

    private int playerFrequency = 2;
    private int playerAmplitude = 20;

    private ButtonWidget freqMinusBtn;
    private ButtonWidget freqPlusBtn;
    private ButtonWidget ampMinusBtn;
    private ButtonWidget ampPlusBtn;
    private ButtonWidget calibrateBtn;
    private ButtonWidget closeBtn;

    public FrequencyMinigameClient(MinigameScreen parent) {
        super(parent);
        java.util.Random rnd = new java.util.Random();
        this.targetFrequency = 3 + rnd.nextInt(4); // 3 to 6
        this.targetAmplitude = 25 + rnd.nextInt(4) * 5; // 25 to 40
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int oscW = 210;
        int oscH = 100;
        int oscX = centerX - oscW / 2;
        int oscY = centerY - oscH / 2 - 25;

        int controlY = oscY + oscH + 12;

        freqMinusBtn = this.addDrawableChild(ButtonWidget.builder(Text.literal("FREQ -"), btn -> {
            if (playerFrequency > 1) {
                playerFrequency--;
                playTuneSound();
            }
        }).dimensions(centerX - 95, controlY, 44, 20).build());

        freqPlusBtn = this.addDrawableChild(ButtonWidget.builder(Text.literal("FREQ +"), btn -> {
            if (playerFrequency < 8) {
                playerFrequency++;
                playTuneSound();
            }
        }).dimensions(centerX - 47, controlY, 44, 20).build());

        ampMinusBtn = this.addDrawableChild(ButtonWidget.builder(Text.literal("AMP -"), btn -> {
            if (playerAmplitude > 10) {
                playerAmplitude -= 5;
                playTuneSound();
            }
        }).dimensions(centerX + 3, controlY, 44, 20).build());

        ampPlusBtn = this.addDrawableChild(ButtonWidget.builder(Text.literal("AMP +"), btn -> {
            if (playerAmplitude < 50) {
                playerAmplitude += 5;
                playTuneSound();
            }
        }).dimensions(centerX + 51, controlY, 44, 20).build());

        calibrateBtn = this.addDrawableChild(ButtonWidget.builder(Text.literal("CALIBRATE LOCK"), btn -> {
            if (calculateResonance() >= 90) {
                setSolved(true);
                var mc = MinecraftClient.getInstance();
                if (mc.interactionManager != null) {
                    mc.interactionManager.clickButton(this.handler.syncId, 0);
                }
                if (mc.player != null) {
                    mc.player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.2F);
                }
                updateButtonStates();
            } else {
                var mc = MinecraftClient.getInstance();
                if (mc.player != null) {
                    mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1.0F, 0.5F);
                }
                if (mc.interactionManager != null) {
                    mc.interactionManager.clickButton(this.handler.syncId, 2);
                }
            }
        }).dimensions(centerX - 70, controlY + 24, 140, 20).build());

        closeBtn = this.addDrawableChild(ButtonWidget.builder(Text.translatable("screen.botumi.close"), btn -> {
            if (MinecraftClient.getInstance().currentScreen != null) {
                MinecraftClient.getInstance().currentScreen.close();
            }
        }).dimensions(centerX - 40, this.height - 28, 80, 20).build());

        updateButtonStates();
    }

    private void playTuneSound() {
        var mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.playSound(SoundEvents.BLOCK_BEACON_AMBIENT, 0.4F, 1.4F + playerFrequency * 0.1F);
        }
    }

    private int calculateResonance() {
        int freqDiff = Math.abs(targetFrequency - playerFrequency);
        int ampDiff = Math.abs(targetAmplitude - playerAmplitude);

        int penalty = freqDiff * 25 + ampDiff * 2;
        return Math.max(0, 100 - penalty);
    }

    private void updateButtonStates() {
        boolean completed = handler.isCompletedClient() || isSolved();
        if (freqMinusBtn != null) freqMinusBtn.active = !completed;
        if (freqPlusBtn != null) freqPlusBtn.active = !completed;
        if (ampMinusBtn != null) ampMinusBtn.active = !completed;
        if (ampPlusBtn != null) ampPlusBtn.active = !completed;
        if (calibrateBtn != null) {
            calibrateBtn.active = !completed;
            calibrateBtn.visible = !completed;
        }
        if (closeBtn != null) closeBtn.visible = completed;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int oscW = 210;
        int oscH = 100;
        int oscX = centerX - oscW / 2;
        int oscY = centerY - oscH / 2 - 25;

        // Terminal Background Panel
        ctx.fill(oscX - 10, oscY - 26, oscX + oscW + 10, oscY + oscH + 66, 0xDC0D1117);
        ctx.drawBorder(oscX - 10, oscY - 26, oscW + 20, oscH + 92, 0xFF30363D);

        // Header
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("◆ HARMONIC RESONANCE ◆").formatted(Formatting.GREEN, Formatting.BOLD), centerX, oscY - 20, 0xFF00FF66);

        // Oscilloscope Screen Box
        ctx.fill(oscX, oscY, oscX + oscW, oscY + oscH, 0xFF05100A);
        ctx.drawBorder(oscX, oscY, oscW, oscH, 0xFF008F39);

        // Grid lines
        for (int gy = oscY + 20; gy < oscY + oscH; gy += 20) {
            ctx.fill(oscX, gy, oscX + oscW, gy + 1, 0x3000FF66);
        }
        for (int gx = oscX + 25; gx < oscX + oscW; gx += 25) {
            ctx.fill(gx, oscY, gx + 1, oscY + oscH, 0x3000FF66);
        }
        // Center zero line
        int zeroY = oscY + oscH / 2;
        ctx.fill(oscX, zeroY, oscX + oscW, zeroY + 1, 0x6000FF66);

        // Wave animation offset
        float animOffset = (System.currentTimeMillis() % 4000L) / 4000.0f * (float) (Math.PI * 2);

        // Draw Target Waveform (faint cyan/blue dots)
        for (int x = 0; x < oscW; x += 2) {
            double rad = (x / (double) oscW) * (targetFrequency * Math.PI * 2) + animOffset;
            int wy = zeroY - (int) (Math.sin(rad) * targetAmplitude);
            wy = Math.max(oscY + 2, Math.min(oscY + oscH - 2, wy));
            ctx.fill(oscX + x, wy, oscX + x + 1, wy + 1, 0x8055FFFF);
        }

        // Draw Player Waveform (bright neon green line)
        for (int x = 0; x < oscW; x++) {
            double rad = (x / (double) oscW) * (playerFrequency * Math.PI * 2) + animOffset;
            int wy = zeroY - (int) (Math.sin(rad) * playerAmplitude);
            wy = Math.max(oscY + 2, Math.min(oscY + oscH - 2, wy));
            ctx.fill(oscX + x, wy, oscX + x + 1, wy + 2, 0xFF00FF66);
        }

        // Resonance Meter
        int resonance = calculateResonance();
        Formatting resColor = resonance >= 90 ? Formatting.GREEN : (resonance >= 60 ? Formatting.YELLOW : Formatting.RED);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("RESONANCE: " + resonance + "%").formatted(resColor, Formatting.BOLD), centerX, oscY + oscH - 12, 0xFFFFFFFF);

        if (handler.isCompletedClient() || isSolved()) {
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("HARMONIC LOCK OVERRIDDEN").formatted(Formatting.GREEN, Formatting.BOLD), centerX, oscY + oscH + 20, 0xFF55FF55);
            closeBtn.visible = true;
        }
    }
}
