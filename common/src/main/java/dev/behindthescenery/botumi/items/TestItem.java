package dev.behindthescenery.botumi.items;

import dev.behindthescenery.botumi.Botumi;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;

public class TestItem extends Item {

    public static Identifier ID = Identifier.of(Botumi.MOD_ID + "test_item");

    public TestItem() {
        super(new Item.Settings());
    }
}
