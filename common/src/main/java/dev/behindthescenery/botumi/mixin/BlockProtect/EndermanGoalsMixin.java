package dev.behindthescenery.botumi.mixin.BlockProtect;


import dev.behindthescenery.botumi.util.StructureGuard;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class EndermanGoalsMixin {
    @Mixin(targets = "net.minecraft.entity.mob.EndermanEntity$PickUpBlockGoal")
    abstract static class EndermanPickUpGoalMixin {

        @Redirect(
                method = "tick",
                at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z")
        )
        private boolean botumi$denyEndermanRemove(World world, BlockPos pos, boolean move) {
            if (world instanceof ServerWorld serverWorld &&
                    StructureGuard.isInProtectedStructure(serverWorld, pos)) {
                return false;
            }
            return world.removeBlock(pos, move);
        }
    }

    @Mixin(targets = "net.minecraft.entity.mob.EndermanEntity$PlaceBlockGoal")
    abstract static class EndermanPlaceGoalMixin {

        @Inject(method = "canPlaceOn",
                at = @At("HEAD"),
                cancellable = true)
        private void botumi$denyEndermanPlace(World world, BlockPos posAbove, BlockState carriedState, BlockState stateAbove, BlockState state, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
            if (world instanceof ServerWorld serverWorld &&
                    StructureGuard.isInProtectedStructure(serverWorld, pos)) {
                cir.setReturnValue(true);
            }
        }
    }
}
