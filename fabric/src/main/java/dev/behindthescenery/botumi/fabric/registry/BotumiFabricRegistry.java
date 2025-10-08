package dev.behindthescenery.botumi.fabric.registry;

import dev.behindthescenery.botumi.blocks.MinigamesBlock;
import dev.behindthescenery.botumi.blocks.entity.MinigamesBlockEntity;
import dev.behindthescenery.botumi.items.TestItem;
import dev.behindthescenery.botumi.registry.BotumiRegistry;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class BotumiFabricRegistry {
    public static void registerItemsAndBlocks() {
        Registry.register(Registries.BLOCK, MinigamesBlock.ID, BotumiRegistry.MINIGAMES_BLOCK);
        Registry.register(Registries.ITEM, MinigamesBlock.ID, BotumiRegistry.MINIGAMES_BLOCK_ITEM);
        Registry.register(Registries.BLOCK_ENTITY_TYPE, MinigamesBlockEntity.ID , BlockEntityType.Builder.create(MinigamesBlockEntity::new, BotumiRegistry.MINIGAMES_BLOCK).build());
        Registry.register(Registries.ITEM, TestItem.ID, BotumiRegistry.TEST_ITEM);
        Registry.register(Registries.ITEM_GROUP, BotumiRegistry.BOTUMI_ITEM_GROUP_ID, BotumiRegistry.BOTUMI_ITEM_GROUP);
    }

    public static void register() {
        registerItemsAndBlocks();
    }
}
