package dev.behindthescenery.botumi.mixin.BlockProtect;

import dev.behindthescenery.botumi.util.StructureGuard;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
    @Inject(
            method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void botumi$denyPlaceInProtected(ItemPlacementContext ctx, CallbackInfoReturnable<ActionResult> cir) {
        if (!(ctx.getWorld() instanceof ServerWorld serverWorld)) return;
        BlockPos placePos = ctx.getBlockPos();
        if (StructureGuard.isInProtectedStructure(serverWorld, placePos)) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}
