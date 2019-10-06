package de.skyrising.casual;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MutableIntBoundingBox;

public class Utils {
    private Utils() {}

    public static void expand(MutableIntBoundingBox box, BlockPos pos) {
        if (box.contains(pos)) return;
        box.setFrom(new MutableIntBoundingBox(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ()));
    }
}
