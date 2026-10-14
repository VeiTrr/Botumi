package dev.behindthescenery.botumi.client.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

public class DomeScreen extends HandledScreen<DomeScreenHandler> {
    private ButtonWidget toggleButton;
    private ButtonWidget removeButton;
    private Boolean lastDebugMode = null;
    private boolean uiBuilt = false;
    private float scrollOffset = 0.0f;
    private int textContentHeight = 0;

    public DomeScreen(DomeScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 252;
        this.backgroundHeight = 142;
    }

    @Override
    protected void init() {
        super.init();
        if (isDataReady()) {
            buildUi();
        }
    }

    private boolean isDataReady() {
        return this.handler != null && this.handler.getListSizeClient() > 0;
    }

    private void buildUi() {
        this.clearChildren();
        this.uiBuilt = true;

        int cx = this.x + this.backgroundWidth / 2;
        int cy = this.y + this.backgroundHeight - 22;

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
            ).dimensions(cx - 98, cy - 10, 80, 20).build();

            ButtonWidget prevButton = ButtonWidget.builder(Text.literal("◀"), btn -> {
                if (MinecraftClient.getInstance().interactionManager != null) {
                    MinecraftClient.getInstance().interactionManager.clickButton(this.handler.syncId, 1);
                }
            }).dimensions(cx - 14, cy - 10, 20, 20).build();

            ButtonWidget nextButton = ButtonWidget.builder(Text.literal("▶"), btn -> {
                if (MinecraftClient.getInstance().interactionManager != null) {
                    MinecraftClient.getInstance().interactionManager.clickButton(this.handler.syncId, 2);
                }
            }).dimensions(cx + 10, cy - 10, 20, 20).build();

            ButtonWidget protectButton = ButtonWidget.builder(Text.translatable("screen.botumi.protect"), btn -> {
                if (MinecraftClient.getInstance().interactionManager != null) {
                    MinecraftClient.getInstance().interactionManager.clickButton(this.handler.syncId, 3);
                }
            }).dimensions(cx + 34, cy - 10, 64, 20).build();

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
            ).dimensions(cx - 75, cy - 10, 150, 20).build();

            this.addDrawableChild(this.removeButton);
            updateRemoveButtonState();
        }
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        if (!isDataReady()) return;

        if (!uiBuilt) {
            buildUi();
            return;
        }

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

        // Modern Sci-Fi HUD Panel Design
        context.fill(left, top, left + this.backgroundWidth, top + this.backgroundHeight, 0xEB0F141C);
        context.drawBorder(left, top, this.backgroundWidth, this.backgroundHeight, 0xFF00ADB5);

        // Header separator line
        context.fill(left + 8, top + 22, left + this.backgroundWidth - 8, top + 23, 0x4000ADB5);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int left = this.x + 12;
        int top = this.y + 28;

        if (!isDataReady()) {
            context.drawText(this.textRenderer, Text.translatable("gui.loading"), left, top, 0xAAAAAA, false);
        } else if (handler.isDebugModeClient()) {
            String saved = handler.getSavedIdClient();
            String selected = handler.getSelectedIdClient();
            boolean enabled = handler.isEnabledClient();

            // Status Badge
            int badgeColor = enabled ? 0xFF00FF66 : 0xFFFF4444;
            String statusStr = enabled ? "ONLINE" : "OFFLINE";
            context.fill(left, top, left + 6, top + 6, badgeColor);
            context.drawText(this.textRenderer, Text.literal("SHIELD: " + statusStr).formatted(enabled ? Formatting.GREEN : Formatting.RED), left + 10, top - 1, badgeColor, false);

            context.drawText(this.textRenderer, Text.literal("Target: " + (saved == null || saved.isEmpty() ? "<none>" : saved)), left, top + 14, 0xEEEEEE, false);
            context.drawText(this.textRenderer, Text.literal("Selected: " + (selected == null || selected.isEmpty() ? "<none>" : selected)), left, top + 26, 0x88CCFF, false);
        } else {
            int secs = handler.getDestroyCountdownSecondsClient();
            if (secs > 0) {
                // Warning Alert Box with pulsating effect
                boolean pulse = (System.currentTimeMillis() / 400L) % 2 == 0;
                int alertBg = pulse ? 0x60FF0000 : 0x30FF0000;
                Text warningTitle = Text.translatable("screen.botumi.self_destruct_warning").formatted(Formatting.RED, Formatting.BOLD);
                int titleW = this.textRenderer.getWidth(warningTitle);
                int boxWidth = Math.min(this.backgroundWidth - 16, Math.max(this.backgroundWidth - 24, titleW + 24));
                int boxX = this.x + (this.backgroundWidth - boxWidth) / 2;
                int boxHeight = 44;
                context.fill(boxX, top, boxX + boxWidth, top + boxHeight, alertBg);
                context.drawBorder(boxX, top, boxWidth, boxHeight, 0xFFFF3333);

                int boxCenterX = boxX + boxWidth / 2;
                context.drawCenteredTextWithShadow(this.textRenderer, warningTitle, boxCenterX, top + 8, 0xFFFF5555);
                context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("screen.botumi.will_be_destroyed_in", secs).formatted(Formatting.WHITE), boxCenterX, top + 24, 0xFFFFFFFF);
            } else {
                context.drawText(this.textRenderer, Text.literal("FORCEFIELD GENERATOR").formatted(Formatting.AQUA, Formatting.BOLD), left, top, 0xFF00ADB5, false);
                int textAreaW = this.backgroundWidth - 24;
                int textAreaH = 65;
                this.textContentHeight = GuiTextUtil.drawScrollableText(
                        context,
                        this.textRenderer,
                        Text.translatable("screen.botumi.remove_protection_hint").formatted(Formatting.GRAY),
                        left,
                        top + 14,
                        textAreaW,
                        textAreaH,
                        this.scrollOffset,
                        0xAAAAAA,
                        false
                );
            }
        }

        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, this.textContentHeight - 65);
        if (maxScroll > 0) {
            this.scrollOffset = MathHelper.clamp(this.scrollOffset - (float) verticalAmount * 12.0f, 0.0f, (float) maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        Text titleText = Text.literal("◆ DOME CONTROLLER ◆").formatted(Formatting.AQUA, Formatting.BOLD);
        int titleW = this.textRenderer.getWidth(titleText);
        context.drawText(this.textRenderer, titleText, (this.backgroundWidth - titleW) / 2, 7, 0xFF00ADB5, false);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
