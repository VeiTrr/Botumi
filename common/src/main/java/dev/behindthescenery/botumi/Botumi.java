package dev.behindthescenery.botumi;


import dev.behindthescenery.botumi.config.BConfig;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;

import java.util.List;

public final class Botumi {
    public static final String MOD_ID = "botumi";

    public static void init() {
        BConfig.load();
    }



    public static Box getStructureData(ServerWorld world, BlockPos structurePos) {

        List<StructureStart> structureStarts = world.getStructureAccessor().getStructureStarts(ChunkSectionPos.from(structurePos), world.getRegistryManager().get(RegistryKeys.STRUCTURE).get(Identifier.of(BConfig.INSTANCE.protectedStructureId)));
        Box overallBox = null;
        if (structureStarts != null && !structureStarts.isEmpty()) {
            for (StructureStart structureStart : structureStarts) {
                if (structureStart != null && structureStart.hasChildren()) {
                    BlockBox box = structureStart.getBoundingBox();
                    if (overallBox == null) {
                        overallBox = new Box(box.getMinX(), box.getMinY(), box.getMinZ(), box.getMaxX(), box.getMaxY(), box.getMaxZ());
                    } else {
                        overallBox = overallBox.union(new Box(box.getMinX(), box.getMinY(), box.getMinZ(), box.getMaxX(), box.getMaxY(), box.getMaxZ()));
                    }
                }
            }
        }
        return overallBox;

    }

    //    north = -z
    //    south = +z
    //    west = -x
    //    east = +x


//   6 double array for structure size (north, south, west, east, up, down)

    public static double[] getStructureSize(ServerWorld world, BlockPos structurePos) {
        Box box = getStructureData(world, structurePos);
        if (box == null) {
            System.out.println("Structure box is null for structure at " + structurePos);
            return new double[]{0, 0, 0, 0, 0, 0};
        }
        Vec3d north = new Vec3d(structurePos.getX(), structurePos.getY(), box.minZ);
        Vec3d south = new Vec3d(structurePos.getX(), structurePos.getY(), box.maxZ);
        Vec3d west = new Vec3d(box.minX, structurePos.getY(), structurePos.getZ());
        Vec3d east = new Vec3d(box.maxX, structurePos.getY(), structurePos.getZ());
        Vec3d up = new Vec3d(structurePos.getX(), box.maxY, structurePos.getZ());
        Vec3d down = new Vec3d(structurePos.getX(), box.minY, structurePos.getZ());
        return new double[]{
                Math.abs(structurePos.getZ() - north.getZ()),
                Math.abs(structurePos.getZ() - south.getZ()),
                Math.abs(structurePos.getX() - west.getX()),
                Math.abs(structurePos.getX() - east.getX()),
                Math.abs(structurePos.getY() - up.getY()),
                Math.abs(structurePos.getY() - down.getY())
        };
    }

//   6 Vec3d array for structure end coords (north, south, west, east, up, down).

    public static Vec3d[] getStructureEndCoordinates(ServerWorld world, BlockPos structurePos) {
        Box box = getStructureData(world, structurePos);
        if (box == null) {
            System.out.println("Structure box is null for structure at " + structurePos);
            return new Vec3d[]{Vec3d.of(structurePos), Vec3d.of(structurePos), Vec3d.of(structurePos), Vec3d.of(structurePos), Vec3d.of(structurePos), Vec3d.of(structurePos)};
        }

        Vec3d center = box.getCenter();
        Vec3d north = new Vec3d(center.getX(), center.getY(), box.minZ);
        Vec3d south = new Vec3d(center.getX(), center.getY(), box.maxZ);
        Vec3d west = new Vec3d(box.minX, center.getY(), center.getZ());
        Vec3d east = new Vec3d(box.maxX, center.getY(), center.getZ());
        Vec3d up = new Vec3d(center.getX(), box.maxY, center.getZ());
        Vec3d down = new Vec3d(center.getX(), box.minY, center.getZ());
        return new Vec3d[]{north, south, west, east, up, down};
    }
}
