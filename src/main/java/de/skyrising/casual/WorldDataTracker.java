package de.skyrising.casual;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.mojang.datafixers.util.Either;
import fi.dy.masa.malilib.util.IntBoundingBox;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.Pair;
import net.minecraft.util.ThreadExecutor;
import net.minecraft.util.math.*;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.feature.Feature;
import org.apache.logging.log4j.LogManager;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WorldDataTracker {
    private static WorldDataTracker instance = null;
    final World world;

    public Multimap<ChunkPos, Feature> features = MultimapBuilder.hashKeys().hashSetValues(1).build();
    private final Set<StructureFinder<?>> finders = new HashSet<>();
    private final Multimap<ChunkPos, Pair<StructureFinder<?>, BlockPos>> queues = MultimapBuilder.hashKeys().hashSetValues().build();

    {
        finders.add(new BuriedTreasureFinder());
        finders.add(new MineshaftFinder(this));
        finders.add(new ShipwreckFinder(this));
        finders.add(new DungeonFinder(this));
    }

    public Object2FloatMap<ChunkPos> slimeChunks = new Object2FloatOpenHashMap<>();
    private ObjectSet<UUID> knownEntities = new ObjectOpenHashSet<>();

    public Map<BlockPos, Marker> markers = new ConcurrentHashMap<>();

    public WorldDataTracker(World world) {
        this.world = world;
    }

    public static WorldDataTracker getInstance(World world) {
        if (instance == null || instance.world != world) instance = new WorldDataTracker(world);
        return instance;
    }

    public void updateSlimeChunk(ChunkPos pos, float uncertainty) {
        float current = slimeChunks.getOrDefault(pos, 1);
        float newUncertainty = current * uncertainty;
        if (newUncertainty < current && newUncertainty < 0.1) {
            System.out.printf("%s: slime chunk %f\n", pos, newUncertainty);
        }
        slimeChunks.put(pos, newUncertainty);
    }

    public void addStructure(ChunkPos pos, Feature feature) {
        features.put(pos, feature);
        markers.put(new BlockPos(pos.x << 4, 0, pos.z << 4), new Marker(MarkerType.STRUCTURE, pos));
        System.out.printf("%s: %s\n", pos, Registry.FEATURE.getId(feature));
    }

    public void onSlimeSpawn(SlimeEntity slime) {
        if (!knownEntities.add(slime.getUuid())) return;
        Vec3d pos = slime.getPos();
        if (pos.y > 40 || pos.y < 0) return;
        double xOffset = pos.x % 1;
        if (xOffset < 0) xOffset += 1;
        double yOffset = pos.y % 1;
        if (yOffset < 0) yOffset += 1;
        double zOffset = pos.z % 1;
        if (zOffset < 0) zOffset += 1;
        int cx = (int) pos.x >> 4;
        int cz = (int) pos.z >> 4;
        ChunkPos chunkPos = new ChunkPos(cx, cz);
        if (xOffset == 0.5 && zOffset == 0.5 && yOffset == 0) {
            updateSlimeChunk(chunkPos, 0);
            return;
        }
        double centerX = (cx << 4) + 8;
        double centerZ = (cz << 4) + 8;
        double distX = Math.abs(pos.x - centerX);
        double distZ = Math.abs(pos.z - centerZ);
        updateSlimeChunk(chunkPos, (float) Math.max(distX, distZ) / 8);
    }

    private void runFinder(StructureFinder<?> finder, BlockPos pos, @Nullable BlockEntity blockEntity) {
        try {
            Either<Optional<ChunkPos>, BlockPos> result = finder.find(world, pos, blockEntity);
            result.ifRight(unloadedPos -> {
                ChunkPos unloadedChunkPos = new ChunkPos(unloadedPos.getX() >> 4, unloadedPos.getZ() >> 4);
                queues.put(unloadedChunkPos, new Pair<>(finder, pos));
            }).ifLeft(featureChunk -> {
                featureChunk.ifPresent(value -> addStructure(value, finder.getFeature()));
            });
        } catch (Exception e) {
            LogManager.getLogger().warn("Exception running structure finder for " + Registry.FEATURE.getId(finder.getFeature()), e);
        }
    }

    public void onChunkLoad(WorldChunk chunk) {
        MinecraftClient.getInstance().execute(() -> {
            ChunkPos chunkPos = chunk.getPos();
            long start = System.nanoTime();
            for (StructureFinder<?> finder : finders) {
                if (!finder.isStartChunk(chunk)) continue;
                BlockPos pos = chunk.getPos().toBlockPos(0, -1, 0);
                runFinder(finder, pos, null);
            }
            for (Map.Entry<BlockPos, BlockEntity> e : chunk.getBlockEntities().entrySet()) {
                BlockPos pos = e.getKey();
                BlockEntity be = e.getValue();
                if (be instanceof MobSpawnerBlockEntity) {
                    EntityType type = ((MobSpawnerBlockEntity) be).getLogic().getRenderedEntity().getType();
                    markers.putIfAbsent(pos, new Marker(MarkerType.SPAWNER, pos));
                    System.out.printf("%s spawner @%s\n", EntityType.getId(type), pos);
                }
                for (StructureFinder<?> finder : finders) {
                    if (!finder.isStartBlock(world, pos, be)) continue;
                    runFinder(finder, pos, be);
                }
            }
            for (Pair<StructureFinder<?>, BlockPos> entry : queues.removeAll(chunkPos)) {
                BlockPos pos = entry.getRight();
                ChunkPos queuedChunk = new ChunkPos(pos.getX() >> 4, pos.getY() >> 4);
                if (((ClientWorld) world).method_2935().isChunkLoaded(queuedChunk.x, queuedChunk.z)) {
                    runFinder(entry.getLeft(), pos, world.getBlockEntity(pos));
                } else {
                    queues.put(queuedChunk, entry);
                }
            }
            //System.out.printf("Processed chunk %s in %fms\n", chunkPos, (System.nanoTime() - start) / 1e6);
        });
    }

    public static class Marker {
        public final MarkerType type;
        public final Set<BlockPos> blocks;
        public final Set<ChunkPos> chunks;
        public final Set<IntBoundingBox> boxes;

        public Marker(MarkerType type, Set<BlockPos> blocks, Set<ChunkPos> chunks, Set<IntBoundingBox> boxes) {
            this.type = type;
            this.blocks = blocks;
            this.chunks = chunks;
            this.boxes = boxes;
        }

        public Marker(MarkerType type, Collection<BlockPos> blocks, Collection<ChunkPos> chunks, Collection<IntBoundingBox> boxes) {
            this(type, new LinkedHashSet<>(blocks), new LinkedHashSet<>(chunks), new LinkedHashSet<>(boxes));
        }

        public Marker(MarkerType type, BlockPos ...blocks) {
            this(type, Arrays.asList(blocks), Collections.emptyList(), Collections.emptyList());
        }

        public Marker(MarkerType type, ChunkPos ...chunks) {
            this(type, Collections.emptyList(), Arrays.asList(chunks), Collections.emptyList());
        }
    }

    public enum MarkerType {
        STRUCTURE, CHEST, SPAWNER, MINESHAFT
    }
}
