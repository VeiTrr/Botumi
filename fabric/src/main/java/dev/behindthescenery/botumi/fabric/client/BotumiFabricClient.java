package dev.behindthescenery.botumi.fabric.client;

import dev.behindthescenery.botumi.ui.DomeScreen;
import dev.behindthescenery.botumi.ui.ExampleMinigameScreen;
import dev.behindthescenery.botumi.registry.BotumiRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public final class BotumiFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(BotumiRegistry.MINIGAME_SCREEN_HANDLER, ExampleMinigameScreen::new);
        HandledScreens.register(BotumiRegistry.DOME_SCREEN_HANDLER, DomeScreen::new);
    }
}

