package dev.behindthescenery.botumi.util;

import dev.behindthescenery.botumi.entity.CoreSentinelBossEntity;
import dev.behindthescenery.botumi.entity.DomeGuardianEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.WorldProperties;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WrapperProtoChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.tick.QueryableTickScheduler;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class TerrainRegenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger("TerrainRegenerator");

    public record BlockRestoreEntry(BlockPos pos, BlockState state) {}
    public record RestorationPlan(List<BlockRestoreEntry> entries, @Nullable BlockState domeNaturalState) {}

    public static void regenerateArea(ServerWorld sw, Vec3d center, double radius, @Nullable Box structureBounds) {
        RestorationPlan plan = createRestorationPlan(sw, center, radius, structureBounds, BlockPos.ORIGIN);
        for (BlockRestoreEntry entry : plan.entries()) {
            sw.setBlockState(entry.pos(), entry.state(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE | Block.SKIP_DROPS);
        }
        cleanEntities(sw, center, radius);
    }

    public static RestorationPlan createRestorationPlan(ServerWorld sw, Vec3d center, double radius, @Nullable Box structureBounds, BlockPos domePos) {
        LOGGER.info("[Botumi] Building terrain restoration plan at center={}, radius={}", center, radius);

        // 1. Initial cleanup: discard dropped items, falling blocks, hostiles in the area
        cleanEntities(sw, center, radius);

        double effRadius = radius + 2.0;
        double minX = center.x - effRadius;
        double maxX = center.x + effRadius;
        double minZ = center.z - effRadius;
        double maxZ = center.z + effRadius;

        if (structureBounds != null) {
            // Clamp structure bounds within reasonable vicinity of center (effRadius + 16)
            minX = Math.max(center.x - effRadius - 16.0, Math.min(minX, structureBounds.minX));
            maxX = Math.min(center.x + effRadius + 16.0, Math.max(maxX, structureBounds.maxX));
            minZ = Math.max(center.z - effRadius - 16.0, Math.min(minZ, structureBounds.minZ));
            maxZ = Math.min(center.z + effRadius + 16.0, Math.max(maxZ, structureBounds.maxZ));
        }

        int minChunkX = MathHelper.floor(minX / 16.0);
        int maxChunkX = MathHelper.floor(maxX / 16.0);
        int minChunkZ = MathHelper.floor(minZ / 16.0);
        int maxChunkZ = MathHelper.floor(maxZ / 16.0);

        ChunkGenerator generator = sw.getChunkManager().getChunkGenerator();
        NoiseConfig noiseConfig = sw.getChunkManager().getNoiseConfig();

        // Structure accessor that returns empty structure starts so that pristine terrain noise is generated without beard/flattening and without worker-to-main thread deadlocks
        StructureAccessor filteredStructureAccessor = new StructureAccessor(sw, sw.getServer().getSaveProperties().getGeneratorOptions(), null) {
            @Override
            public List<StructureStart> getStructureStarts(ChunkPos pos, Predicate<Structure> predicate) {
                return Collections.emptyList();
            }

            @Override
            public List<StructureStart> getStructureStarts(ChunkSectionPos pos, Structure structure) {
                return Collections.emptyList();
            }

            @Override
            public boolean shouldGenerateStructures() {
                return false;
            }
        };

        double rSq = effRadius * effRadius;
        BlockPos.Mutable mut = new BlockPos.Mutable();

        List<WorldChunk> targetChunks = new java.util.ArrayList<>();
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (!sw.getChunkManager().isChunkLoaded(cx, cz)) {
                    continue;
                }

                double chunkMinX = cx * 16.0;
                double chunkMaxX = chunkMinX + 16.0;
                double chunkMinZ = cz * 16.0;
                double chunkMaxZ = chunkMinZ + 16.0;

                double closestX = MathHelper.clamp(center.x, chunkMinX, chunkMaxX);
                double closestZ = MathHelper.clamp(center.z, chunkMinZ, chunkMaxZ);
                double distSq = (closestX - center.x) * (closestX - center.x) + (closestZ - center.z) * (closestZ - center.z);
                boolean intersectsCircle = distSq <= rSq;
                boolean intersectsBounds = structureBounds != null &&
                        (chunkMaxX >= structureBounds.minX && chunkMinX <= structureBounds.maxX &&
                         chunkMaxZ >= structureBounds.minZ && chunkMinZ <= structureBounds.maxZ);

                if (!intersectsCircle && !intersectsBounds) {
                    continue;
                }

                WorldChunk wc = sw.getChunk(cx, cz);
                if (wc != null) {
                    targetChunks.add(wc);
                }
            }
        }
        LOGGER.info("[Botumi] Identified {} intersecting chunks to regenerate around center {}", targetChunks.size(), center);

        java.util.Map<ChunkPos, ProtoChunk> protoMap = new java.util.concurrent.ConcurrentHashMap<>();
        List<CompletableFuture<Void>> futures = new java.util.ArrayList<>();
        Registry<Biome> biomeRegistry = sw.getRegistryManager().get(RegistryKeys.BIOME);
        HeightContext heightContext = new HeightContext(generator, sw);
        BiomeAccess biomeAccess = sw.getBiomeAccess();

        for (WorldChunk wc : targetChunks) {
            ChunkPos cp = wc.getPos();
            ProtoChunk proto = new ProtoChunk(cp, UpgradeData.NO_UPGRADE_DATA, sw, biomeRegistry, null);
            protoMap.put(cp, proto);

            CompletableFuture<Void> cf = generator.populateBiomes(noiseConfig, Blender.getNoBlending(), filteredStructureAccessor, proto)
                    .thenCompose(p -> {
                        if (p instanceof ProtoChunk pc) pc.setStatus(net.minecraft.world.chunk.ChunkStatus.BIOMES);
                        return generator.populateNoise(Blender.getNoBlending(), noiseConfig, filteredStructureAccessor, p);
                    })
                    .thenAccept(p -> {
                        if (p instanceof ProtoChunk pc) pc.setStatus(net.minecraft.world.chunk.ChunkStatus.NOISE);
                        if (generator instanceof NoiseChunkGenerator noiseGen && p instanceof ProtoChunk pc) {
                            BiomeAccess protoBiomeAccess = biomeAccess.withSource((bx, by, bz) -> pc.getBiomeForNoiseGen(bx, by, bz));
                            noiseGen.buildSurface(pc, heightContext, noiseConfig, filteredStructureAccessor, protoBiomeAccess, biomeRegistry, Blender.getNoBlending());
                            pc.setStatus(net.minecraft.world.chunk.ChunkStatus.SURFACE);
                        }
                    });
            futures.add(cf);
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            LOGGER.error("[Botumi] Error during parallel proto chunk generation", e);
        }

        // Deregister Botumi structures from chunk metadata safely
        for (WorldChunk wc : targetChunks) {
            java.util.Map<Structure, StructureStart> starts = new java.util.HashMap<>(wc.getStructureStarts());
            starts.entrySet().removeIf(e -> {
                Identifier id = sw.getRegistryManager().get(RegistryKeys.STRUCTURE).getId(e.getKey());
                return id != null && id.getNamespace().equals("botumi");
            });
            wc.setStructureStarts(starts);

            java.util.Map<Structure, it.unimi.dsi.fastutil.longs.LongSet> refs = new java.util.HashMap<>(wc.getStructureReferences());
            refs.entrySet().removeIf(e -> {
                Identifier id = sw.getRegistryManager().get(RegistryKeys.STRUCTURE).getId(e.getKey());
                return id != null && id.getNamespace().equals("botumi");
            });
            wc.setStructureReferences(refs);
            wc.setNeedsSaving(true);
        }

        // Phase 2: Natural world features generation (trees, flowers, grass, ores, decorations) in memory
        RestorationWorldAccess worldAccess = new RestorationWorldAccess(sw, protoMap, center, effRadius, structureBounds);
        for (WorldChunk wc : targetChunks) {
            ChunkPos cp = wc.getPos();
            ProtoChunk proto = protoMap.get(cp);
            if (proto == null) continue;
            try {
                generator.generateFeatures(worldAccess, proto, filteredStructureAccessor);
            } catch (Exception e) {
                LOGGER.warn("[Botumi] Error during feature generation for chunk " + cp, e);
            }
        }

        // Phase 3: Build list of block restorations
        List<BlockRestoreEntry> plan = new java.util.ArrayList<>();
        java.util.Set<BlockPos> plannedPositions = new java.util.HashSet<>();
        BlockState savedDomeState = null;

        int scanMinY = Math.max(sw.getBottomY(), Math.min(structureBounds != null ? (int) structureBounds.minY - 12 : (int) center.y - 30, (int) center.y - (int) radius - 10));
        int scanMaxY = Math.min(sw.getTopY(), Math.max(structureBounds != null ? (int) structureBounds.maxY + 12 : (int) center.y + 30, (int) center.y + (int) radius + 10));

        for (WorldChunk wc : targetChunks) {
            ChunkPos cp = wc.getPos();
            ProtoChunk proto = protoMap.get(cp);
            if (proto == null) continue;

            int startX = cp.x << 4;
            int startZ = cp.z << 4;

            for (int lx = 0; lx < 16; lx++) {
                int wx = startX + lx;
                for (int lz = 0; lz < 16; lz++) {
                    int wz = startZ + lz;
                    double dx = wx + 0.5 - center.x;
                    double dz = wz + 0.5 - center.z;

                    boolean inside = (dx * dx + dz * dz <= rSq);
                    if (!inside && structureBounds != null
                            && (wx + 0.5 >= structureBounds.minX && wx + 0.5 <= structureBounds.maxX
                            && wz + 0.5 >= structureBounds.minZ && wz + 0.5 <= structureBounds.maxZ)) {
                        inside = true;
                    }

                    if (!inside) continue;

                    for (int wy = scanMinY; wy < scanMaxY; wy++) {
                        mut.set(wx, wy, wz);
                        BlockState targetSt = proto.getBlockState(mut);
                        BlockState curSt = sw.getBlockState(mut);

                        if (!curSt.equals(targetSt)) {
                            if (mut.equals(domePos)) {
                                savedDomeState = targetSt;
                            } else {
                                BlockPos immutable = mut.toImmutable();
                                plan.add(new BlockRestoreEntry(immutable, targetSt));
                                plannedPositions.add(immutable);
                            }
                        }
                    }
                }
            }
        }

        // Add any overflow blocks recorded by RestorationWorldAccess that lie within restoration zone
        for (java.util.Map.Entry<BlockPos, BlockState> entry : worldAccess.getOverflowMap().entrySet()) {
            BlockPos p = entry.getKey();
            if (p.equals(domePos)) {
                savedDomeState = entry.getValue();
                continue;
            }
            double dx = p.getX() + 0.5 - center.x;
            double dz = p.getZ() + 0.5 - center.z;
            boolean inside = (dx * dx + dz * dz <= rSq);
            if (!inside && structureBounds != null
                    && (p.getX() + 0.5 >= structureBounds.minX && p.getX() + 0.5 <= structureBounds.maxX
                    && p.getZ() + 0.5 >= structureBounds.minZ && p.getZ() + 0.5 <= structureBounds.maxZ)) {
                inside = true;
            }
            if (!inside) continue;

            if (!plannedPositions.contains(p)) {
                BlockState curSt = sw.getBlockState(p);
                if (!curSt.equals(entry.getValue())) {
                    plan.add(new BlockRestoreEntry(p, entry.getValue()));
                    plannedPositions.add(p);
                }
            }
        }

        // Sort plan strictly bottom-to-top (Y ascending), then radially outward from center
        plan.sort((e1, e2) -> {
            int yCmp = Integer.compare(e1.pos().getY(), e2.pos().getY());
            if (yCmp != 0) return yCmp;
            double d1 = (e1.pos().getX() + 0.5 - center.x) * (e1.pos().getX() + 0.5 - center.x)
                      + (e1.pos().getZ() + 0.5 - center.z) * (e1.pos().getZ() + 0.5 - center.z);
            double d2 = (e2.pos().getX() + 0.5 - center.x) * (e2.pos().getX() + 0.5 - center.x)
                      + (e2.pos().getZ() + 0.5 - center.z) * (e2.pos().getZ() + 0.5 - center.z);
            return Double.compare(d1, d2);
        });

        LOGGER.info("[Botumi] Restoration plan created: {} blocks to restore from Y={} to Y={}",
                plan.size(),
                plan.isEmpty() ? 0 : plan.get(0).pos().getY(),
                plan.isEmpty() ? 0 : plan.get(plan.size() - 1).pos().getY());

        return new RestorationPlan(plan, savedDomeState);
    }

    public static void cleanEntities(ServerWorld sw, Vec3d center, double radius) {
        int minY = Math.max(sw.getBottomY(), (int) center.y - 25);
        int maxY = Math.min(sw.getTopY(), (int) center.y + (int) radius + 16);
        Box box = new Box(
                center.x - radius - 16, minY, center.z - radius - 16,
                center.x + radius + 16, maxY, center.z + radius + 16
        );

        for (Entity e : sw.getOtherEntities(null, box, entity -> !entity.isRemoved() && !(entity instanceof PlayerEntity))) {
            if (e instanceof ItemEntity || e instanceof FallingBlockEntity
                    || e instanceof HostileEntity
                    || e instanceof DomeGuardianEntity || e instanceof CoreSentinelBossEntity) {
                e.discard();
            }
        }
    }

    public static class RestorationWorldAccess implements StructureWorldAccess {
        private final ServerWorld sw;
        private final java.util.Map<ChunkPos, ProtoChunk> protoMap;
        private final java.util.Map<ChunkPos, WrapperProtoChunk> wrapperMap = new java.util.HashMap<>();
        private final java.util.Map<BlockPos, BlockState> overflowMap = new java.util.concurrent.ConcurrentHashMap<>();
        private final Vec3d center;
        private final double radiusSq;
        private final double maxRadiusSq;
        private final double overflowRadiusSq;
        private final Box structureBounds;

        public RestorationWorldAccess(ServerWorld sw, java.util.Map<ChunkPos, ProtoChunk> protoMap, Vec3d center, double radius, @Nullable Box structureBounds) {
            this.sw = sw;
            this.protoMap = protoMap;
            this.center = center;
            this.radiusSq = radius * radius;
            this.maxRadiusSq = (radius + 6.0) * (radius + 6.0);
            this.overflowRadiusSq = (radius + 16.0) * (radius + 16.0);
            this.structureBounds = structureBounds;
        }

        public java.util.Map<BlockPos, BlockState> getOverflowMap() {
            return overflowMap;
        }

        private boolean isInsideZone(BlockPos pos) {
            double dx = pos.getX() + 0.5 - center.x;
            double dz = pos.getZ() + 0.5 - center.z;
            double dSq = dx * dx + dz * dz;
            if (dSq <= radiusSq) {
                return true;
            }
            if (structureBounds != null &&
                    pos.getX() >= structureBounds.minX && pos.getX() <= structureBounds.maxX &&
                    pos.getZ() >= structureBounds.minZ && pos.getZ() <= structureBounds.maxZ) {
                return true;
            }
            if (dSq <= maxRadiusSq) {
                return sw.getBlockState(pos).isAir();
            }
            return false;
        }

        @Override
        public Chunk getChunk(int x, int z) {
            ChunkPos cp = new ChunkPos(x, z);
            ProtoChunk proto = protoMap.get(cp);
            if (proto != null) {
                return proto;
            }
            return wrapperMap.computeIfAbsent(cp, p -> {
                WorldChunk wc = sw.getChunk(p.x, p.z);
                return new WrapperProtoChunk(wc, false);
            });
        }

        @Override
        public Chunk getChunk(int x, int z, ChunkStatus status, boolean create) {
            return getChunk(x, z);
        }

        @Override
        public boolean setBlockState(BlockPos pos, BlockState state, int flags, int maxUpdateDepth) {
            ChunkPos cp = new ChunkPos(pos);
            ProtoChunk proto = protoMap.get(cp);
            if (proto != null) {
                proto.setBlockState(pos, state, false);
                return true;
            }
            double dx = pos.getX() + 0.5 - center.x;
            double dz = pos.getZ() + 0.5 - center.z;
            if (dx * dx + dz * dz <= overflowRadiusSq) {
                overflowMap.put(pos.toImmutable(), state);
                return true;
            }
            return false;
        }

        @Override
        public boolean setBlockState(BlockPos pos, BlockState state, int flags) {
            return setBlockState(pos, state, flags, 512);
        }

        @Override
        public boolean isValidForSetBlock(BlockPos pos) {
            return isInsideZone(pos);
        }

        @Override
        public boolean spawnEntity(Entity entity) {
            return false;
        }

        @Override
        public long getSeed() {
            return sw.getSeed();
        }

        @Override
        public ServerWorld toServerWorld() {
            return sw;
        }

        @Override
        public DynamicRegistryManager getRegistryManager() {
            return sw.getRegistryManager();
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            BlockState overflow = overflowMap.get(pos);
            if (overflow != null) {
                return overflow;
            }
            ChunkPos cp = new ChunkPos(pos);
            ProtoChunk proto = protoMap.get(cp);
            if (proto != null) {
                return proto.getBlockState(pos);
            }
            return sw.getBlockState(pos);
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            ChunkPos cp = new ChunkPos(pos);
            ProtoChunk proto = protoMap.get(cp);
            if (proto != null) {
                return proto.getFluidState(pos);
            }
            return sw.getFluidState(pos);
        }

        @Override
        public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
            ChunkPos cp = new ChunkPos(pos);
            ProtoChunk proto = protoMap.get(cp);
            if (proto != null) {
                return proto.getBlockEntity(pos);
            }
            return sw.getBlockEntity(pos);
        }

        @Override
        public int getTopY(Heightmap.Type heightmap, int x, int z) {
            ChunkPos cp = new ChunkPos(x >> 4, z >> 4);
            ProtoChunk proto = protoMap.get(cp);
            if (proto != null) {
                return proto.sampleHeightmap(heightmap, x & 15, z & 15) + 1;
            }
            return sw.getTopY(heightmap, x, z);
        }

        @Override
        public int getBottomY() {
            return sw.getBottomY();
        }

        @Override
        public int getHeight() {
            return sw.getHeight();
        }

        @Override
        public WorldProperties getLevelProperties() {
            return sw.getLevelProperties();
        }

        @Override
        public LocalDifficulty getLocalDifficulty(BlockPos pos) {
            return sw.getLocalDifficulty(pos);
        }

        @Override
        public MinecraftServer getServer() {
            return sw.getServer();
        }

        @Override
        public net.minecraft.world.chunk.ChunkManager getChunkManager() {
            return sw.getChunkManager();
        }

        @Override
        public net.minecraft.util.math.random.Random getRandom() {
            return sw.getRandom();
        }

        @Override
        public BiomeAccess getBiomeAccess() {
            return sw.getBiomeAccess();
        }

        @Override
        public DimensionType getDimension() {
            return sw.getDimension();
        }

        @Override
        public boolean testFluidState(BlockPos pos, Predicate<FluidState> state) {
            return state.test(this.getFluidState(pos));
        }

        @Override
        public boolean testBlockState(BlockPos pos, Predicate<BlockState> state) {
            return state.test(this.getBlockState(pos));
        }

        @Override
        public RegistryEntry<Biome> getGeneratorStoredBiome(int biomeX, int biomeY, int biomeZ) {
            return sw.getGeneratorStoredBiome(biomeX, biomeY, biomeZ);
        }

        @Override
        public LightingProvider getLightingProvider() {
            return sw.getLightingProvider();
        }

        @Override
        public WorldBorder getWorldBorder() {
            return sw.getWorldBorder();
        }

        @Override
        public int getSeaLevel() {
            return sw.getSeaLevel();
        }

        @Override
        public boolean isClient() {
            return false;
        }

        @Override
        public int getAmbientDarkness() {
            return sw.getAmbientDarkness();
        }

        @Override
        public float getBrightness(Direction direction, boolean shaded) {
            return sw.getBrightness(direction, shaded);
        }

        @Override
        public FeatureSet getEnabledFeatures() {
            return sw.getEnabledFeatures();
        }

        @Override
        public QueryableTickScheduler<Block> getBlockTickScheduler() {
            return sw.getBlockTickScheduler();
        }

        @Override
        public QueryableTickScheduler<Fluid> getFluidTickScheduler() {
            return sw.getFluidTickScheduler();
        }

        @Override
        public long getTickOrder() {
            return sw.getTickOrder();
        }

        @Override
        public boolean removeBlock(BlockPos pos, boolean move) {
            if (isInsideZone(pos)) {
                return setBlockState(pos, net.minecraft.block.Blocks.AIR.getDefaultState(), 3);
            }
            return false;
        }

        @Override
        public boolean breakBlock(BlockPos pos, boolean drop, @Nullable Entity breakingEntity, int maxUpdateDepth) {
            if (isInsideZone(pos)) {
                return setBlockState(pos, net.minecraft.block.Blocks.AIR.getDefaultState(), 3);
            }
            return false;
        }

        @Override
        public List<Entity> getOtherEntities(@Nullable Entity except, Box box, Predicate<? super Entity> predicate) {
            return sw.getOtherEntities(except, box, predicate);
        }

        @Override
        public <T extends Entity> List<T> getEntitiesByType(TypeFilter<Entity, T> filter, Box box, Predicate<? super T> predicate) {
            return sw.getEntitiesByType(filter, box, predicate);
        }

        @Override
        public List<ServerPlayerEntity> getPlayers() {
            return sw.getPlayers();
        }

        @Override
        public void playSound(@Nullable PlayerEntity except, BlockPos pos, SoundEvent sound, SoundCategory category, float volume, float pitch) {
        }

        @Override
        public void addParticle(ParticleEffect parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
        }

        @Override
        public void syncWorldEvent(@Nullable PlayerEntity player, int eventId, BlockPos pos, int data) {
        }

        @Override
        public void emitGameEvent(RegistryEntry<GameEvent> event, Vec3d emitterPos, GameEvent.Emitter emitter) {
        }
    }
}
