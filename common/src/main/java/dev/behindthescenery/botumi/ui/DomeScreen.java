package dev.behindthescenery.botumi.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class DomeScreen extends HandledScreen<DomeScreenHandler> {
    private ButtonWidget toggleButton;
    private ButtonWidget prevButton;
    private ButtonWidget nextButton;
    private ButtonWidget protectButton;

    public DomeScreen(DomeScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 120;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.x + this.backgroundWidth / 2;
        int cy = this.y + this.backgroundHeight - 24;

        this.toggleButton = ButtonWidget.builder(handler.isEnabledClient() ?
                        Text.translatable("screen.botumi.disable").formatted(Formatting.RED)
                        : Text.translatable("screen.botumi.enable").formatted(Formatting.GREEN)
                , btn -> {
                    if (MinecraftClient.getInstance().interactionManager != null) {
                        MinecraftClient.getInstance().interactionManager.clickButton(this.handler.syncId, 0);
                        updateButtonText();
                    }
                }).dimensions(cx - 70, cy - 12, 60, 20).build();

        this.prevButton = ButtonWidget.builder(Text.literal("<"), btn -> {
            if (MinecraftClient.getInstance().interactionManager != null) {
                MinecraftClient.getInstance().interactionManager.clickButton(this.handler.syncId, 1);
            }
        }).dimensions(cx - 20, cy - 12, 20, 20).build();

        this.nextButton = ButtonWidget.builder(Text.literal(">"), btn -> {
            if (MinecraftClient.getInstance().interactionManager != null) {
                MinecraftClient.getInstance().interactionManager.clickButton(this.handler.syncId, 2);
            }
        }).dimensions(cx + 2, cy - 12, 20, 20).build();

        this.protectButton = ButtonWidget.builder(Text.translatable("screen.botumi.protect"), btn -> {
            if (MinecraftClient.getInstance().interactionManager != null) {
                MinecraftClient.getInstance().interactionManager.clickButton(this.handler.syncId, 3);
            }
        }).dimensions(cx + 26, cy - 12, 60, 20).build();

        this.addDrawableChild(this.toggleButton);
        this.addDrawableChild(this.prevButton);
        this.addDrawableChild(this.nextButton);
        this.addDrawableChild(this.protectButton);
        updateButtonText();
    }

    private void updateButtonText() {
        if (handler.isEnabledClient()) {
            this.toggleButton.setMessage(Text.translatable("screen.botumi.disable").formatted(Formatting.RED));
        } else {
            this.toggleButton.setMessage(Text.translatable("screen.botumi.enable").formatted(Formatting.GREEN));
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

        boolean enabled = handler.isEnabledClient();
        String saved = handler.getSavedIdClient();
        String selected = handler.getSelectedIdClient();
        int total = handler.getListSizeClient();
        int idx = handler.getSelectedIndexClient();

        context.drawText(this.textRenderer, Text.translatable("screen.botumi.enabled",
                        enabled ? Text.translatable("screen.botumi.yes").formatted(Formatting.GREEN)
                                : Text.translatable("screen.botumi.no").formatted(Formatting.RED)),
                left, top, 0xFFFFFF, false);

        context.drawText(this.textRenderer, Text.translatable("screen.botumi.saved_structure",
                        saved == null || saved.isEmpty() ? Text.translatable("screen.botumi.none").formatted(Formatting.GRAY) : Text.literal(saved)),
                left, top + 12, 0xFFFFFF, false);

        context.drawText(this.textRenderer, Text.translatable("screen.botumi.selected_structure",
                        total > 0 ? Text.literal(selected + "  [" + (idx + 1) + "/" + total + "]")
                                : Text.translatable("screen.botumi.none").formatted(Formatting.GRAY)),
                left, top + 24, 0xFFFFFF, false);

        this.drawMouseoverTooltip(context, mouseX, mouseY);
        updateButtonText();
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, 8, 6, 0xFFFFFF, false);
    }
}
