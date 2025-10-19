package dev.behindthescenery.botumi.blocks.entity;

import dev.behindthescenery.botumi.Botumi;
import dev.behindthescenery.botumi.client.ui.DomeScreenHandler;
import dev.behindthescenery.botumi.util.StructureGuard;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

import static dev.behindthescenery.botumi.client.render.DomeBlockEntityRenderer.normPi;

@SuppressWarnings("unused")
public class DomeBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {
    public static final Identifier ID = Identifier.of(Botumi.MOD_ID, "dome_block_entity");
    private final java.util.Map<BlockPos, Integer> barrierTtl = new java.util.HashMap<>();
    int tickCounter = 0;
    private String protectedStructureId = "";
    private boolean enabled = true;
    private double radius = 70.0;
    private boolean useStructureCenter = true;
    private boolean useStructureRadius = false;
    private Identifier textureId = Identifier.of("minecraft", "textures/misc/forcefield.png");
    private double textureTileSize = 1.0;
    private int domeColor = Color.cyan.getRGB();
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

        if (world instanceof ServerWorld sw && domeBlockEntity.enabled) {
            domeBlockEntity.enforceBarrier(sw);
            domeBlockEntity.decayTempColliders(sw);
        }
    }

    private boolean isAtGate(double py, double angle, double radius) {
        final double gateHeightRatio = 0.01;
        final double gateYCenter = radius * gateHeightRatio;
        final double gateYMin = gateYCenter - 1.0;
        final double gateYMax = gateYCenter + 1.0;

        if (py < gateYMin || py > gateYMax) return false;

        final double gateWidthBlocks = 2.0;
        final double rGate = Math.max(1.0e-3, Math.sqrt(Math.max(0.0, radius * radius - gateYCenter * gateYCenter)));
        final double halfAngleGate = (gateWidthBlocks * 0.5) / rGate;

        final double[] gateAngles = new double[]{0.0, Math.PI / 2.0, Math.PI, -Math.PI / 2.0};
        for (double ga : gateAngles) {
            double diff = Math.abs(normPi(angle - ga));
            if (diff <= halfAngleGate) return true;
        }
        return false;
    }

    private void decayTempColliders(ServerWorld sw) {
        java.util.Iterator<java.util.Map.Entry<BlockPos, Integer>> it = barrierTtl.entrySet().iterator();
        while (it.hasNext()) {
            java.util.Map.Entry<BlockPos, Integer> en = it.next();
            int ttl = en.getValue() - 1;
            if (ttl <= 0) {
                BlockPos p = en.getKey();
                if (sw.getBlockState(p).isOf(Blocks.BARRIER)) {
                    sw.setBlockState(p, Blocks.AIR.getDefaultState(), 3);
                }
                it.remove();
            } else {
                en.setValue(ttl);
            }
        }
    }

    private void addCollider(ServerWorld sw, BlockPos pos) {
        if (!sw.isInBuildLimit(pos)) return;
        var st = sw.getBlockState(pos);
        if (st.isAir() || st.isOf(Blocks.BARRIER)) {
            sw.setBlockState(pos, Blocks.BARRIER.getDefaultState(), 3);
            barrierTtl.put(pos, 3);
        }
    }

    private void placeBarrierPatch(ServerWorld sw, Vec3d c, double R, Vec3d rNow) {
        double py = rNow.y;
        if (py < 0.0 || py > R) return;
        double nx = rNow.x, ny = rNow.y, nz = rNow.z;
        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1.0e-6) return;
        nx /= len;
        ny /= len;
        nz /= len;

        Vec3d pShell = new Vec3d(c.x + nx * R, c.y + ny * R, c.z + nz * R);
        BlockPos base = BlockPos.ofFloored(pShell.x, pShell.y, pShell.z);

        int ax;
        int ay;
        int az;
        double anx = Math.abs(nx), any = Math.abs(ny), anz = Math.abs(nz);
        if (anx >= any && anx >= anz) {
            ax = (nx >= 0 ? 1 : -1);
            ay = 0;
            az = 0;
        } else if (any >= anx && any >= anz) {
            ax = 0;
            ay = (ny >= 0 ? 1 : -1);
            az = 0;
        } else {
            ax = 0;
            ay = 0;
            az = (nz >= 0 ? 1 : -1);
        }
        BlockPos step = new BlockPos(ax, ay, az);

        java.util.ArrayList<BlockPos> candidates = new java.util.ArrayList<>(27);
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (Math.abs(dy) + Math.abs(dx) + Math.abs(dz) <= 2) {
                        candidates.add(base.add(dx, dy, dz));
                    }
                }
            }
        }
        candidates.add(base.add(step));

        for (BlockPos p : candidates) {
            double cx = p.getX() + 0.5 - c.x;
            double cy = p.getY() + 0.5 - c.y;
            double cz = p.getZ() + 0.5 - c.z;
            double d = Math.sqrt(cx * cx + cy * cy + cz * cz);
            if (d < 1.0e-6) continue;
            if (Math.abs(d - R) > 1.25) continue;
            if (cy < 0.0 || cy > R) continue;
            double ang = Math.atan2(cz, cx);
            if (isAtGate(cy, ang, R)) continue;
            addCollider(sw, p);
        }
    }

    private void enforceBarrier(ServerWorld sw) {
        if (!this.enabled) return;

        final double R = getRenderRadius();
        if (R < 0.5) return;

        final Vec3d c = getDomeBaseCenter();

        final Box query = new Box(
                c.x - R - 2.0, c.y - 2.0, c.z - R - 2.0,
                c.x + R + 2.0, c.y + R + 2.0, c.z + R + 2.0
        );

        for (Entity e : sw.getOtherEntities(null, query)) {
            if (e instanceof PlayerEntity pe && pe.isSpectator()) continue;

            final Vec3d centerNow = e.getBoundingBox().getCenter();
            final Vec3d rNow = centerNow.subtract(c);
            double dNow = rNow.length();
            if (dNow < 1.0e-6) continue;

            if (Math.abs(dNow - R) <= 6.0) {
                placeBarrierPatch(sw, c, R, rNow);
            }
        }
    }


    public String getProtectedStructureId() {
        return protectedStructureId;
    }

    public void setProtectedStructureId(String id) {
        this.protectedStructureId = id == null ? "" : id;
        recalcEffectiveRadius();
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

    public boolean isUseStructureCenter() {
        return useStructureCenter;
    }

    public void setUseStructureCenter(boolean use) {
        this.useStructureCenter = use;
        recalcEffectiveRadius();
        markChanged();
    }

    public boolean isUseStructure() {
        return useStructureRadius;
    }

    public void setUseStructureRadius(boolean use) {
        this.useStructureRadius = use;
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

    public double getTextureTileSize() {
        return textureTileSize;
    }

    public void setTextureTileSize(double size) {
        this.textureTileSize = Math.max(0.25, size);
        markChanged();
    }

    public int getDomeColor() {
        return domeColor;
    }

    public void setDomeColor(int color) {
        this.domeColor = color;
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
        if (this.world == null) return;

        this.markDirty();

        if (!this.world.isClient) {
            BlockState st = this.getCachedState();
            this.world.updateListeners(this.pos, st, st, 3);
            ((ServerWorld) this.world).getChunkManager().markForUpdate(this.pos);
        }
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        if (nbt.contains("ProtectedStructureId")) this.protectedStructureId = nbt.getString("ProtectedStructureId");
        else this.protectedStructureId = "";

        this.enabled = nbt.getBoolean("ProtectedEnabled");

        if (nbt.contains("Radius")) this.radius = Math.max(1.0, nbt.getDouble("Radius"));
        if (nbt.contains("useStructureСenter")) this.useStructureCenter = nbt.getBoolean("useStructureСenter");
        if (nbt.contains("UseStructureRadius")) this.useStructureRadius = nbt.getBoolean("UseStructureRadius");
        if (nbt.contains("Texture")) this.textureId = Identifier.tryParse(nbt.getString("Texture"));
        if (this.textureId == null) this.textureId = Identifier.of("minecraft", "textures/misc/forcefield.png");
        if (nbt.contains("TextureTileSize")) this.textureTileSize = Math.max(0.25, nbt.getDouble("TextureTileSize"));
        if (nbt.contains("DomeColor")) this.domeColor = nbt.getInt("DomeColor");
        else this.domeColor = Color.cyan.getRGB();
        if (nbt.contains("ComputedRadius")) this.computedRadius = Math.max(1.0, nbt.getDouble("ComputedRadius"));
        else this.computedRadius = Math.max(1.0, this.radius);

        if (nbt.contains("DomeCenterX")) this.centerX = nbt.getDouble("DomeCenterX");
        if (nbt.contains("DomeCenterY")) this.centerY = nbt.getDouble("DomeCenterY");
        if (nbt.contains("DomeCenterZ")) this.centerZ = nbt.getDouble("DomeCenterZ");
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        if (this.protectedStructureId != null && !this.protectedStructureId.isEmpty()) {
            nbt.putString("ProtectedStructureId", this.protectedStructureId);
        }
        nbt.putBoolean("ProtectedEnabled", this.enabled);
        nbt.putDouble("Radius", Math.max(1.0, this.radius));
        nbt.putBoolean("useStructureСenter", this.useStructureCenter);
        nbt.putBoolean("UseStructureRadius", this.useStructureRadius);
        nbt.putString("Texture", this.textureId.toString());
        nbt.putDouble("TextureTileSize", Math.max(0.25, this.textureTileSize));
        nbt.putInt("DomeColor", this.domeColor);
        nbt.putDouble("ComputedRadius", Math.max(1.0, this.computedRadius));
        nbt.putDouble("DomeCenterX", this.centerX);
        nbt.putDouble("DomeCenterY", this.centerY);
        nbt.putDouble("DomeCenterZ", this.centerZ);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("ProtectedStructureId", this.protectedStructureId);
        nbt.putBoolean("ProtectedEnabled", this.enabled);
        nbt.putDouble("Radius", Math.max(1.0, this.radius));
        nbt.putBoolean("useStructureСenter", this.useStructureCenter);
        nbt.putBoolean("UseStructureRadius", this.useStructureRadius);
        nbt.putString("Texture", this.textureId.toString());
        nbt.putDouble("TextureTileSize", Math.max(0.25, this.textureTileSize));
        nbt.putInt("DomeColor", this.domeColor);
        nbt.putDouble("ComputedRadius", Math.max(1.0, this.computedRadius));
        nbt.putDouble("DomeCenterX", this.centerX);
        nbt.putDouble("DomeCenterY", this.centerY);
        nbt.putDouble("DomeCenterZ", this.centerZ);
        return nbt;
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
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

        if (useStructureCenter && protectedStructureId != null && !protectedStructureId.isEmpty()) {
            Box b = StructureGuard.getStructureData(sw, this.pos, protectedStructureId);
            if (b != null) {
                if (useStructureRadius) {
                    double maxSq = getMaxSq(b);
                    this.computedRadius = Math.max(1.0, Math.sqrt(maxSq));
                } else {
                    this.computedRadius = Math.max(1.0, this.radius);
                }
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
