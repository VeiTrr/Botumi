package dev.behindthescenery.botumi.mixin.BlockProtect;

import dev.behindthescenery.botumi.util.StructureGuard;
import net.minecraft.block.BlockState;
import net.minecraft.block.PistonBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonBlock.class)
public abstract class PistonBlockMixin {
    @Inject(
            method = "isMovable",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void botumi$denyPistonMove(BlockState state, World world, BlockPos pos, Direction motionDir, boolean allowDestroy, Direction pistonDir, CallbackInfoReturnable<Boolean> cir) {
        if (!(world instanceof ServerWorld serverWorld)) return;

        if (StructureGuard.isInProtectedStructure(serverWorld, pos)) {
            cir.setReturnValue(false);
            return;
        }

        if (!state.isAir()) {
            BlockPos dest = pos.offset(motionDir);
            if (StructureGuard.isInProtectedStructure(serverWorld, dest)) {
                cir.setReturnValue(false);
            }
        }
    }
}
