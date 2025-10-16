package dev.behindthescenery.botumi.blocks.entity;

import dev.behindthescenery.botumi.Botumi;
import dev.behindthescenery.botumi.ui.DomeScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.registry.Registries;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class DomeBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {
    public static final Identifier ID = Identifier.of(Botumi.MOD_ID, "dome_block_entity");

    private String protectedStructureId = "";
    private boolean enabled = true;

    public DomeBlockEntity(BlockPos pos, BlockState state) {
        super(Registries.BLOCK_ENTITY_TYPE.get(ID), pos, state);
    }

    public String getProtectedStructureId() {
        return protectedStructureId;
    }

    public void setProtectedStructureId(String id) {
        this.protectedStructureId = id == null ? "" : id;
        markChanged();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        this.enabled = value;
        markChanged();
    }

    private void markChanged() {
        if (this.world != null) {
            this.markDirty();
            BlockState st = this.getCachedState();
            this.world.updateListeners(this.pos, st, st, 3);
        }
    }

    @Override
    public void readNbt(net.minecraft.nbt.NbtCompound nbt, net.minecraft.registry.RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        if (nbt.contains("ProtectedStructureId")) {
            this.protectedStructureId = nbt.getString("ProtectedStructureId");
        } else {
            this.protectedStructureId = "";
        }
        this.enabled = nbt.getBoolean("ProtectedEnabled");
    }

    @Override
    protected void writeNbt(net.minecraft.nbt.NbtCompound nbt, net.minecraft.registry.RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        if (this.protectedStructureId != null && !this.protectedStructureId.isEmpty()) {
            nbt.putString("ProtectedStructureId", this.protectedStructureId);
        }
        nbt.putBoolean("ProtectedEnabled", this.enabled);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("screen.botumi.dome");
    }

    @Override
    public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new DomeScreenHandler(syncId, playerInventory, this);
    }
}
