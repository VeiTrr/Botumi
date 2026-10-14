package dev.behindthescenery.botumi.fabric.client;

import dev.behindthescenery.botumi.client.render.CoreSentinelBossEntityRenderer;
import dev.behindthescenery.botumi.client.render.DomeBlockEntityRenderer;
import dev.behindthescenery.botumi.client.render.DomeGuardianEntityRenderer;
import dev.behindthescenery.botumi.client.ui.DomeScreen;
import dev.behindthescenery.botumi.client.ui.MinigameScreen;
import dev.behindthescenery.botumi.registry.BotumiRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public final class BotumiFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(BotumiRegistry.MINIGAME_SCREEN_HANDLER, MinigameScreen::new);
        HandledScreens.register(BotumiRegistry.DOME_SCREEN_HANDLER, DomeScreen::new);

        BlockEntityRendererFactories.register(BotumiRegistry.DOME_BLOCK_ENTITY, DomeBlockEntityRenderer::new);

        EntityRendererRegistry.register(BotumiRegistry.DOME_GUARDIAN_ENTITY_TYPE, DomeGuardianEntityRenderer::new);
        EntityRendererRegistry.register(BotumiRegistry.CORE_SENTINEL_ENTITY_TYPE, CoreSentinelBossEntityRenderer::new);
    }
}

