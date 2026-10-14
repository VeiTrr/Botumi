package dev.behindthescenery.botumi.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

public class DomeGuardianEntity extends HostileEntity {
    private int surgeCooldown = 60;

    public DomeGuardianEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 15;
    }

    public static DefaultAttributeContainer.Builder createGuardianAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 45.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.26)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 7.0)
                .add(EntityAttributes.GENERIC_ARMOR, 6.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.4);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.15, false));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, IronGolemEntity.class, true));
    }

    @Override
    public void tickMovement() {
        super.tickMovement();

        if (this.getWorld().isClient) {
            if (this.random.nextInt(4) == 0) {
                this.getWorld().addParticle(
                        ParticleTypes.ELECTRIC_SPARK,
                        this.getX() + (this.random.nextDouble() - 0.5) * 0.6,
                        this.getY() + this.random.nextDouble() * 1.8,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 0.6,
                        0.0, 0.02, 0.0
                );
            }
            return;
        }

        // Energy surge attack logic
        if (this.getTarget() != null && this.isAlive()) {
            surgeCooldown--;
            if (surgeCooldown <= 0) {
                performElectricSurge();
                surgeCooldown = 80 + this.random.nextInt(40);
            }
        }
    }

    private void performElectricSurge() {
        if (!(this.getWorld() instanceof ServerWorld sw)) return;

        sw.spawnParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 1.0, this.getZ(), 20, 0.5, 0.5, 0.5, 0.15);
        this.playSound(SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), 1.2F, 1.5F);

        double range = 4.5;
        for (LivingEntity nearby : sw.getEntitiesByClass(LivingEntity.class, this.getBoundingBox().expand(range), e -> e != this && !(e instanceof DomeGuardianEntity))) {
            if (nearby instanceof PlayerEntity || nearby instanceof IronGolemEntity) {
                nearby.damage(this.getDamageSources().magic(), 4.0F);
                nearby.takeKnockback(0.4, this.getX() - nearby.getX(), this.getZ() - nearby.getZ());
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_IRON_GOLEM_STEP;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_IRON_GOLEM_DEATH;
    }
}
