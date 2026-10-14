package dev.behindthescenery.botumi.neoforge;

import dev.behindthescenery.botumi.Botumi;
import dev.behindthescenery.botumi.blocks.DomeBlock;
import dev.behindthescenery.botumi.blocks.MinigamesBlock;
import dev.behindthescenery.botumi.blocks.entity.DomeBlockEntity;
import dev.behindthescenery.botumi.blocks.entity.MinigamesBlockEntity;
import dev.behindthescenery.botumi.client.render.CoreSentinelBossEntityRenderer;
import dev.behindthescenery.botumi.client.render.DomeBlockEntityRenderer;
import dev.behindthescenery.botumi.client.render.DomeGuardianEntityRenderer;
import dev.behindthescenery.botumi.client.ui.DomeScreen;
import dev.behindthescenery.botumi.client.ui.DomeScreenHandler;
import dev.behindthescenery.botumi.client.ui.MinigameScreen;
import dev.behindthescenery.botumi.client.ui.MinigameScreenHandler;
import dev.behindthescenery.botumi.entity.CoreSentinelBossEntity;
import dev.behindthescenery.botumi.entity.DomeGuardianEntity;
import dev.behindthescenery.botumi.registry.BotumiRegistry;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(Botumi.MOD_ID)
public final class BotumiNeoForge {
    public BotumiNeoForge(IEventBus modEventBus) {
        Botumi.init();
        modEventBus.addListener(BotumiNeoForge::onRegister);
        modEventBus.addListener(BotumiNeoForge::entityAttributeCreation);
    }

    public static void onRegister(RegisterEvent event) {
        event.register(Registries.BLOCK.getKey(), helper -> {
            helper.register(MinigamesBlock.ID, BotumiRegistry.MINIGAMES_BLOCK);
            helper.register(DomeBlock.ID, BotumiRegistry.DOME_BLOCK);
        });

        event.register(Registries.ITEM.getKey(), helper -> {
            helper.register(MinigamesBlock.ID, BotumiRegistry.MINIGAMES_BLOCK_ITEM);
            helper.register(DomeBlock.ID, BotumiRegistry.DOME_BLOCK_ITEM);
        });

        event.register(Registries.BLOCK_ENTITY_TYPE.getKey(), helper -> {
            BotumiRegistry.MINIGAMES_BLOCK_ENTITY = BlockEntityType.Builder.create(MinigamesBlockEntity::new, BotumiRegistry.MINIGAMES_BLOCK).build(null);
            BotumiRegistry.DOME_BLOCK_ENTITY = BlockEntityType.Builder.create(DomeBlockEntity::new, BotumiRegistry.DOME_BLOCK).build(null);
            helper.register(MinigamesBlockEntity.ID, BotumiRegistry.MINIGAMES_BLOCK_ENTITY);
            helper.register(DomeBlockEntity.ID, BotumiRegistry.DOME_BLOCK_ENTITY);
        });

        event.register(Registries.ENTITY_TYPE.getKey(), helper -> {
            BotumiRegistry.DOME_GUARDIAN_ENTITY_TYPE = EntityType.Builder.create(DomeGuardianEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.9f, 2.1f)
                    .build("botumi:dome_guardian");
            BotumiRegistry.CORE_SENTINEL_ENTITY_TYPE = EntityType.Builder.create(CoreSentinelBossEntity::new, SpawnGroup.MONSTER)
                    .dimensions(1.4f, 2.7f)
                    .build("botumi:core_sentinel");
            helper.register(Identifier.of(Botumi.MOD_ID, "dome_guardian"), BotumiRegistry.DOME_GUARDIAN_ENTITY_TYPE);
            helper.register(Identifier.of(Botumi.MOD_ID, "core_sentinel"), BotumiRegistry.CORE_SENTINEL_ENTITY_TYPE);
        });

        event.register(Registries.SCREEN_HANDLER.getKey(), helper -> {
            BotumiRegistry.MINIGAME_SCREEN_HANDLER = new ScreenHandlerType<>(MinigameScreenHandler::new, FeatureFlags.VANILLA_FEATURES);
            BotumiRegistry.DOME_SCREEN_HANDLER = new ScreenHandlerType<>(DomeScreenHandler::new, FeatureFlags.VANILLA_FEATURES);
            helper.register(Identifier.of(Botumi.MOD_ID, "minigame"), BotumiRegistry.MINIGAME_SCREEN_HANDLER);
            helper.register(Identifier.of(Botumi.MOD_ID, "dome"), BotumiRegistry.DOME_SCREEN_HANDLER);
        });

        event.register(Registries.ITEM_GROUP.getKey(), helper -> {
            helper.register(BotumiRegistry.BOTUMI_ITEM_GROUP_ID, BotumiRegistry.BOTUMI_ITEM_GROUP);
        });
    }

    public static void entityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(BotumiRegistry.DOME_GUARDIAN_ENTITY_TYPE, DomeGuardianEntity.createGuardianAttributes().build());
        event.put(BotumiRegistry.CORE_SENTINEL_ENTITY_TYPE, CoreSentinelBossEntity.createBossAttributes().build());
    }

    @EventBusSubscriber(modid = Botumi.MOD_ID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(BotumiRegistry.DOME_GUARDIAN_ENTITY_TYPE, DomeGuardianEntityRenderer::new);
            event.registerEntityRenderer(BotumiRegistry.CORE_SENTINEL_ENTITY_TYPE, CoreSentinelBossEntityRenderer::new);
            event.registerBlockEntityRenderer(BotumiRegistry.DOME_BLOCK_ENTITY, DomeBlockEntityRenderer::new);
        }

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(BotumiRegistry.MINIGAME_SCREEN_HANDLER, MinigameScreen::new);
            event.register(BotumiRegistry.DOME_SCREEN_HANDLER, DomeScreen::new);
        }
    }
}
