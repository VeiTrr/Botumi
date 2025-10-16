package dev.behindthescenery.botumi.ui;

import dev.behindthescenery.botumi.blocks.entity.MinigamesBlockEntity;
import dev.behindthescenery.botumi.registry.BotumiRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;

import java.util.UUID;

public class MinigameScreenHandler extends ScreenHandler {
    public static final int MAX_TYPE_CHARS = 32;
    // 0: completed (0/1)
    // 1..4: UUID (4 ints)
    // 5: actionCode
    // 6: typeLen
    // 7..(7+MAX_TYPE_CHARS-1): type chars (int per char)
    public static final int PROPS = 7 + MAX_TYPE_CHARS;

    private final PropertyDelegate properties;
    private MinigamesBlockEntity beRef;

    public MinigameScreenHandler(int syncId, PlayerInventory inv) {
        super(BotumiRegistry.MINIGAME_SCREEN_HANDLER, syncId);
        this.properties = new ArrayPropertyDelegate(PROPS);
        this.addProperties(this.properties);
    }

    public MinigameScreenHandler(int syncId, PlayerInventory inv, MinigamesBlockEntity be) {
        this(syncId, inv);
        this.beRef = be;
        this.pushFromBlockEntity();
    }

    public PropertyDelegate getProperties() { return properties; }

    private void pushFromBlockEntity() {
        if (beRef == null) return;
        properties.set(0, beRef.isCompleted() ? 1 : 0);
        int[] buf = new int[4];
        MinigamesBlockEntity.uuidToInts(beRef.getCompletedBy(), buf, 0);
        for (int i = 0; i < 4; i++) properties.set(1 + i, buf[i]);
        properties.set(5, beRef.getActionCode());
        String type = beRef.getMinigameType();
        int len = Math.min(type.length(), MAX_TYPE_CHARS);
        properties.set(6, len);
        for (int i = 0; i < MAX_TYPE_CHARS; i++) {
            int code = i < len ? type.charAt(i) : 0;
            properties.set(7 + i, code);
        }
        sendContentUpdates();
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (beRef == null || player == null || player.getWorld().isClient) return false;
        if (id == 0) {
            if (!beRef.isCompleted()) {
                beRef.setCompleted(true);
                beRef.setCompletedBy(player.getUuid());
                beRef.executeConfiguredActions(player);
            } else {
                beRef.setCompleted(false);
                beRef.setCompletedBy(null);
                beRef.setActionCode(0);
            }
            pushFromBlockEntity();
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return null;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    public boolean isCompletedClient() { return properties.get(0) == 1; }

    public UUID getCompletedByUuidClient() {
        int a = properties.get(1);
        int b = properties.get(2);
        int c = properties.get(3);
        int d = properties.get(4);
        long most = ((long)a << 32) | (b & 0xFFFFFFFFL);
        long least = ((long)c << 32) | (d & 0xFFFFFFFFL);
        if (most == 0L && least == 0L) return null;
        return new UUID(most, least);
    }

    public int getActionCodeClient() { return properties.get(5); }

    public String getTypeClient() {
        int len = Math.max(0, Math.min(MAX_TYPE_CHARS, properties.get(6)));
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            int code = properties.get(7 + i);
            if (code != 0) sb.append((char) code);
        }
        return sb.toString();
    }
}
