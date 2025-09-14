package dev.behindthescenery.botumi;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class DomeParticles {
    private DomeParticles() {}

    public static int spawnDome(ServerWorld world, BlockPos center, double radius, double step) {
        if (radius < 1) radius = 1;
        if (step < 2) step = 2;
        int count = 0;
        double cx = center.getX() + 0.5;
        double cy = center.getY() + 0.5;
        double cz = center.getZ() + 0.5;
        for (double theta = 0; theta <= 180; theta += step) {
            for (double phi = 0; phi < 360; phi += step) {
                double radTheta = Math.toRadians(theta);
                double radPhi = Math.toRadians(phi);
                double x = cx + radius * Math.sin(radTheta) * Math.cos(radPhi);
                double y = cy + radius * Math.cos(radTheta);
                double z = cz + radius * Math.sin(radTheta) * Math.sin(radPhi);
                world.spawnParticles(ParticleTypes.END_ROD, x, y, z, 1, 0,0,0,0);
                count++;
            }
        }
        return count;
    }
}

