package dev.behindthescenery.botumi.util;

import dev.behindthescenery.botumi.blocks.entity.DomeBlockEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.structure.Structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class StructureGuard {
    private StructureGuard() {}

    private record StructureInstance(String id, Box box) {}

    public static boolean isInProtectedStructure(ServerWorld world, Vec3d pos) {
        BlockPos checkPos = BlockPos.ofFloored(pos);
        List<StructureInstance> containers = getContainingStructureInstances(world, checkPos);
        if (containers.isEmpty()) return false;

        for (StructureInstance si : containers) {
            if (si.box().contains(pos)) {
                if (hasEnabledDomeForStructureInLoadedChunks(world, si.box(), si.id())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isInProtectedStructure(ServerWorld world, BlockPos pos) {
        return isInProtectedStructure(world, Vec3d.ofCenter(pos));
    }

    private static boolean hasEnabledDomeForStructureInLoadedChunks(ServerWorld world, Box box, String structureId) {
        if (structureId == null || structureId.isEmpty()) return false;

        final int minX = MathHelper.floor(box.minX);
        final int maxX = MathHelper.floor(box.maxX);
        final int minZ = MathHelper.floor(box.minZ);
        final int maxZ = MathHelper.floor(box.maxZ);
        final int minY = MathHelper.clamp(MathHelper.floor(box.minY), world.getBottomY(), world.getTopY());
        final int maxY = MathHelper.clamp(MathHelper.floor(box.maxY), world.getBottomY(), world.getTopY());

        final int minCx = minX >> 4;
        final int maxCx = maxX >> 4;
        final int minCz = minZ >> 4;
        final int maxCz = maxZ >> 4;

        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                WorldChunk chunk = world.getChunkManager().getWorldChunk(cx, cz, false);
                if (chunk == null) continue;
                for (var be : chunk.getBlockEntities().values()) {
                    if (be instanceof DomeBlockEntity dome && dome.isEnabled()) {
                        String id = dome.getProtectedStructureId();
                        if (!structureId.equals(id)) continue;

                        BlockPos p = be.getPos();
                        if (p.getX() >= minX && p.getX() <= maxX &&
                                p.getZ() >= minZ && p.getZ() <= maxZ &&
                                p.getY() >= minY && p.getY() <= maxY) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static List<StructureInstance> getContainingStructureInstances(ServerWorld world, BlockPos pos) {
        var registry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
        List<StructureInstance> result = new ArrayList<>();
        Vec3d center = Vec3d.ofCenter(pos);
        for (Map.Entry<RegistryKey<Structure>, Structure> e : registry.getEntrySet()) {
            Identifier structId = e.getKey().getValue();
            String idStr = structId.toString();
            Box b = getStructureData(world, pos, idStr);
            if (b != null && b.contains(center)) {
                result.add(new StructureInstance(idStr, b));
            }
        }
        return result;
    }

    public static Box getStructureData(ServerWorld world, BlockPos structurePos, String structureId) {
        if (structureId == null || structureId.isEmpty()) return null;

        var reg = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
        var structureType = reg.get(Identifier.of(structureId));
        if (structureType == null) return null;

        List<StructureStart> starts = world.getStructureAccessor()
                .getStructureStarts(ChunkSectionPos.from(structurePos), structureType);

        if (starts == null || starts.isEmpty()) return null;

        Box overall = null;
        for (StructureStart start : starts) {
            if (start == null || !start.hasChildren()) continue;
            BlockBox bb = start.getBoundingBox();
            Box b = new Box(bb.getMinX(), bb.getMinY(), bb.getMinZ(),
                    bb.getMaxX() + 1, bb.getMaxY() + 1, bb.getMaxZ() + 1);
            overall = (overall == null) ? b : overall.union(b);
        }
        return overall;
    }

    public static List<String> getContainingStructureIds(ServerWorld world, BlockPos pos) {
        var registry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
        List<String> ids = new ArrayList<>();
        Vec3d center = Vec3d.ofCenter(pos);
        for (Map.Entry<RegistryKey<Structure>, Structure> e : registry.getEntrySet()) {
            Identifier structId = e.getKey().getValue();
            Box b = getStructureData(world, pos, structId.toString());
            if (b != null && b.contains(center)) {
                ids.add(structId.toString());
            }
        }
        return ids;
    }
}
