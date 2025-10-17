package dev.behindthescenery.botumi.mixin;

import dev.behindthescenery.botumi.config.BConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.HuskEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class HuskEntityRemoveMixin {
    @Inject(method = "remove", at = @org.spongepowered.asm.mixin.injection.At("HEAD"))
    private void botumi$remove(Entity.RemovalReason reason, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof HuskEntity && self.hasPassengers()) {
            if (self.getPassengerList().stream().anyMatch(e -> e instanceof ArmorStandEntity)) {
                for (Entity passenger : self.getPassengerList()) {
                    if (passenger instanceof ArmorStandEntity armorStand) {
                        String name = armorStand.getCustomName() != null ? armorStand.getCustomName().getString() : "";
                        String key = BConfig.INSTANCE.CustomHuskTag;
                        if (key != null && !key.isBlank() && name.contains(key)) {
                            armorStand.remove(reason);
                        }
                    }
                }
            }
        }
    }
}
