package de.skyrising.casual;

import com.mojang.datafixers.util.Either;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.feature.Feature;

import javax.annotation.Nullable;
import java.util.Optional;

public interface StructureFinder<T extends Feature> {
    default boolean isStartBlock(World world, BlockPos pos, @Nullable BlockEntity blockEntity) {
        return isStartChunk(world.getWorldChunk(pos));
    }

    default boolean isStartChunk(WorldChunk chunk) {
        return false;
    }

    default Either<Optional<ChunkPos>, BlockPos> find(World world, BlockPos pos, @Nullable BlockEntity blockEntity) {
        return Either.left(Optional.of(new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4)));
    }

    T getFeature();
}
