package dev.behindthescenery.botumi.registry;

import dev.behindthescenery.botumi.blocks.MinigamesBlock;
import dev.behindthescenery.botumi.items.TestItem;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public class BotumiRegistry {

    public static final Block MINIGAMES_BLOCK = new MinigamesBlock();
    public static final Item MINIGAMES_BLOCK_ITEM = new BlockItem(MINIGAMES_BLOCK, new Item.Settings());
    public static final Item TEST_ITEM = new TestItem();

    public static ItemGroup Botumi = ItemGroup.create(null, -1)
            .displayName(Text.translatable("itemGroup.botumi"))
            .icon(() -> new ItemStack(BotumiRegistry.MINIGAMES_BLOCK_ITEM))
            .entries((displayContext, entries) -> {
                entries.add(BotumiRegistry.MINIGAMES_BLOCK_ITEM);
                entries.add(BotumiRegistry.TEST_ITEM);
            })
            .build();
}
