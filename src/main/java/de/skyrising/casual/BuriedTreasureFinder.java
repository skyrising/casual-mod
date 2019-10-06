package de.skyrising.casual;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.BuriedTreasureFeature;
import net.minecraft.world.gen.feature.Feature;

import javax.annotation.Nullable;

public class BuriedTreasureFinder implements StructureFinder<BuriedTreasureFeature> {
    @Override
    public boolean isStartBlock(World world, BlockPos pos, @Nullable BlockEntity blockEntity) {
        if (!(blockEntity instanceof ChestBlockEntity)) return false;
        return world.getBiome(pos).getCategory() == Biome.Category.BEACH
            && (pos.getX() & 0xf) == 9 && (pos.getZ() & 0xf) == 9
            && pos.getY() > 40 && pos.getY() < 62;
    }

    @Override
    public BuriedTreasureFeature getFeature() {
        return (BuriedTreasureFeature) Feature.BURIED_TREASURE;
    }
}
