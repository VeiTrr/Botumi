package dev.behindthescenery.botumi.mixin.Render;


import dev.behindthescenery.botumi.client.HuskTextureRules;
import net.minecraft.client.render.entity.HuskEntityRenderer;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HuskEntityRenderer.class)
public abstract class HuskEntityRendererMixin {
    @Shadow
    @Final
    private static Identifier TEXTURE;

    @Inject(method = "getTexture(Lnet/minecraft/entity/mob/ZombieEntity;)Lnet/minecraft/util/Identifier;", at = @At("HEAD"), cancellable = true)
    private void botumi$overrideTexture(ZombieEntity entity, CallbackInfoReturnable<Identifier> cir) {
        Identifier id = HuskTextureRules.selectTexture(entity, TEXTURE);
        if (id != null) {
            cir.setReturnValue(id);
        }
    }
}
