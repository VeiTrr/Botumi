package dev.behindthescenery.botumi.blocks.entity;

import dev.behindthescenery.botumi.Botumi;
import dev.behindthescenery.botumi.client.ui.DomeScreenHandler;
import dev.behindthescenery.botumi.util.StructureGuard;
import dev.behindthescenery.botumi.util.TerrainRegenerator;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.block.StainedGlassBlock;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.behindthescenery.botumi.client.render.DomeBlockEntityRenderer;
import dev.behindthescenery.botumi.entity.CoreSentinelBossEntity;
import dev.behindthescenery.botumi.entity.DomeGuardianEntity;
import dev.behindthescenery.botumi.registry.BotumiRegistry;
import net.minecraft.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import static dev.behindthescenery.botumi.client.render.DomeBlockEntityRenderer.normPi;

@SuppressWarnings("unused")
public class DomeBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {
    public static final Identifier ID = Identifier.of(Botumi.MOD_ID, "dome_block_entity");
    private final java.util.Map<BlockPos, Integer> barrierTtl = new java.util.HashMap<>();
    int tickCounter = 0;
    private String protectedStructureId = "";
    private boolean enabled = false;
    private boolean debugMode = false;
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

    private int destroyCountdownTicks = -1;
    private boolean isDestroying = false;
    private final List<BlockPos> destructionQueue = new ArrayList<>();
    private int destructionSoundTick = 0;

    private boolean isRestoring = false;
    private final List<TerrainRegenerator.BlockRestoreEntry> restorationQueue = new ArrayList<>();
    private BlockState domeNaturalState = Blocks.AIR.getDefaultState();

    private boolean bossSpawned = false;
    private boolean bossDefeated = false;

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

