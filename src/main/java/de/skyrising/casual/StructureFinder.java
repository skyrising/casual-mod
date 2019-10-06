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

/**
 * @param <T> The type of structure/feature this finder will search for
 */
public interface StructureFinder<T extends Feature> {
    /**
     * Is this block a starting block for searching the structure {@link T}
     * @param world The client world
     * @param pos The position of the block
     * @param blockEntity The block entity at that position
     * @return true if {@link #find(World, BlockPos, BlockEntity)} is likely to yield results with these arguments
     */
    default boolean isStartBlock(World world, BlockPos pos, @Nullable BlockEntity blockEntity) {
        return isStartChunk(world.getWorldChunk(pos));
    }

    /**
     * Is this chunk a starting point for searching the structure {@link T}
     * @param chunk The chunk
     * @return true if {@link #find(World, BlockPos, BlockEntity)} is likely to yield results
     *  with the arguments ({@code chunk.getWorld}, {@code chunk.getPos().toBlockPos(0, -1, 0)}, {@code null})
     */
    default boolean isStartChunk(WorldChunk chunk) {
        return false;
    }

    /**
     * Finds a structure of type {@link T} from the given starting point.
     * @param world The client world
     * @param pos The position of the block ({@code y == -1} for chunks)
     * @param blockEntity The block entity at {@code pos}
     * @return Either an optional {@link ChunkPos} indicating the structure's start if the search is complete
     *  or a {@link BlockPos} that needs to get loaded for the search to continue
     */
    default Either<Optional<ChunkPos>, BlockPos> find(World world, BlockPos pos, @Nullable BlockEntity blockEntity) {
        return Either.left(Optional.of(new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4)));
    }

    T getFeature();
}
