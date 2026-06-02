package com.xmtools.riftanchoring.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public final class ObsidianBaseDetector {

    private ObsidianBaseDetector() {}

    public static boolean hasValidBase(Level level, BlockPos anchorPos) {
        BlockPos centerBase = anchorPos.below();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos checkPos = centerBase.offset(dx, 0, dz);
                if (!level.getBlockState(checkPos).is(Blocks.OBSIDIAN)) {
                    return false;
                }
            }
        }

        return true;
    }
}
