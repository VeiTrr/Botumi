package dev.behindthescenery.botumi.client.ui;

import dev.behindthescenery.botumi.blocks.entity.DomeBlockEntity;
import dev.behindthescenery.botumi.registry.BotumiRegistry;
import dev.behindthescenery.botumi.util.StructureGuard;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;

import java.util.ArrayList;
import java.util.List;

public class DomeScreenHandler extends ScreenHandler {
    public static final int MAX_ID_CHARS = 64;
    // 0: enabled (0/1)
    // 1: listSize
    // 2: selectedIndex
    // 3: savedIdLen
    // 4..(4+MAX_ID_CHARS-1): savedId chars
    // 4+MAX_ID_CHARS: selectedIdLen
    // 5+MAX_ID_CHARS .. (5+2*MAX_ID_CHARS-1): selectedId chars
    // 5+2*MAX_ID_CHARS: debugMode (0/1)
    // 6+2*MAX_ID_CHARS: destroyCountdownSeconds
    public static final int IDX_DEBUG = 5 + 2 * MAX_ID_CHARS;
    public static final int IDX_COUNTDOWN = 6 + 2 * MAX_ID_CHARS;
    public static final int PROPS = 7 + 2 * MAX_ID_CHARS;

    private final PropertyDelegate properties;
    private DomeBlockEntity beRef;

    private List<String> availableIds = new ArrayList<>();
    private int selectedIndex = 0;

    public DomeScreenHandler(int syncId, PlayerInventory inv) {
        super(BotumiRegistry.DOME_SCREEN_HANDLER, syncId);
        this.properties = new ArrayPropertyDelegate(PROPS);
        this.addProperties(this.properties);
    }

    public DomeScreenHandler(int syncId, PlayerInventory inv, DomeBlockEntity be) {
        this(syncId, inv);
        this.beRef = be;
        buildAvailableIds();
        pushFromBlockEntity();
    }

    private void buildAvailableIds() {
        availableIds.clear();
        if (beRef != null && beRef.getWorld() != null && !beRef.getWorld().isClient) {
            availableIds = StructureGuard.getContainingStructureIds((net.minecraft.server.world.ServerWorld) beRef.getWorld(), beRef.getPos());
        }
        if (availableIds.isEmpty()) {
            availableIds.add("");
        }
        selectedIndex = 0;
    }

    public void syncFromBlockEntity() {
        pushFromBlockEntity();
    }

    public boolean isForBlockEntity(DomeBlockEntity be) {
        return this.beRef == be;
    }

    private void pushFromBlockEntity() {
        if (beRef == null) return;
        properties.set(0, beRef.isEnabled() ? 1 : 0);
        properties.set(1, availableIds.size());
        properties.set(2, selectedIndex);

        writeString(3, 4, beRef.getProtectedStructureId());

        String sel = availableIds.get(Math.max(0, Math.min(selectedIndex, availableIds.size() - 1)));
        int selLenIdx = 4 + MAX_ID_CHARS;
        int selCharsStart = 5 + MAX_ID_CHARS;
        writeString(selLenIdx, selCharsStart, sel);

        properties.set(IDX_DEBUG, beRef.isDebugMode() ? 1 : 0);
        properties.set(IDX_COUNTDOWN, beRef.getDestroyCountdownSeconds());

        sendContentUpdates();
    }

    private void writeString(int lenIndex, int charsStart, String s) {
        String str = s == null ? "" : s;
        int len = Math.min(str.length(), MAX_ID_CHARS);
        properties.set(lenIndex, len);
        for (int i = 0; i < MAX_ID_CHARS; i++) {
            int code = i < len ? str.charAt(i) : 0;
            properties.set(charsStart + i, code);
        }
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (beRef == null || player == null || player.getWorld().isClient) return false;

        switch (id) {
            case 0 -> beRef.setEnabled(!beRef.isEnabled());
            case 1 -> {
                int size = availableIds.size();
                if (size > 0) {
                    selectedIndex = (selectedIndex - 1 + size) % size;
                }
            }
            case 2 -> {
                int size = availableIds.size();
                if (size > 0) {
                    selectedIndex = (selectedIndex + 1) % size;
                }
            }
            case 3 -> {
                if (!availableIds.isEmpty()) {
                    String sel = availableIds.get(Math.max(0, Math.min(selectedIndex, availableIds.size() - 1)));
                    beRef.setProtectedStructureId(sel);
                    beRef.setEnabled(true);
                }
            }
            case 4 -> {
                beRef.scheduleStructureDestruction();
            }
            default -> {
                return false;
            }
        }
        pushFromBlockEntity();
        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return null;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    public boolean isEnabledClient() {
        return properties.get(0) == 1;
    }

    public int getListSizeClient() {
        return Math.max(0, properties.get(1));
    }

    public int getSelectedIndexClient() {
        return Math.max(0, properties.get(2));
    }

    public String getSavedIdClient() {
        return readString(3, 4);
    }

    public String getSelectedIdClient() {
        int selLenIdx = 4 + MAX_ID_CHARS;
        int selCharsStart = 5 + MAX_ID_CHARS;
        return readString(selLenIdx, selCharsStart);
    }

    public boolean isDebugModeClient() {
        return properties.get(IDX_DEBUG) == 1;
    }

    public int getDestroyCountdownSecondsClient() {
        return Math.max(0, properties.get(IDX_COUNTDOWN));
    }

    private String readString(int lenIndex, int charsStart) {
        int len = Math.max(0, Math.min(MAX_ID_CHARS, properties.get(lenIndex)));
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            int code = properties.get(charsStart + i);
            if (code != 0) sb.append((char) code);
        }
        return sb.toString();
    }

    public PropertyDelegate getProperties() {
        return properties;
    }
}
