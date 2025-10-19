package dev.behindthescenery.botumi.minigames.api;

import dev.behindthescenery.botumi.client.ui.MinigameScreen;
import dev.behindthescenery.botumi.client.ui.MinigameScreenHandler;
import net.minecraft.client.gui.screen.Screen;

public class MinigameClient extends Screen {
    protected final MinigameScreen parent;
    protected final MinigameScreenHandler handler;

    public MinigameClient(MinigameScreen parent) {
        super(parent.getTitle());
        this.parent = parent;
        this.handler = parent.getScreenHandler();
    }

    public boolean isSolved() {
        return (parent.solved || handler.isCompletedClient());
    }

    public void setSolved(boolean solved) {
        parent.solved = solved;
    }

    @Override
    public void close() {
        parent.close();
        super.close();
    }
}
