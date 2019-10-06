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
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.DungeonFeature;
import net.minecraft.world.gen.feature.Feature;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Optional;

public class BuriedTreasureFinder implements StructureFinder<DungeonFeature> {
    @Override
    public boolean isStartBlock(World world, BlockPos pos, @Nullable BlockEntity blockEntity) {
        Biome biome = world.getBiome(pos);
        Biome.Category biomeCategory = biome.getCategory();
        return biomeCategory == Biome.Category.BEACH
            && (pos.getX() & 0xf) == 9 && (pos.getZ() & 0xf) == 9
            && pos.getY() > 40 && pos.getY() < 62;
    }

    @Override
    public DungeonFeature getFeature() {
        return (DungeonFeature) Feature.MONSTER_ROOM;
    }
}
