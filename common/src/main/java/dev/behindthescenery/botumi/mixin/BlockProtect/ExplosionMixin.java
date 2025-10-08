package dev.behindthescenery.botumi.mixin.BlockProtect;

import dev.behindthescenery.botumi.util.StructureGuard;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;

@Mixin(Explosion.class)
public abstract class ExplosionMixin {
    @Shadow
    @Final
    private World world;

    @Redirect(
            method = "collectBlocksAndDamageEntities",
            at = @At(value = "INVOKE", target = "Ljava/util/Set;add(Ljava/lang/Object;)Z")
    )
    private boolean botumi$filterAffectedBlocks(Set<BlockPos> set, Object posObj) {
        BlockPos pos = (BlockPos) posObj;
        if (this.world instanceof ServerWorld serverWorld &&
                StructureGuard.isInProtectedStructure(serverWorld, pos)) {
            return false;
        }
        return set.add(pos);
    }
}
