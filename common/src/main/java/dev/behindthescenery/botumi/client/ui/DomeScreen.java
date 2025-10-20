package dev.behindthescenery.botumi.client.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Timer;
import java.util.TimerTask;

public class DomeScreen extends HandledScreen<DomeScreenHandler> {
    private ButtonWidget toggleButton;
    private ButtonWidget removeButton;

    private Timer propPollTimer;
    private Boolean lastDebugMode = null;

    public DomeScreen(DomeScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 120;
    }

    @Override
    protected void init() {
        super.init();
        startPollingOrBuild();
    }

    @Override
    public void close() {
        if (propPollTimer != null) {
            propPollTimer.cancel();
            propPollTimer = null;
        }
        super.close();
    }

    private void startPollingOrBuild() {
        if (isDataReady()) {
            buildUi();
            return;
        }
        if (propPollTimer != null) propPollTimer.cancel();
        propPollTimer = new Timer(true);
        propPollTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!isDataReady()) return;
                MinecraftClient mc = MinecraftClient.getInstance();
                mc.execute(() -> {
                    if (mc.currentScreen == DomeScreen.this) {
                        buildUi();
                    }
                });
                propPollTimer.cancel();
            }
        }, 50L, 50L);
    }

    private boolean isDataReady() {
        return this.handler != null && this.handler.getListSizeClient() > 0;
    }

    private void buildUi() {
        this.clearChildren();

        int cx = this.x + this.backgroundWidth / 2;
        int cy = this.y + this.backgroundHeight - 24;

        boolean debug = handler.isDebugModeClient();
        lastDebugMode = debug;

        if (debug) {
            this.toggleButton = ButtonWidget.builder(
                    handler.isEnabledClient()
                            ? Text.translatable("screen.botumi.disable").formatted(Formatting.RED)
                            : Text.translatable("screen.botumi.enable").formatted(Formatting.GREEN),
                    btn -> {
                        if (MinecraftClient.getInstance().interactionManager != null) {
                            MinecraftClient.getInstance().interactionManager.clickButton(this.handler.syncId, 0);
                        }
                    }
            ).dimensions(cx - 84, cy - 12, 80, 20).build();

            ButtonWidget prevButton = ButtonWidget.builder(Text.literal("<"), btn -> {
                if (MinecraftClient.getInstance().interactionManager != null) {
                    MinecraftClient.getInstance().interactionManager.clickButton(this.handler.syncId, 1);
                }
            }).dimensions(cx - 20, cy - 12, 20, 20).build();

            ButtonWidget nextButton = ButtonWidget.builder(Text.literal(">"), btn -> {
                if (MinecraftClient.getInstance().interactionManager != null) {
                    MinecraftClient.getInstance().interactionManager.clickButton(this.handler.syncId, 2);
                }
            }).dimensions(cx + 2, cy - 12, 20, 20).build();

            ButtonWidget protectButton = ButtonWidget.builder(Text.translatable("screen.botumi.protect"), btn -> {
                if (MinecraftClient.getInstance().interactionManager != null) {
                    MinecraftClient.getInstance().interactionManager.clickButton(this.handler.syncId, 3);
                }
            }).dimensions(cx + 26, cy - 12, 60, 20).build();

            this.addDrawableChild(this.toggleButton);
            this.addDrawableChild(prevButton);
            this.addDrawableChild(nextButton);
            this.addDrawableChild(protectButton);
            updateToggleText();
        } else {
            this.removeButton = ButtonWidget.builder(
                    Text.translatable("screen.botumi.remove_protection").formatted(Formatting.RED),
                    btn -> {
                        if (MinecraftClient.getInstance().interactionManager != null) {
                            MinecraftClient.getInstance().interactionManager.clickButton(this.handler.syncId, 4);
                        }
                    }
            ).dimensions(cx - 40, cy - 12, 80, 20).build();

            this.addDrawableChild(this.removeButton);
            updateRemoveButtonState();
        }
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        if (!isDataReady()) return;

        boolean debug = handler.isDebugModeClient();
        if (lastDebugMode == null || !lastDebugMode.equals(debug)) {
            buildUi();
            return;
        }

        if (debug) {
            updateToggleText();
        } else {
            updateRemoveButtonState();
        }
    }

    private void updateToggleText() {
        if (this.toggleButton == null) return;
        if (handler.isEnabledClient()) {
            this.toggleButton.setMessage(Text.translatable("screen.botumi.disable").formatted(Formatting.RED));
        } else {
            this.toggleButton.setMessage(Text.translatable("screen.botumi.enable").formatted(Formatting.GREEN));
        }
    }

    private void updateRemoveButtonState() {
        if (this.removeButton == null) return;
        boolean hasSaved = !(handler.getSavedIdClient() == null || handler.getSavedIdClient().isEmpty());
        int secs = handler.getDestroyCountdownSecondsClient();

        this.removeButton.active = hasSaved && secs == 0;

        if (secs > 0) {
            this.removeButton.setMessage(Text.translatable("screen.botumi.remove_protection_wait").formatted(Formatting.GRAY));
        } else {
            this.removeButton.setMessage(Text.translatable("screen.botumi.remove_protection").formatted(Formatting.RED));
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int left = this.x;
        int top = this.y;
        context.fill(left, top, left + this.backgroundWidth, top + this.backgroundHeight, 0xC0101010);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int left = this.x + 12;
        int top = this.y + 16;

        if (!isDataReady()) {
            context.drawText(this.textRenderer, Text.translatable("gui.loading"), left, top, 0xAAAAAA, false);
        } else if (handler.isDebugModeClient()) {
            String saved = handler.getSavedIdClient();
            String selected = handler.getSelectedIdClient();
            context.drawText(this.textRenderer, Text.literal("Saved: " + (saved == null ? "" : saved)), left, top, 0xFFFFFF, false);
            context.drawText(this.textRenderer, Text.literal("Selected: " + (selected == null ? "" : selected)), left, top + 12, 0xCCCCCC, false);
        } else {
            int secs = handler.getDestroyCountdownSecondsClient();
            if (secs > 0) {
                context.drawText(this.textRenderer, Text.translatable("screen.botumi.will_be_destroyed_in", secs).formatted(Formatting.RED), left, top, 0xFF4444, false);
            } else {
                context.drawText(this.textRenderer, Text.translatable("screen.botumi.remove_protection_hint").formatted(Formatting.GRAY), left, top, 0xCCCCCC, false);
            }
        }

        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, 8, 6, 0xFFFFFF, false);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
