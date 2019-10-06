package de.skyrising.casual;

import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.MineshaftFeature;

public class MineshaftFinder implements StructureFinder<MineshaftFeature> {
    @Override
    public boolean isStartChunk(WorldChunk chunk) {
        ChunkPos pos = chunk.getPos();
        BlockPos.Mutable mutPos = new BlockPos.Mutable((pos.x << 4) + 9, 0, (pos.z << 4) + 9);
        Biome biome = chunk.getBiome(mutPos);
        if (!biome.hasStructureFeature(Feature.MINESHAFT)) return false;
        int x = (pos.x << 4) + 2, z = (pos.z << 4) + 2;
        for (int y = 10; y < 60; y++) {
            if (chunk.getBlockState(mutPos.set(x, y, z)).getBlock() != Blocks.DIRT) continue;
            for (int y2 = y + 1; y2 < 60; y2++) {
                if (chunk.getBlockState(mutPos.set(x + 5, y2, z + 5)).isOpaque()) {
                    if (y2 - y < 5) break;
                    return chunk.getBlockState(mutPos.set(x + 6, y, z + 6)).getBlock() == Blocks.DIRT;
                }
            }
        }
        return false;
    }

    @Override
    public MineshaftFeature getFeature() {
        return (MineshaftFeature) Feature.MINESHAFT;
    }
}
