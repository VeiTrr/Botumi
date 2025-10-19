package dev.behindthescenery.botumi.client.ui;

import dev.behindthescenery.botumi.minigames.api.MinigameClientRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

import java.util.Timer;
import java.util.TimerTask;

public class MinigameScreen extends HandledScreen<MinigameScreenHandler> {
    public Boolean solved = false;
    private Timer typePollTimer;

    public MinigameScreen(MinigameScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }


    @Override
    protected void init() {
        super.init();
        String type = this.handler.getTypeClient();
        if (type == null || type.isEmpty()) {
            if (typePollTimer != null) typePollTimer.cancel();
            typePollTimer = new Timer(true);
            typePollTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    String t = MinigameScreen.this.handler.getTypeClient();
                    if (t != null && !t.isEmpty()) {
                        MinecraftClient mc = MinecraftClient.getInstance();
                        mc.execute(() -> mc.setScreen(MinigameClientRegistry.create(t, MinigameScreen.this)));
                        typePollTimer.cancel();
                    }
                }
            }, 50L, 50L);
        } else {
            MinecraftClient.getInstance().setScreen(MinigameClientRegistry.create(type, this));
        }
    }

    @Override
    public void close() {
        if (typePollTimer != null) {
            typePollTimer.cancel();
            typePollTimer = null;
        }
        if (solved && MinecraftClient.getInstance().interactionManager != null) {
            MinecraftClient.getInstance().interactionManager.clickButton(this.handler.syncId, 0);
        }
        super.close();
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
    }
}
