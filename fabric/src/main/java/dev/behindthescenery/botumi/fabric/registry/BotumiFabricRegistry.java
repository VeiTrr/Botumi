package dev.behindthescenery.botumi.fabric.registry;

import dev.behindthescenery.botumi.Botumi;
import dev.behindthescenery.botumi.blocks.DomeBlock;
import dev.behindthescenery.botumi.blocks.MinigamesBlock;
import dev.behindthescenery.botumi.blocks.entity.DomeBlockEntity;
import dev.behindthescenery.botumi.blocks.entity.MinigamesBlockEntity;
import dev.behindthescenery.botumi.registry.BotumiRegistry;
import dev.behindthescenery.botumi.client.ui.DomeScreenHandler;
import dev.behindthescenery.botumi.client.ui.MinigameScreenHandler;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class BotumiFabricRegistry {

    public static void registerItemsAndBlocks() {
        Registry.register(Registries.BLOCK, MinigamesBlock.ID, BotumiRegistry.MINIGAMES_BLOCK);
        Registry.register(Registries.ITEM, MinigamesBlock.ID, BotumiRegistry.MINIGAMES_BLOCK_ITEM);
        Registry.register(Registries.BLOCK, DomeBlock.ID, BotumiRegistry.DOME_BLOCK);
        Registry.register(Registries.ITEM, DomeBlock.ID, BotumiRegistry.DOME_BLOCK_ITEM);
        BotumiRegistry.MINIGAMES_BLOCK_ENTITY = registerBlockEntity(MinigamesBlockEntity.ID, MinigamesBlockEntity::new, BotumiRegistry.MINIGAMES_BLOCK);
        BotumiRegistry.DOME_BLOCK_ENTITY = registerBlockEntity(DomeBlockEntity.ID, DomeBlockEntity::new, BotumiRegistry.DOME_BLOCK);
        Registry.register(Registries.ITEM_GROUP, BotumiRegistry.BOTUMI_ITEM_GROUP_ID, BotumiRegistry.BOTUMI_ITEM_GROUP);
        BotumiRegistry.MINIGAME_SCREEN_HANDLER = Registry.register(
                Registries.SCREEN_HANDLER,
                Identifier.of(Botumi.MOD_ID, "minigame"),
                new ScreenHandlerType<>(MinigameScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
        );
        BotumiRegistry.DOME_SCREEN_HANDLER = Registry.register(
                Registries.SCREEN_HANDLER,
                Identifier.of(Botumi.MOD_ID, "dome"),
                new ScreenHandlerType<>(DomeScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
        );

        BotumiRegistry.DOME_GUARDIAN_ENTITY_TYPE = Registry.register(
                Registries.ENTITY_TYPE,
                Identifier.of(Botumi.MOD_ID, "dome_guardian"),
                net.minecraft.entity.EntityType.Builder.create(dev.behindthescenery.botumi.entity.DomeGuardianEntity::new, net.minecraft.entity.SpawnGroup.MONSTER)
                        .dimensions(0.9f, 2.1f)
                        .build()
        );

        BotumiRegistry.CORE_SENTINEL_ENTITY_TYPE = Registry.register(
                Registries.ENTITY_TYPE,
                Identifier.of(Botumi.MOD_ID, "core_sentinel"),
                net.minecraft.entity.EntityType.Builder.create(dev.behindthescenery.botumi.entity.CoreSentinelBossEntity::new, net.minecraft.entity.SpawnGroup.MONSTER)
                        .dimensions(1.4f, 2.7f)
                        .build()
        );

        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                BotumiRegistry.DOME_GUARDIAN_ENTITY_TYPE,
                dev.behindthescenery.botumi.entity.DomeGuardianEntity.createGuardianAttributes()
        );

        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                BotumiRegistry.CORE_SENTINEL_ENTITY_TYPE,
                dev.behindthescenery.botumi.entity.CoreSentinelBossEntity.createBossAttributes()
        );
    }

    private static <T extends BlockEntity> BlockEntityType<T> registerBlockEntity(Identifier id,
                                                                                  BlockEntityType.BlockEntityFactory<? extends T> entityFactory,
                                                                                  Block... blocks) {
        return Registry.register(Registries.BLOCK_ENTITY_TYPE, id, BlockEntityType.Builder.<T>create(entityFactory, blocks).build());
    }

    public static void register() {
        registerItemsAndBlocks();
    }
}
