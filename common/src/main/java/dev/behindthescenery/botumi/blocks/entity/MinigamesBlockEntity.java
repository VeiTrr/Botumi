package dev.behindthescenery.botumi.blocks.entity;

import dev.behindthescenery.botumi.Botumi;
import dev.behindthescenery.botumi.client.ui.MinigameScreenHandler;
import dev.behindthescenery.botumi.minigames.MinigameActions;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class MinigamesBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {

    public static final Identifier ID = Identifier.of(Botumi.MOD_ID, "minigames_block_entity");

    private String minigameType = "botumi:minigame_debug";
    private boolean completed = false;
    @Nullable
    private UUID completedBy = null;
    // 0-none, 1-reward_given, 2-passage_opened, 3-player_recorded
    private int actionCode = 0;

    private NbtCompound action = new NbtCompound();

    public MinigamesBlockEntity(BlockPos pos, BlockState state) {
        super(Registries.BLOCK_ENTITY_TYPE.get(ID), pos, state);
    }

    public static void uuidToInts(@Nullable UUID uuid, int[] out, int offset) {
        long most = 0L, least = 0L;
        if (uuid != null) {
            most = uuid.getMostSignificantBits();
            least = uuid.getLeastSignificantBits();
        }
        out[offset] = (int) (most >>> 32);
        out[offset + 1] = (int) most;
        out[offset + 2] = (int) (least >>> 32);
        out[offset + 3] = (int) least;
    }

    @Nullable
    public static UUID intsToUuid(int[] in, int offset) {
        long most = ((long) in[offset] << 32) | (in[offset + 1] & 0xFFFFFFFFL);
        long least = ((long) in[offset + 2] << 32) | (in[offset + 3] & 0xFFFFFFFFL);
        if (most == 0L && least == 0L) return null;
        return new UUID(most, least);
    }

    public String getMinigameType() {
        return minigameType;
    }

    public void setMinigameType(String value) {
        this.minigameType = value;
        markChanged();
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean value) {
        this.completed = value;
        markChanged();
    }

    @Nullable
    public UUID getCompletedBy() {
        return completedBy;
    }

    public void setCompletedBy(@Nullable UUID uuid) {
        this.completedBy = uuid;
        markChanged();
    }

    public int getActionCode() {
        return actionCode;
    }

    public void setActionCode(int code) {
        this.actionCode = code;
        markChanged();
    }

    public NbtCompound getAction() {
        return action;
    }

    public void setAction(@Nullable NbtCompound value) {
        this.action = value == null ? new NbtCompound() : value;
        markChanged();
    }

    public void executeConfiguredActions(PlayerEntity player) {
        int code = MinigameActions.execute(this, player);
        setActionCode(code);
    }

    private void markChanged() {
        if (this.world != null) {
            this.markDirty();
            BlockState st = this.getCachedState();
            this.world.updateListeners(this.pos, st, st, 3);
        }
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        this.minigameType = nbt.getString("MinigameType");
        this.completed = nbt.getBoolean("Completed");
        if (nbt.contains("CompletedByMost") && nbt.contains("CompletedByLeast")) {
            long most = nbt.getLong("CompletedByMost");
            long least = nbt.getLong("CompletedByLeast");
            this.completedBy = new UUID(most, least);
        } else {
            this.completedBy = null;
        }
        this.actionCode = nbt.getInt("ActionCode");
        if (nbt.contains("Action")) {
            this.action = nbt.getCompound("Action");
        } else {
            this.action = new NbtCompound();
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putString("MinigameType", this.minigameType);
        nbt.putBoolean("Completed", this.completed);
        if (this.completedBy != null) {
            nbt.putLong("CompletedByMost", this.completedBy.getMostSignificantBits());
            nbt.putLong("CompletedByLeast", this.completedBy.getLeastSignificantBits());
        }
        nbt.putInt("ActionCode", this.actionCode);
        if (this.action != null && !this.action.isEmpty()) {
            nbt.put("Action", this.action);
        }
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("screen.botumi.minigame");
    }

    @Override
    public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new MinigameScreenHandler(syncId, inv, this);
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        NbtCompound nbt = new NbtCompound();
        writeNbt(nbt, registryLookup);
        return nbt;
    }
}
