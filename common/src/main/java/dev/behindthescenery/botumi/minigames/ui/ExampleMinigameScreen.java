package dev.behindthescenery.botumi.minigames.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;

public class ExampleMinigameScreen extends HandledScreen<MinigameScreenHandler> {

    private ButtonWidget finishButton;

    public ExampleMinigameScreen(MinigameScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 120;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.x + this.backgroundWidth / 2;
        int cy = this.y + this.backgroundHeight - 24;

        this.finishButton = ButtonWidget.builder(Text.translatable("screen.botumi.finish"), btn -> {
            if (MinecraftClient.getInstance().interactionManager != null) {
                MinecraftClient.getInstance().interactionManager.clickButton(this.handler.syncId, 0);
            }
        }).dimensions(cx - 40, cy - 12, 80, 20).build();

        this.addDrawableChild(this.finishButton);
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

        int left = this.x + 16;
        int top = this.y + 16;

        String type = handler.getTypeClient();
        boolean completed = handler.isCompletedClient();
        int action = handler.getActionCodeClient();
        UUID who = handler.getCompletedByUuidClient();

        context.drawText(this.textRenderer, Text.translatable("screen.botumi.type", type), left, top, 0xFFFFFF, false);
        context.drawText(this.textRenderer, Text.translatable("screen.botumi.completed", completed ? Text.translatable("screen.botumi.yes").formatted(Formatting.GREEN) : Text.translatable("screen.botumi.no").formatted(Formatting.RED))
                , left, top + 12, 0xFFFFFF, false);
        if (who != null) {
            if (MinecraftClient.getInstance().getSession() != null && MinecraftClient.getInstance().getSession().getUsername() != null) {
                context.drawText(this.textRenderer, Text.translatable("screen.botumi.player", MinecraftClient.getInstance().getSession().getUsername())
                        , left, top + 24, 0xFFFFFF, false);
            } else {
                context.drawText(this.textRenderer, Text.translatable("screen.botumi.player_uuid", who)
                        , left, top + 24, 0xFFFFFF, false);
            }
        } else {
            context.drawText(this.textRenderer, Text.translatable("screen.botumi.player", Text.translatable("screen.botumi.none").formatted(Formatting.WHITE))
                    , left, top + 24, 0xFFFFFF, false);
        }

        context.drawText(this.textRenderer, Text.translatable("screen.botumi.action", switch (action) {
            case 1 -> Text.translatable("screen.botumi.reward_given");
            case 2 -> Text.translatable("screen.botumi.passage_opened");
            case 3 -> Text.translatable("screen.botumi.player_recorded");
            default -> Text.translatable("screen.botumi.none").formatted(Formatting.WHITE);
        }), left, top + 36, 0xFFFFFF, false);

        if (completed) {
            finishButton.setMessage(Text.translatable("screen.botumi.reset").formatted(Formatting.YELLOW));
        } else {
            finishButton.setMessage(Text.translatable("screen.botumi.finish"));
        }

        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, 8, 6, 0xFFFFFF, false);
    }
}
