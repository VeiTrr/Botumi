package dev.behindthescenery.botumi.registry;

import dev.behindthescenery.botumi.Botumi;
import dev.behindthescenery.botumi.blocks.DomeBlock;
import dev.behindthescenery.botumi.blocks.MinigamesBlock;
import dev.behindthescenery.botumi.items.TestItem;
import dev.behindthescenery.botumi.minigames.ui.MinigameScreenHandler;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class BotumiRegistry {

    public static final Block MINIGAMES_BLOCK = new MinigamesBlock();
    public static final Item MINIGAMES_BLOCK_ITEM = new BlockItem(MINIGAMES_BLOCK, new Item.Settings());
    public static final Block DOME_BLOCK = new DomeBlock();
    public static final Item DOME_BLOCK_ITEM = new BlockItem(DOME_BLOCK, new Item.Settings());
    public static final ItemGroup BOTUMI_ITEM_GROUP = ItemGroup.create(null, -1)
            .displayName(Text.translatable("itemGroup.botumi"))
            .icon(() -> new ItemStack(BotumiRegistry.MINIGAMES_BLOCK_ITEM))
            .entries((displayContext, entries) -> {
                entries.add(BotumiRegistry.MINIGAMES_BLOCK_ITEM);
                entries.add(BotumiRegistry.DOME_BLOCK_ITEM);
                entries.add(BotumiRegistry.TEST_ITEM);
            })
            .build();
    public static final Item TEST_ITEM = new TestItem();
    public static final Identifier BOTUMI_ITEM_GROUP_ID = Identifier.of(Botumi.MOD_ID, "botumi");
    public static ScreenHandlerType<MinigameScreenHandler> MINIGAME_SCREEN_HANDLER;
}
