package dev.behindthescenery.botumi.fabric.registry;

import dev.behindthescenery.botumi.Botumi;
import dev.behindthescenery.botumi.blocks.DomeBlock;
import dev.behindthescenery.botumi.blocks.MinigamesBlock;
import dev.behindthescenery.botumi.blocks.entity.DomeBlockEntity;
import dev.behindthescenery.botumi.blocks.entity.MinigamesBlockEntity;
import dev.behindthescenery.botumi.items.TestItem;
import dev.behindthescenery.botumi.ui.DomeScreenHandler;
import dev.behindthescenery.botumi.ui.MinigameScreenHandler;
import dev.behindthescenery.botumi.registry.BotumiRegistry;
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
        Registry.register(Registries.BLOCK_ENTITY_TYPE, MinigamesBlockEntity.ID , BlockEntityType.Builder.create(MinigamesBlockEntity::new, BotumiRegistry.MINIGAMES_BLOCK).build());
        Registry.register(Registries.BLOCK_ENTITY_TYPE, DomeBlockEntity.ID , BlockEntityType.Builder.create(DomeBlockEntity::new, BotumiRegistry.DOME_BLOCK).build());
        Registry.register(Registries.ITEM, TestItem.ID, BotumiRegistry.TEST_ITEM);
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
    }

    public static void register() {
        registerItemsAndBlocks();
    }
}
