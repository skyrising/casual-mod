package de.skyrising.casual;

import com.mojang.datafixers.util.Either;
import fi.dy.masa.malilib.util.IntBoundingBox;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.ShipwreckFeature;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

public class ShipwreckFinder implements StructureFinder<ShipwreckFeature> {
    private final WorldDataTracker data;

    ShipwreckFinder(WorldDataTracker data) {
        this.data = data;
    }

    @Override
    public boolean isStartBlock(World world, BlockPos pos, @Nullable BlockEntity blockEntity) {
        if (!(blockEntity instanceof ChestBlockEntity) || !world.getBlockState(pos).get(ChestBlock.WATERLOGGED)) return false;
        Biome biome = world.getBiome(pos);
        Biome.Category biomeCategory = biome.getCategory();
        return biomeCategory == Biome.Category.OCEAN; // || true;
    }

    @Override
    public Either<Optional<ChunkPos>, BlockPos> find(World world, BlockPos pos, @Nullable BlockEntity blockEntity) {
        BlockPos.Mutable mutablePos = new BlockPos.Mutable(pos);
        Direction chestFacing = world.getBlockState(pos).get(ChestBlock.FACING);
        int[] stairs = new int[4];
        int totalStairs = 0;
        int[] trapdoors = new int[4];
        int totalTrapdoors = 0;
        for (int y = -1; y <= 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    mutablePos.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                    BlockState neighborState = world.getBlockState(mutablePos);
                    Block neighborBlock = neighborState.getBlock();
                    if (neighborBlock == Blocks.VOID_AIR) return Either.right(mutablePos.toImmutable());
                    if (neighborBlock instanceof StairsBlock) {
                        stairs[y + 1]++;
                        totalStairs++;
                    } else if (neighborBlock instanceof TrapdoorBlock) {
                        trapdoors[y + 1]++;
                        totalTrapdoors++;
                    }
                }
            }
        }
        //System.out.printf("%s: chest facing %s\n", pos, chestFacing);
        int chestX = 4;
        int chestY = 2;
        int chestZ = 0;
        int length = 16;
        int height = 9;
        Direction direction = chestFacing;
        if (trapdoors[3] > 4) { // with_mast[_degraded]
            chestZ = 9;
            height = 21;
            length = 28;
        } else if (totalTrapdoors == 0 && stairs[3] == 3) { // upsidedown_backhalf[_degraded]
            if (stairs[0] == 0) {
                chestX = 2;
                chestZ = 12;
                direction = chestFacing.getOpposite();
            } else { // redundant
                chestX = 3;
                chestY = 5;
                chestZ = 5;
                direction = chestFacing.rotateYClockwise();
            }
        } else if (totalTrapdoors == 0) { // rightsideup that have backhalf
            if (stairs[0] == 4) {
                if (totalStairs > 4) {
                    chestX = 6;
                    chestY = 4;
                    chestZ = 12;
                    direction = chestFacing.getOpposite();
                } else { // sideways backhalf
                    chestX = 6;
                    chestY = 3;
                    chestZ = 8;
                    length = 17;
                    direction = chestFacing.getOpposite();
                }
            } else if (stairs[0] == 3 && totalStairs > 5) {
                chestX = 5;
                chestZ = 6;
                direction = chestFacing.rotateYCounterclockwise();
            }
            mutablePos.set(pos);
            mutablePos.setOffset(0, -chestY, 0);
            mutablePos.setOffset(direction.rotateYClockwise(), chestX - 4);
            mutablePos.setOffset(direction, -chestZ - 1);
            if (world.getBlockState(mutablePos).getMaterial() == Material.WOOD) {
                if (length == 17) { // sideways
                    chestZ += 11;
                    length += 11;
                } else {
                    chestZ += 12;
                    length += 12;
                }
                mutablePos.setOffset(0, 10, 0);
                if (world.getBlockState(mutablePos).getBlock() instanceof LogBlock) {
                    height = 21;
                }
            }
        } else if (totalTrapdoors == 2 && trapdoors[3] == 2 && stairs[3] == 3) { // rightsideup_fronthalf[_degraded]
            chestZ = 8;
            length = 24;
        }
        Optional<ChunkPos> foundChunk = Optional.empty();
        if (chestZ != 0) {
            mutablePos.set(pos);
            mutablePos.setOffset(direction, 15 - chestZ);
            mutablePos.setOffset(direction.rotateYClockwise(), chestX - 4);
            BlockPos.Mutable pos2 = new BlockPos.Mutable(mutablePos);
            pos2.setOffset(0, -chestY, 0);
            pos2.setOffset(direction, -15);
            pos2.setOffset(direction.rotateYClockwise(), 4);
            BlockPos.Mutable pos3 = new BlockPos.Mutable(pos2);
            pos3.setOffset(direction, length - 1);
            pos3.setOffset(direction.rotateYClockwise(), -8);
            pos3.setOffset(0, height - 1, 0);
            IntBoundingBox box = new IntBoundingBox(
                    Math.min(pos2.getX(), pos3.getX()), pos2.getY(), Math.min(pos2.getZ(), pos3.getZ()),
                    Math.max(pos2.getX(), pos3.getX()), pos3.getY(), Math.max(pos2.getZ(), pos3.getZ()));
            mutablePos.setOffset(-4, -chestY, -15);
            data.markers.put(pos, new WorldDataTracker.Marker(WorldDataTracker.MarkerType.STRUCTURE,
                    Arrays.asList(mutablePos.toImmutable(), pos),
                    Collections.emptyList(),
                    Collections.singleton(box)));
            if ((mutablePos.getX() & 0xf) == 0 && (mutablePos.getZ() & 0xf) == 0) {
                foundChunk = Optional.of(new ChunkPos(mutablePos.getX() >> 4, mutablePos.getZ() >> 4));
            }
            //System.out.printf("%d,%d,%s -> predicted origin: %s\n", chestX, chestZ, direction, mutablePos);
        } else {
            data.markers.put(pos, new WorldDataTracker.Marker(WorldDataTracker.MarkerType.CHEST, pos));
        }
        if (chestZ == 0 || mutablePos.getZ() != 164) {
            //System.out.printf("%s stairs, %s trapdoors\n", Arrays.toString(stairs), Arrays.toString(trapdoors));
        }
        return Either.left(foundChunk);
    }

    @Override
    public ShipwreckFeature getFeature() {
        return (ShipwreckFeature) Feature.SHIPWRECK;
    }
}
