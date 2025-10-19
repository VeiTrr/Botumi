package dev.behindthescenery.botumi.minigames.client;

import dev.behindthescenery.botumi.client.ui.MinigameScreen;
import dev.behindthescenery.botumi.client.ui.MinigameScreenHandler;
import dev.behindthescenery.botumi.minigames.api.MinigameClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;

public class DebugMinigameClient extends MinigameClient {

    private final MinigameScreenHandler minigameScreenHandler;
    private ButtonWidget finishButton;

    public DebugMinigameClient(MinigameScreen parent) {
        super(parent);
        this.minigameScreenHandler = parent.getScreenHandler();
    }

    @Override
    protected void init() {
        super.init();
        finishButton = ButtonWidget.builder(Text.translatable("screen.botumi.finish"), button -> {
            if (MinecraftClient.getInstance().interactionManager != null) {
                MinecraftClient.getInstance().interactionManager.clickButton(this.handler.syncId, isSolved() ? 1 : 0);
                setSolved(!isSolved());
            }
        }).dimensions(this.width / 2 - 50, this.height / 2 - 10, 100, 20).build();
        this.addDrawableChild(finishButton);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int left = (this.width / 2) - 100;
        int top = (this.height / 2) - 50;
        String type = minigameScreenHandler.getTypeClient();
        boolean completed = minigameScreenHandler.isCompletedClient();
        int action = minigameScreenHandler.getActionCodeClient();
        UUID who = minigameScreenHandler.getCompletedByUuidClient();
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
            case 2 -> Text.translatable("screen.botumi.function_run");
            case 3 -> Text.translatable("screen.botumi.player_recorded");
            default -> Text.translatable("screen.botumi.none").formatted(Formatting.WHITE);
        }), left, top + 36, 0xFFFFFF, false);

        if (completed) {
            finishButton.setMessage(Text.translatable("screen.botumi.reset").formatted(Formatting.YELLOW));
        } else {
            finishButton.setMessage(Text.translatable("screen.botumi.finish"));
        }
    }
}
