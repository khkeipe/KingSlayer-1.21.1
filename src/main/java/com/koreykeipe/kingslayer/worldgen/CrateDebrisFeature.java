package com.koreykeipe.kingslayer.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * Places a single guaranteed crate at the origin, then heaps a tight, circular debris
 * mound of rarity-themed blocks around it — denser and taller toward the center. Works
 * on dry land and submerged floors alike (debris settles on the ocean/river bed).
 *
 * <p>Optionally a higher-tier "bonus" crate is hidden in the rubble at a low chance, so
 * a humble pile can occasionally over-deliver. Unlike {@link Feature#BLOCK_PILE}, the
 * primary crate is always present exactly once.</p>
 */
public class CrateDebrisFeature extends Feature<CrateDebrisConfiguration> {

    private static final int NO_PLACE = Integer.MIN_VALUE;

    public CrateDebrisFeature(Codec<CrateDebrisConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<CrateDebrisConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        RandomSource rand = ctx.random();
        CrateDebrisConfiguration cfg = ctx.config();
        int r = cfg.radius();

        // 1 — the guaranteed crate at the center.
        level.setBlock(origin, cfg.crate(), Block.UPDATE_CLIENTS);

        // 2 — optional higher-tier bonus crate, hidden at one rubble cell.
        long bonusCell = Long.MIN_VALUE;
        if (r > 0 && cfg.bonus().isPresent() && rand.nextFloat() < cfg.bonusChance()) {
            int bdx, bdz;
            do {
                bdx = Mth.nextInt(rand, -r, r);
                bdz = Mth.nextInt(rand, -r, r);
            } while (bdx == 0 && bdz == 0);
            placeOnSurface(level, origin.getX() + bdx, origin.getZ() + bdz,
                    cfg.bonus().get().getState(rand, origin));
            bonusCell = pack(bdx, bdz);
        }

        // 3 — the themed debris mound: a tight circular footprint, denser/taller at center.
        double maxD2 = (r + 0.5) * (r + 0.5);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx == 0 && dz == 0) continue;          // crate sits here
                if (pack(dx, dz) == bonusCell) continue;   // bonus crate sits here
                double d2 = dx * dx + dz * dz;
                if (d2 > maxD2) continue;                  // keep the pile round and compact
                if (rand.nextFloat() > cfg.chance()) continue;

                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                int y = placeOnSurface(level, x, z, cfg.debris().getState(rand, origin));

                // Mound up: cells adjacent to the crate may stack a second block.
                if (y != NO_PLACE && d2 <= 2.0 && rand.nextFloat() < 0.5f) {
                    BlockPos up = new BlockPos(x, y + 1, z);
                    if (level.getBlockState(up).canBeReplaced()) {
                        level.setBlock(up, cfg.debris().getState(rand, up), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        return true;
    }

    /** Places {@code state} on the topmost solid block of column (x,z); returns that Y, or {@link #NO_PLACE}. */
    private static int placeOnSurface(WorldGenLevel level, int x, int z, BlockState state) {
        int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        if (!level.getBlockState(pos).canBeReplaced()) return NO_PLACE; // don't clobber logs/solids
        if (!state.canSurvive(level, pos)) return NO_PLACE;             // skip props with no valid support
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        return y;
    }

    /** Packs a small (dx,dz) offset into a single long for cheap cell comparison. */
    private static long pack(int dx, int dz) {
        return ((long) dx << 32) ^ (dz & 0xffffffffL);
    }
}
