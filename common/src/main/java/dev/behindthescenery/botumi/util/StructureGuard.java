package dev.behindthescenery.botumi.util;

import dev.behindthescenery.botumi.Botumi;
import dev.behindthescenery.botumi.config.BConfig;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class StructureGuard {
    private StructureGuard() {}

    public static boolean isInProtectedStructure(ServerWorld world, Vec3d pos) {
        if (!BConfig.INSTANCE.protectInStructure) return false;

        Box box = Botumi.getStructureData(world, BlockPos.ofFloored(pos));
        return box != null && box.contains(pos);
    }

    public static boolean isInProtectedStructure(ServerWorld world, BlockPos pos) {
        return isInProtectedStructure(world, Vec3d.ofCenter(pos));
    }
}
