package dev.behindthescenery.botumi.client.ui;

import dev.behindthescenery.botumi.minigames.api.MinigameClient;
import dev.behindthescenery.botumi.minigames.api.MinigameClientRegistry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public class MinigameScreen extends HandledScreen<MinigameScreenHandler> {
    public Boolean solved = false;
    private MinigameClient activeClient;
    private String loadedType = null;

    public MinigameScreen(MinigameScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        String type = this.handler.getTypeClient();
        if (type != null && !type.isEmpty()) {
            loadClient(type);
        }
    }

    public void loadClient(String type) {
        this.clearChildren();
        this.loadedType = type;
        this.activeClient = MinigameClientRegistry.create(type, this);
        if (this.activeClient != null) {
            this.activeClient.init(this.client, this.width, this.height);
        }
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        String type = this.handler.getTypeClient();
        if (type != null && !type.isEmpty() && !type.equals(this.loadedType)) {
            loadClient(type);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.activeClient != null) {
            this.activeClient.render(context, mouseX, mouseY, delta);
        } else {
            this.renderBackground(context, mouseX, mouseY, delta);
            super.render(context, mouseX, mouseY, delta);
            context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("gui.loading"), this.width / 2, this.height / 2, 0xAAAAAA);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.activeClient != null && this.activeClient.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.activeClient != null && this.activeClient.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.activeClient != null && this.activeClient.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        if (solved && this.client != null && this.client.interactionManager != null) {
            this.client.interactionManager.clickButton(this.handler.syncId, 0);
        }
        super.close();
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
