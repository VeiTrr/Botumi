package dev.behindthescenery.botumi.entity;

import dev.behindthescenery.botumi.blocks.entity.DomeBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CoreSentinelBossEntity extends HostileEntity {
    private final ServerBossBar bossBar;
    private int shockwaveCooldown = 100;
    private boolean phaseTwoTriggered = false;
    private BlockPos domeOrigin;

    public CoreSentinelBossEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 100;
        this.setPersistent();
        this.bossBar = (ServerBossBar) new ServerBossBar(
                Text.literal("Core Sentinel").formatted(Formatting.AQUA, Formatting.BOLD),
                BossBar.Color.BLUE,
                BossBar.Style.NOTCHED_10
        ).setDarkenSky(true);
    }

    public void setDomeOrigin(BlockPos pos) {
        this.domeOrigin = pos;
    }

    public BlockPos getDomeOrigin() {
        return this.domeOrigin;
    }

    public static DefaultAttributeContainer.Builder createBossAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 160.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 10.0)
                .add(EntityAttributes.GENERIC_ARMOR, 12.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.25, false));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 12.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, IronGolemEntity.class, true));
    }

    @Override
    public void tickMovement() {
        super.tickMovement();

        if (this.getWorld().isClient) {
            float particleCount = phaseTwoTriggered ? 3 : 1;
            for (int i = 0; i < particleCount; i++) {
                this.getWorld().addParticle(
                        phaseTwoTriggered ? ParticleTypes.PORTAL : ParticleTypes.ELECTRIC_SPARK,
                        this.getX() + (this.random.nextDouble() - 0.5) * 1.2,
                        this.getY() + this.random.nextDouble() * 2.4,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 1.2,
                        (this.random.nextDouble() - 0.5) * 0.2,
                        0.05,
                        (this.random.nextDouble() - 0.5) * 0.2
                );
            }
            return;
        }

        // Update Boss Bar
        this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());

        // Check Phase 2 (Under 50% HP)
        if (!phaseTwoTriggered && this.getHealth() <= this.getMaxHealth() * 0.5F) {
            triggerPhaseTwo();
        }

        // Combat shockwave ability
        if (this.getTarget() != null && this.isAlive()) {
            shockwaveCooldown--;
            if (shockwaveCooldown <= 0) {
                unleashCoreOverload();
                shockwaveCooldown = phaseTwoTriggered ? 70 : 120;
            }
        }
    }

    private void triggerPhaseTwo() {
        phaseTwoTriggered = true;
        this.bossBar.setName(Text.literal("Core Sentinel [OVERLOAD]").formatted(Formatting.RED, Formatting.BOLD));
        this.bossBar.setColor(BossBar.Color.RED);

        if (this.getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.2, this.getZ(), 3, 0, 0, 0, 0.0);
            this.playSound(SoundEvents.ENTITY_WITHER_SPAWN, 1.5F, 0.8F);
        }
    }

    private void unleashCoreOverload() {
        if (!(this.getWorld() instanceof ServerWorld sw)) return;

        sw.spawnParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 1.2, this.getZ(), 1, 0, 0, 0, 0.0);
        sw.spawnParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 1.0, this.getZ(), 40, 1.0, 0.5, 1.0, 0.2);
        this.playSound(SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 1.2F, 1.3F);

        double blastRadius = phaseTwoTriggered ? 8.0 : 5.5;
        for (LivingEntity nearby : sw.getEntitiesByClass(LivingEntity.class, this.getBoundingBox().expand(blastRadius), e -> e != this && !(e instanceof DomeGuardianEntity))) {
            if (nearby instanceof PlayerEntity || nearby instanceof IronGolemEntity) {
                nearby.damage(this.getDamageSources().magic(), phaseTwoTriggered ? 8.0F : 5.0F);
                double dx = nearby.getX() - this.getX();
                double dz = nearby.getZ() - this.getZ();
                nearby.takeKnockback(1.0, -dx, -dz);
            }
        }
    }

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        this.bossBar.addPlayer(player);
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        this.bossBar.removePlayer(player);
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);
        this.bossBar.clearPlayers();
        if (this.getWorld() instanceof ServerWorld sw && this.domeOrigin != null) {
            if (sw.getBlockEntity(this.domeOrigin) instanceof DomeBlockEntity dome) {
                dome.setBossDefeated(true);
            }
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        this.bossBar.clearPlayers();
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("PhaseTwoTriggered", this.phaseTwoTriggered);
        if (this.domeOrigin != null) {
            nbt.putInt("DomeOriginX", this.domeOrigin.getX());
            nbt.putInt("DomeOriginY", this.domeOrigin.getY());
            nbt.putInt("DomeOriginZ", this.domeOrigin.getZ());
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.phaseTwoTriggered = nbt.getBoolean("PhaseTwoTriggered");
        if (nbt.contains("DomeOriginX")) {
            this.domeOrigin = new BlockPos(nbt.getInt("DomeOriginX"), nbt.getInt("DomeOriginY"), nbt.getInt("DomeOriginZ"));
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BLOCK_BEACON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_WITHER_DEATH;
    }
}
