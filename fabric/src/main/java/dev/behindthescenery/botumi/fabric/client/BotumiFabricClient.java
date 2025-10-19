package dev.behindthescenery.botumi.fabric.client;

import dev.behindthescenery.botumi.client.render.DomeBlockEntityRenderer;
import dev.behindthescenery.botumi.registry.BotumiRegistry;
import dev.behindthescenery.botumi.client.ui.DomeScreen;
import dev.behindthescenery.botumi.client.ui.MinigameScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public final class BotumiFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(BotumiRegistry.MINIGAME_SCREEN_HANDLER, MinigameScreen::new);
        HandledScreens.register(BotumiRegistry.DOME_SCREEN_HANDLER, DomeScreen::new);

        BlockEntityRendererFactories.register(BotumiRegistry.DOME_BLOCK_ENTITY, DomeBlockEntityRenderer::new);
    }
}

