package com.koreykeipe.kingslayer.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * Places a single guaranteed crate at the origin, then scatters a low, terrain-hugging
 * debris pile of rarity-themed blocks around it. Works on dry land and on submerged
 * floors alike (the debris settles on the ocean/river bed).
 *
 * <p>Unlike {@link Feature#BLOCK_PILE}, the crate is always present exactly once — the
 * pile is purely decorative dressing, so the prize is never missing or duplicated.</p>
 */
public class CrateDebrisFeature extends Feature<CrateDebrisConfiguration> {

    public CrateDebrisFeature(Codec<CrateDebrisConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<CrateDebrisConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        RandomSource rand = ctx.random();
        CrateDebrisConfiguration cfg = ctx.config();

        // 1 — the guaranteed crate at the center.
        level.setBlock(origin, cfg.crate(), Block.UPDATE_CLIENTS);

        // 2 — scatter themed debris around it, each block riding its own column's surface
        //     so the pile follows slopes and sits flush on land or seabed.
        int r = cfg.radius();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx == 0 && dz == 0) continue;                 // leave room for the crate
                if (rand.nextFloat() > cfg.chance()) continue;    // sparse, natural scatter

                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z); // top of solid floor
                cursor.set(x, y, z);

                if (!level.getBlockState(cursor).canBeReplaced()) continue; // don't clobber logs/solids
                level.setBlock(cursor, cfg.debris().getState(rand, cursor), Block.UPDATE_CLIENTS);

                // Occasionally stack a second block for a moundy silhouette.
                if (rand.nextFloat() < 0.25f) {
                    cursor.move(0, 1, 0);
                    if (level.getBlockState(cursor).canBeReplaced()) {
                        level.setBlock(cursor, cfg.debris().getState(rand, cursor), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        return true;
    }
}
