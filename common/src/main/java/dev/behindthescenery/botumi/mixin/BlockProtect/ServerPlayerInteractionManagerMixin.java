package dev.behindthescenery.botumi.mixin.BlockProtect;

import dev.behindthescenery.botumi.util.StructureGuard;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class ServerPlayerInteractionManagerMixin {

    @Shadow
    protected ServerWorld world;

    @Inject(method = "tryBreakBlock", at = @At("HEAD"), cancellable = true)
    private void botumi$denyBreakInProtected(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (StructureGuard.isInProtectedStructure(world, pos)) {
            cir.setReturnValue(false);
        }
    }
}