        if (world instanceof ServerWorld sw) {
            if (domeBlockEntity.enabled) {
                domeBlockEntity.enforceBarrier(sw);
                domeBlockEntity.decayTempColliders(sw);

                if (domeBlockEntity.tickCounter % 20 == 0) {
                    domeBlockEntity.maintainGuardians(sw);
                }
            }

            if (domeBlockEntity.destroyCountdownTicks >= 0) {
                domeBlockEntity.destroyCountdownTicks--;

                if (domeBlockEntity.tickCounter == 0) {
                    domeBlockEntity.syncOpenScreens(sw);
                }

                if (domeBlockEntity.destroyCountdownTicks == 0) {
                    domeBlockEntity.performStructureDestruction(sw);
                    domeBlockEntity.syncOpenScreens(sw);
                }
            }

            if (domeBlockEntity.isDestroying) {
                domeBlockEntity.tickDestructionQueue(sw);
            } else if (domeBlockEntity.isRestoring) {
                domeBlockEntity.tickRestorationQueue(sw);
            }
        }
    }

    private void syncOpenScreens(ServerWorld sw) {
        for (PlayerEntity player : sw.getPlayers()) {
            if (player.currentScreenHandler instanceof DomeScreenHandler handler && handler.isForBlockEntity(this)) {
                handler.syncFromBlockEntity();
            }
        }
    }

    public void scheduleStructureDestruction() {
        scheduleStructureDestruction(25 * 20);
    }

    public void scheduleStructureDestruction(int countdownTicks) {
        if (this.world instanceof ServerWorld) {
            if (this.destroyCountdownTicks < 0 && this.protectedStructureId != null && !this.protectedStructureId.isEmpty()) {
                this.enabled = false;
                this.destroyCountdownTicks = Math.max(1, countdownTicks);
                markChanged();
            }
        }
    }

    public boolean isDestroyScheduled() {
        return this.destroyCountdownTicks >= 0 || this.isDestroying || this.isRestoring;
    }

    public int getDestroyCountdownSeconds() {
        return this.destroyCountdownTicks < 0 ? 0 : (this.destroyCountdownTicks + 19) / 20;
    }

    private void performStructureDestruction(ServerWorld sw) {
        if (this.protectedStructureId == null || this.protectedStructureId.isEmpty()) {
            this.destroyCountdownTicks = -1;
            this.enabled = false;
            markChanged();
            return;
        }

        Box b = StructureGuard.getStructureData(sw, this.pos, this.protectedStructureId);
        final double R = Math.max(0.5, getRenderRadius());
        final Vec3d c = getDomeBaseCenter();

        if (b == null) {
            b = new Box(
                    Math.floor(c.x - R), Math.floor(c.y - 1), Math.floor(c.z - R),
                    Math.ceil(c.x + R), Math.ceil(c.y + R), Math.ceil(c.z + R)
            );
        }

        int minX = (int) Math.floor(b.minX);
        int minY = (int) Math.floor(b.minY);
        int minZ = (int) Math.floor(b.minZ);
        int maxX = (int) Math.ceil(b.maxX);
        int maxY = (int) Math.ceil(b.maxY);
        int maxZ = (int) Math.ceil(b.maxZ);

        destructionQueue.clear();
        BlockPos.Mutable m = new BlockPos.Mutable();

        List<BlockPos> glassAndLights = new ArrayList<>();
        List<BlockPos> roofsAndUpper = new ArrayList<>();
        List<BlockPos> wallsAndFloors = new ArrayList<>();

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    m.set(x, y, z);
                    if (!sw.isInBuildLimit(m)) continue;
                    if (m.equals(this.pos)) continue; // KEEP DOME BLOCK ENTITY ALIVE UNTIL THE VERY END!

                    double dx = (x + 0.5) - c.x;
                    double dy = (y + 0.5) - c.y;
                    double dz = (z + 0.5) - c.z;

                    if (dy < -0.5 || dy > R + 2.0) continue;
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (dist > R + 2.0) continue;

                    BlockState st = sw.getBlockState(m);
                    if (st.isAir() || st.isOf(Blocks.BARRIER)) continue;

                    if (st.isOf(Blocks.LIGHT) || st.getBlock() instanceof StainedGlassBlock || st.isOf(Blocks.GLASS) || st.isOf(Blocks.TINTED_GLASS) || st.isOf(Blocks.LANTERN) || st.isOf(Blocks.SOUL_LANTERN)) {
                        glassAndLights.add(m.toImmutable());
                    } else if (y >= this.pos.getY()) {
                        roofsAndUpper.add(m.toImmutable());
                    } else {
                        wallsAndFloors.add(m.toImmutable());
                    }
                }
            }
        }

        Collections.shuffle(glassAndLights);
        roofsAndUpper.sort((p1, p2) -> Integer.compare(p2.getY(), p1.getY()));
        wallsAndFloors.sort((p1, p2) -> Integer.compare(p2.getY(), p1.getY()));

        // Stage 1: Shatter all lights & glass
        destructionQueue.addAll(glassAndLights);
        // Stage 2: Upper floors crumble
        destructionQueue.addAll(roofsAndUpper);
        // Stage 3: Lower walls & foundation collapse
        destructionQueue.addAll(wallsAndFloors);

        // Initial purge of defending hostiles and stray items/falling blocks
        TerrainRegenerator.cleanEntities(sw, c, R);

        // Blinding core pulse at start
        sw.spawnParticles(ParticleTypes.FLASH, this.pos.getX() + 0.5, this.pos.getY() + 1.5, this.pos.getZ() + 0.5, 3, 0, 0, 0, 0.0);
        sw.spawnParticles(ParticleTypes.SONIC_BOOM, this.pos.getX() + 0.5, this.pos.getY() + 1.5, this.pos.getZ() + 0.5, 1, 0, 0, 0, 0.0);
        sw.playSound(null, this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.BLOCKS, 2.0f, 0.6f);

        this.destroyCountdownTicks = -1;
        this.enabled = false;
        this.isDestroying = true;
        this.destructionSoundTick = 0;
        markChanged();
    }

    private void tickDestructionQueue(ServerWorld sw) {
        if (destructionQueue.isEmpty()) {
            final double R = Math.max(10.0, getRenderRadius());
            Vec3d center = getDomeBaseCenter();
            Box structureBounds = StructureGuard.getStructureData(sw, this.pos, protectedStructureId);

            // Transition from destruction to progressive bottom-up terrain restoration
            TerrainRegenerator.RestorationPlan plan = TerrainRegenerator.createRestorationPlan(sw, center, R, structureBounds, this.pos);
            this.restorationQueue.clear();
            this.restorationQueue.addAll(plan.entries());
            this.domeNaturalState = plan.domeNaturalState() != null ? plan.domeNaturalState() : Blocks.AIR.getDefaultState();

            this.isDestroying = false;
            this.isRestoring = true;
            this.destructionSoundTick = 0;
            this.protectedStructureId = "";
            this.recalcEffectiveRadius();
            markChanged();

            // Core transition pulse: from collapse to restoration
            sw.spawnParticles(ParticleTypes.FLASH, this.pos.getX() + 0.5, this.pos.getY() + 1.5, this.pos.getZ() + 0.5, 2, 0, 0, 0, 0.0);
            sw.playSound(null, this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.5f, 1.2f);
            return;
        }

        // Dynamically scale batch size so collapse progresses smoothly without lag
        int count = Math.max(350, Math.min(1200, destructionQueue.size() / 30));
        count = Math.min(count, destructionQueue.size());

        for (int i = 0; i < count; i++) {
            BlockPos p = destructionQueue.remove(0);
            if (!sw.isInBuildLimit(p) || p.equals(this.pos)) continue;
            BlockState st = sw.getBlockState(p);
            if (!st.isAir() && !st.isOf(Blocks.BARRIER)) {
                if (sw.random.nextFloat() < 0.06f && !st.isOf(Blocks.LIGHT)) {
                    sw.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, st), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 12, 0.35, 0.35, 0.35, 0.15);
                }
                sw.setBlockState(p, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE | Block.SKIP_DROPS);
            }
        }

        // Periodically purge any dropped items from broken attached blocks (torches, double flowers, signs)
        final double R = Math.max(10.0, getRenderRadius());
        Box area = new Box(this.pos).expand(R + 8.0);
        for (ItemEntity item : sw.getEntitiesByClass(ItemEntity.class, area, e -> true)) {
            item.discard();
        }

        destructionSoundTick++;
        if (destructionSoundTick % 2 == 0 && !destructionQueue.isEmpty()) {
            BlockPos p = destructionQueue.get(sw.random.nextInt(Math.min(count * 2, destructionQueue.size())));
            sw.spawnParticles(ParticleTypes.EXPLOSION, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 2, 0.4, 0.4, 0.4, 0.05);
            sw.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 2, 0.3, 0.5, 0.3, 0.02);
            if (destructionSoundTick % 4 == 0) {
                sw.playSound(null, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 0.8f, 0.8f + sw.random.nextFloat() * 0.4f);
            }
        }
    }

    private void tickRestorationQueue(ServerWorld sw) {
        if (restorationQueue.isEmpty()) {
            final double R = Math.max(10.0, getRenderRadius());
            Vec3d center = getDomeBaseCenter();

            // Final entity cleanup pass
            TerrainRegenerator.cleanEntities(sw, center, R);

            this.isRestoring = false;
            markChanged();

            // Final Cataclysmic Core Detonation!
            sw.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5, 4, 1.0, 1.0, 1.0, 0.0);
            sw.spawnParticles(ParticleTypes.FLASH, this.pos.getX() + 0.5, this.pos.getY() + 1.5, this.pos.getZ() + 0.5, 2, 0, 0, 0, 0.0);
            sw.playSound(null, this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 2.0f, 0.5f);
            sw.playSound(null, this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.BLOCKS, 2.0f, 1.2f);

            BlockState naturalState = this.domeNaturalState != null ? this.domeNaturalState : Blocks.AIR.getDefaultState();
            sw.setBlockState(this.pos, naturalState, Block.NOTIFY_LISTENERS | Block.FORCE_STATE | Block.SKIP_DROPS);
            return;
        }

        // Dynamically scale batch size so restoration progresses smoothly from bottom to top (~8-10 seconds for large craters)
        int count = Math.max(600, Math.min(2500, restorationQueue.size() / 20));
        count = Math.min(count, restorationQueue.size());

        for (int i = 0; i < count; i++) {
            TerrainRegenerator.BlockRestoreEntry entry = restorationQueue.remove(0);
            BlockPos p = entry.pos();
            if (!sw.isInBuildLimit(p) || p.equals(this.pos)) continue;

            BlockState current = sw.getBlockState(p);
            BlockState target = entry.state();
            if (!current.equals(target)) {
                if (current.hasBlockEntity()) {
                    sw.removeBlockEntity(p);
                }
                sw.setBlockState(p, target, Block.NOTIFY_LISTENERS | Block.FORCE_STATE | Block.SKIP_DROPS);

                if (sw.random.nextFloat() < 0.04f) {
                    if (target.isOf(Blocks.GRASS_BLOCK) || target.isIn(net.minecraft.registry.tag.BlockTags.LEAVES) || target.isIn(net.minecraft.registry.tag.BlockTags.FLOWERS)) {
                        sw.spawnParticles(ParticleTypes.HAPPY_VILLAGER, p.getX() + 0.5, p.getY() + 1.1, p.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0.02);
                    } else if (!target.isAir()) {
                        sw.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, target), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.1);
                    }
                }
            }
        }

        // Periodically purge any dropped items or falling blocks in restoration zone
        final double R = Math.max(10.0, getRenderRadius());
        Box area = new Box(this.pos).expand(R + 8.0);
        for (ItemEntity item : sw.getEntitiesByClass(ItemEntity.class, area, e -> true)) {
            item.discard();
        }
        for (FallingBlockEntity falling : sw.getEntitiesByClass(FallingBlockEntity.class, area, e -> true)) {
            falling.discard();
        }

        destructionSoundTick++;
        if (destructionSoundTick % 3 == 0) {
            sw.playSound(null, this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5, SoundEvents.BLOCK_GRASS_PLACE, SoundCategory.BLOCKS, 0.9f, 0.9f + sw.random.nextFloat() * 0.3f);
            if (destructionSoundTick % 6 == 0) {
                sw.playSound(null, this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 0.8f, 1.0f + sw.random.nextFloat() * 0.4f);
            }
        }
    }

    private void maintainGuardians(ServerWorld sw) {
        if (!this.enabled || protectedStructureId == null || protectedStructureId.isEmpty()) return;
        final double R = getRenderRadius();
        final Vec3d c = getDomeBaseCenter();
        final Box area = new Box(c.x - R - 12, c.y - 20, c.z - R - 12, c.x + R + 12, c.y + R + 12, c.z + R + 12);

        // Check Dome Guardians
        List<DomeGuardianEntity> guardians = sw.getEntitiesByClass(DomeGuardianEntity.class, area, LivingEntity::isAlive);
        if (guardians.size() < 3) {
            double[] angles = new double[]{0.0, Math.PI / 2.0, Math.PI, -Math.PI / 2.0};
            double ang = angles[sw.random.nextInt(angles.length)];
            double spawnDist = Math.max(8.0, R - 2.0);
            double gx = c.x + Math.cos(ang) * spawnDist;
            double gz = c.z + Math.sin(ang) * spawnDist;

            BlockPos spawnPos = findWalkableSurface(sw, gx, gz, (int) c.y + 1);
            if (spawnPos != null && sw.isInBuildLimit(spawnPos)) {
                DomeGuardianEntity guardian = BotumiRegistry.DOME_GUARDIAN_ENTITY_TYPE.create(sw);
                if (guardian != null) {
                    guardian.refreshPositionAndAngles(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, (float) Math.toDegrees(ang) + 180f, 0f);
                    guardian.initialize(sw, sw.getLocalDifficulty(spawnPos), net.minecraft.entity.SpawnReason.STRUCTURE, null);
                    sw.spawnEntity(guardian);
                }
            }
        }

        // Check Core Sentinel Boss (spawns only once, never respawns once defeated)
        if (!this.bossSpawned && !this.bossDefeated) {
            List<CoreSentinelBossEntity> bosses = sw.getEntitiesByClass(CoreSentinelBossEntity.class, area, LivingEntity::isAlive);
            if (bosses.isEmpty()) {
                BlockPos bPos = findWalkableSurfaceNear(sw, this.pos, 4);
                if (bPos != null && sw.isInBuildLimit(bPos)) {
                    CoreSentinelBossEntity boss = BotumiRegistry.CORE_SENTINEL_ENTITY_TYPE.create(sw);
                    if (boss != null) {
                        boss.setDomeOrigin(this.pos);
                        boss.refreshPositionAndAngles(bPos.getX() + 0.5, bPos.getY(), bPos.getZ() + 0.5, 0f, 0f);
                        boss.initialize(sw, sw.getLocalDifficulty(bPos), net.minecraft.entity.SpawnReason.STRUCTURE, null);
                        sw.spawnEntity(boss);
                        this.bossSpawned = true;
                        markChanged();
                    }
                }
            } else {
                this.bossSpawned = true;
            }
        }
    }

    private BlockPos findWalkableSurface(ServerWorld sw, double x, double z, int preferredY) {
        int bx = MathHelper.floor(x);
        int bz = MathHelper.floor(z);
        int topY = sw.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, bx, bz);

        int scanStart = Math.min(topY + 1, preferredY + 25);
        int scanEnd = Math.max(sw.getBottomY() + 1, preferredY - 20);
        BlockPos.Mutable mut = new BlockPos.Mutable(bx, scanStart, bz);

        for (int y = scanStart; y >= scanEnd; y--) {
            mut.setY(y);
            BlockState below = sw.getBlockState(mut.down());
            BlockState at = sw.getBlockState(mut);
            BlockState above = sw.getBlockState(mut.up());
            if (below.isSolidBlock(sw, mut.down()) && !below.isOf(Blocks.BARRIER)
                    && (at.isAir() || !at.blocksMovement())
                    && (above.isAir() || !above.blocksMovement())) {
                return mut.toImmutable();
            }
        }
        return new BlockPos(bx, preferredY + 1, bz);
    }

    private BlockPos findWalkableSurfaceNear(ServerWorld sw, BlockPos center, int radius) {
        BlockPos.Mutable mut = new BlockPos.Mutable();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (Math.abs(dx) + Math.abs(dz) < 2) continue;
                for (int dy = 2; dy >= -2; dy--) {
                    mut.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState below = sw.getBlockState(mut.down());
                    BlockState at = sw.getBlockState(mut);
                    BlockState above = sw.getBlockState(mut.up());
                    if (below.isSolidBlock(sw, mut.down()) && !below.isOf(Blocks.BARRIER)
                            && (at.isAir() || !at.blocksMovement())
                            && (above.isAir() || !above.blocksMovement())) {
                        return mut.toImmutable();
                    }
                }
            }
        }
        return center.up();
    }

    private boolean isAtGate(double py, double angle, double radius) {
        return DomeBlockEntityRenderer.isAtGate(py, angle, radius);
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
        if (st.isAir()) {
            sw.setBlockState(pos, Blocks.BARRIER.getDefaultState(), 3);
            barrierTtl.put(pos, 4);
        } else if (st.isOf(Blocks.BARRIER)) {
            barrierTtl.put(pos, 4);
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
            if (cy < 0.0 || cy > R) continue;
            double d = Math.sqrt(cx * cx + cy * cy + cz * cz);
            if (d < 1.0e-6) continue;
            if (Math.abs(d - R) > 1.25) continue;
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
        final double maxDistanceSq = (R + 24.0) * (R + 24.0);

        boolean playerNearby = false;
        for (PlayerEntity p : sw.getPlayers()) {
            if (p.squaredDistanceTo(c.x, c.y, c.z) <= maxDistanceSq) {
                playerNearby = true;
                break;
            }
        }
        if (!playerNearby) return;

        final Box query = new Box(
                c.x - R - 2.0, c.y - 2.0, c.z - R - 2.0,
                c.x + R + 2.0, c.y + R + 2.0, c.z + R + 2.0
        );

        for (var e : sw.getOtherEntities(null, query)) {
            if (e instanceof PlayerEntity pe && pe.isSpectator()) continue;

            final Vec3d centerNow = e.getBoundingBox().getCenter();
            final Vec3d rNow = centerNow.subtract(c);
            double dNow = rNow.length();
            if (dNow < 1.0e-6) continue;

            double py = centerNow.y - c.y;
            if (py < 0.0 || py > R + 1.0) continue;

            double ang = Math.atan2(rNow.z, rNow.x);
            if (isAtGate(py, ang, R)) continue;

            double distDiff = Math.abs(dNow - R);
            if (distDiff <= 1.2) {
                Vec3d norm = rNow.normalize();
                double pushStrength = 0.35;
                if (dNow >= R) {
                    e.addVelocity(norm.x * pushStrength, 0.08, norm.z * pushStrength);
                } else {
                    e.addVelocity(-norm.x * pushStrength, 0.08, -norm.z * pushStrength);
                }
                e.velocityModified = true;

                sw.spawnParticles(ParticleTypes.ELECTRIC_SPARK, centerNow.x, centerNow.y, centerNow.z, 2, 0.2, 0.2, 0.2, 0.02);

                if (e instanceof PlayerEntity && distDiff <= 0.8) {
                    placeBarrierPatch(sw, c, R, rNow);
                }
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

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean value) {
        this.debugMode = value;
        markChanged();
    }

    public boolean isDestroying() {
        return this.isDestroying;
    }

    public int getDestructionQueueSize() {
        return this.destructionQueue.size();
    }

    public boolean isRestoring() {
        return this.isRestoring;
    }

    public int getRestorationQueueSize() {
        return this.restorationQueue.size();
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
        this.debugMode = nbt.getBoolean("DebugMode");

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

        this.destroyCountdownTicks = nbt.contains("DestroyCountdown") ? nbt.getInt("DestroyCountdown") : -1;
        this.bossSpawned = nbt.getBoolean("BossSpawned");
        this.bossDefeated = nbt.getBoolean("BossDefeated");
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        if (this.protectedStructureId != null && !this.protectedStructureId.isEmpty()) {
            nbt.putString("ProtectedStructureId", this.protectedStructureId);
        }
        nbt.putBoolean("ProtectedEnabled", this.enabled);
        nbt.putBoolean("DebugMode", this.debugMode);
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
        nbt.putInt("DestroyCountdown", this.destroyCountdownTicks);
        nbt.putBoolean("BossSpawned", this.bossSpawned);
        nbt.putBoolean("BossDefeated", this.bossDefeated);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("ProtectedStructureId", this.protectedStructureId);
        nbt.putBoolean("ProtectedEnabled", this.enabled);
        nbt.putBoolean("DebugMode", this.debugMode);
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
        nbt.putInt("DestroyCountdown", this.destroyCountdownTicks);
        nbt.putBoolean("BossSpawned", this.bossSpawned);
        nbt.putBoolean("BossDefeated", this.bossDefeated);
        return nbt;
    }

    public boolean isBossDefeated() {
        return bossDefeated;
    }

    public void setBossDefeated(boolean defeated) {
        this.bossDefeated = defeated;
        markChanged();
    }

    public boolean isBossSpawned() {
        return bossSpawned;
    }

    public void setBossSpawned(boolean spawned) {
        this.bossSpawned = spawned;
        markChanged();
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
                this.centerX = b.getCenter().x;
                if ("botumi:town".equals(protectedStructureId)) {
                    this.centerY = Math.max(b.minY, this.pos.getY() - 42);
                } else {
                    this.centerY = b.minY;
                }
                this.centerZ = b.getCenter().z;

                if (useStructureRadius) {
                    double maxSq = getMaxSq(b);
                    this.computedRadius = Math.max(1.0, Math.sqrt(maxSq));
                } else {
                    this.computedRadius = Math.max(1.0, this.radius);
                }
            } else if ("botumi:town".equals(protectedStructureId)) {
                this.centerY = this.pos.getY() - 42;
                this.computedRadius = Math.max(1.0, this.radius);
            }
        }
    }

    private double getMaxSq(Box b) {
        Vec3d c = new Vec3d(this.centerX, this.centerY, this.centerZ);
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
