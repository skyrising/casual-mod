package de.skyrising.casual;

import com.mojang.datafixers.util.Either;
import fi.dy.masa.malilib.util.IntBoundingBox;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.DungeonFeature;
import net.minecraft.world.gen.feature.Feature;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Optional;

public class DungeonFinder implements StructureFinder<DungeonFeature> {
    private final WorldDataTracker data;

    DungeonFinder(WorldDataTracker data) {
        this.data = data;
    }

    @Override
    public boolean isStartBlock(World world, BlockPos pos, @Nullable BlockEntity blockEntity) {
        if (!(blockEntity instanceof MobSpawnerBlockEntity)) return false;
        EntityType type = ((MobSpawnerBlockEntity) blockEntity).getLogic().getRenderedEntity().getType();
        return type == EntityType.ZOMBIE || type == EntityType.SPIDER || type == EntityType.SKELETON;
    }

    @Override
    public Either<Optional<ChunkPos>, BlockPos> find(World world, BlockPos spawnerPos, @Nullable BlockEntity blockEntity) {
        BlockPos.Mutable mutablePos = new BlockPos.Mutable(spawnerPos);
        for (int yOff = 1; yOff < 5; yOff++) {
            BlockState state = world.getBlockState(mutablePos.set(spawnerPos.getX(), spawnerPos.getY() + yOff, spawnerPos.getZ()));
            if (yOff < 4 && !state.isAir()) return Either.left(Optional.empty());
            if (yOff == 4 && !state.getMaterial().isSolid()) return Either.left(Optional.empty());
        }
        Block bx = world.getBlockState(mutablePos.set(spawnerPos.getX() + 4, spawnerPos.getY() - 1, spawnerPos.getZ())).getBlock();
        if (bx == Blocks.VOID_AIR) return Either.right(mutablePos.toImmutable());
        int xSize = bx == Blocks.COBBLESTONE || bx == Blocks.MOSSY_COBBLESTONE ? 4 : 3;
        Block bz = world.getBlockState(mutablePos.set(spawnerPos.getX(), spawnerPos.getY() - 1, spawnerPos.getZ() + 4)).getBlock();
        if (bz == Blocks.VOID_AIR) return Either.right(mutablePos.toImmutable());
        int zSize = bz == Blocks.COBBLESTONE || bz == Blocks.MOSSY_COBBLESTONE ? 4 : 3;
        data.markers.put(spawnerPos, new WorldDataTracker.Marker(WorldDataTracker.MarkerType.SPAWNER, Collections.singleton(spawnerPos), Collections.emptyList(), Collections.singleton(
                new IntBoundingBox(
                    spawnerPos.getX() - xSize, spawnerPos.getY() - 1, spawnerPos.getZ() - zSize,
                    spawnerPos.getX() + xSize, spawnerPos.getY() + 3, spawnerPos.getZ() + zSize)
        )));
        return Either.left(Optional.empty());
    }

    @Override
    public DungeonFeature getFeature() {
        return (DungeonFeature) Feature.MONSTER_ROOM;
    }
}
