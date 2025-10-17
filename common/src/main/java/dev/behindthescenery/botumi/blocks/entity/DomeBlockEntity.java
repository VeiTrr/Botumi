package dev.behindthescenery.botumi.blocks.entity;

import dev.behindthescenery.botumi.Botumi;
import dev.behindthescenery.botumi.ui.DomeScreenHandler;
import dev.behindthescenery.botumi.util.StructureGuard;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class DomeBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {
    public static final Identifier ID = Identifier.of(Botumi.MOD_ID, "dome_block_entity");
    int tickCounter = 0;
    private String protectedStructureId = "";
    private boolean enabled = true;
    private double radius = 70.0;
    private boolean useStructureSize = true;
    private Identifier textureId = Identifier.of("minecraft", "textures/misc/forcefield.png");
    private double computedRadius = radius;
    private double centerX;
    private double centerY;
    private double centerZ;

    public DomeBlockEntity(BlockPos pos, BlockState state) {
        super(Registries.BLOCK_ENTITY_TYPE.get(ID), pos, state);
        this.centerX = pos.getX() + 0.5;
        this.centerY = pos.getY();
        this.centerZ = pos.getZ() + 0.5;
    }

    public static void tick(World world, BlockPos blockPos, BlockState blockState, DomeBlockEntity domeBlockEntity) {
        if (world.isClient) return;

        domeBlockEntity.tickCounter++;
        if (domeBlockEntity.tickCounter >= 20) {
            domeBlockEntity.tickCounter = 0;
            domeBlockEntity.recalcEffectiveRadius();
            domeBlockEntity.markChanged();
        }

    }

    public String getProtectedStructureId() {
        return protectedStructureId;
    }

    public void setProtectedStructureId(String id) {
        this.protectedStructureId = id == null ? "" : id;
        markChanged();
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double r) {
        this.radius = Math.max(1.0, r);
        recalcEffectiveRadius();
        markChanged();
    }

    public boolean isUseStructureSize() {
        return useStructureSize;
    }

    public void setUseStructureSize(boolean use) {
        this.useStructureSize = use;
        recalcEffectiveRadius();
        markChanged();
    }

    public Identifier getTextureId() {
        return textureId;
    }

    public void setTextureId(Identifier id) {
        this.textureId = (id == null) ? Identifier.of("minecraft", "textures/misc/forcefield.png") : id;
        markChanged();
    }

    public double getRenderRadius() {
        return Math.max(1.0, computedRadius > 0 ? computedRadius : radius);
    }

    public Vec3d getCenter() {
        return Vec3d.ofCenter(this.pos);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        this.enabled = value;
        markChanged();
    }

    public Vec3d getDomeBaseCenter() {
        return new Vec3d(centerX, centerY, centerZ);
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
        if (nbt.contains("ProtectedStructureId")) {
            this.protectedStructureId = nbt.getString("ProtectedStructureId");
        } else {
            this.protectedStructureId = "";
        }
        this.enabled = nbt.getBoolean("ProtectedEnabled");

        if (nbt.contains("Radius")) this.radius = Math.max(1.0, nbt.getDouble("Radius"));
        if (nbt.contains("UseStructureSize")) this.useStructureSize = nbt.getBoolean("UseStructureSize");
        if (nbt.contains("Texture")) this.textureId = Identifier.tryParse(nbt.getString("Texture"));
        if (this.textureId == null) this.textureId = Identifier.of("minecraft", "textures/misc/forcefield.png");
        if (nbt.contains("ComputedRadius")) this.computedRadius = Math.max(1.0, nbt.getDouble("ComputedRadius"));
        else this.computedRadius = Math.max(1.0, this.radius);

        if (nbt.contains("DomeCenterX")) {
            this.centerX = nbt.getDouble("DomeCenterX");
        }
        if (nbt.contains("DomeCenterY")) {
            this.centerY = nbt.getDouble("DomeCenterY");
        }
        if (nbt.contains("DomeCenterZ")){
            this.centerZ = nbt.getDouble("DomeCenterZ");
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        if (this.protectedStructureId != null && !this.protectedStructureId.isEmpty()) {
            nbt.putString("ProtectedStructureId", this.protectedStructureId);
        }
        nbt.putBoolean("ProtectedEnabled", this.enabled);
        nbt.putDouble("Radius", Math.max(1.0, this.radius));
        nbt.putBoolean("UseStructureSize", this.useStructureSize);
        nbt.putString("Texture", this.textureId.toString());
        nbt.putDouble("ComputedRadius", Math.max(1.0, this.computedRadius));

        nbt.putDouble("DomeCenterX", this.centerX);
        nbt.putDouble("DomeCenterY", this.centerY);
        nbt.putDouble("DomeCenterZ", this.centerZ);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("screen.botumi.dome");
    }

    @Override
    public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new DomeScreenHandler(syncId, playerInventory, this);
    }

    public void recalcEffectiveRadius() {
        if (!(this.world instanceof ServerWorld sw)) {
            this.computedRadius = Math.max(1.0, this.radius);
            return;
        }

        if (useStructureSize && protectedStructureId != null && !protectedStructureId.isEmpty()) {
            Box b = StructureGuard.getStructureData(sw, this.pos, protectedStructureId);
            if (b != null) {
                double maxSq = getMaxSq(b);
                this.computedRadius = Math.max(1.0, Math.sqrt(maxSq));
                this.centerX = b.getCenter().x;
                this.centerY = this.pos.getY();
                this.centerZ = b.getCenter().z;
            }
        }
    }

    private double getMaxSq(Box b) {
        Vec3d c = new Vec3d(b.getCenter().getX(), this.pos.getY(), b.getCenter().getZ());
        double maxSq = 0.0;
        for (double x : new double[]{b.minX, b.maxX}) {
            for (double y : new double[]{b.minY, b.maxY}) {
                for (double z : new double[]{b.minZ, b.maxZ}) {
                    double dx = x - c.x;
                    double dy = y - c.y;
                    double dz = z - c.z;
                    double d2 = dx * dx + dy * dy + dz * dz;
                    if (d2 > maxSq) maxSq = d2;
                }
            }
        }
        return maxSq;
    }

    @Override
    public void setWorld(World world) {
        super.setWorld(world);
        if (!world.isClient) {
            recalcEffectiveRadius();
        }
    }
}
