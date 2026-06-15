package com.koreykeipe.kingslayer.exchange;

import com.koreykeipe.kingslayer.KingSlayer;
import com.koreykeipe.kingslayer.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Builds (once) a small indestructible shrine housing the Tribute Stone at world spawn,
 * so the exchange hub is always dead-center and impossible to miss. Called on server
 * start; it no-ops if the shrine is already present.
 *
 * <p>The pad is levelled to the true ground: it finds the real surface (ignoring leaves,
 * logs, snow and other foliage), clears anything above each column, and fills underneath
 * down to solid ground — so the shrine sits flush instead of floating in trees or off a
 * slope.</p>
 */
public final class TributeStoneShrine {

    private static final int HEADROOM = 5;   // blocks cleared above the floor (trees, mounds, headroom)
    private static final int FILL_MAX = 12;  // how far down we'll fill to reach solid ground

    private TributeStoneShrine() {}

    public static void ensureAtSpawn(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        int cx = spawn.getX();
        int cz = spawn.getZ();

        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();

        // Idempotent: if a Tribute Stone already stands in the spawn column, leave it alone.
        for (int yy = level.getMaxBuildHeight() - 1; yy >= level.getMinBuildHeight(); yy--) {
            if (level.getBlockState(m.set(cx, yy, cz)).is(ModBlocks.TRIBUTE_STONE.get())) {
                return;
            }
        }

        int floorY = groundY(level, cx, cz); // the platform top sits flush at the real surface

        BlockState floorBrick   = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        BlockState floorCracked = Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
        BlockState pillar       = Blocks.REINFORCED_DEEPSLATE.defaultBlockState();

        // 5x5 levelled pad: clear above, lay the floor flush, fill underneath to solid ground.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int x = cx + dx, z = cz + dz;

                // Clear trees / dirt mounds / snow above the pad.
                for (int yy = floorY + 1; yy <= floorY + HEADROOM; yy++) {
                    level.setBlockAndUpdate(m.set(x, yy, z).immutable(), Blocks.AIR.defaultBlockState());
                }

                // Lay the floor flush with the surface.
                BlockState f = level.random.nextInt(4) == 0 ? floorCracked : floorBrick;
                level.setBlockAndUpdate(new BlockPos(x, floorY, z), f);

                // Fill underneath so nothing floats over slopes / overhangs.
                for (int yy = floorY - 1; yy >= floorY - FILL_MAX; yy--) {
                    BlockState below = level.getBlockState(m.set(x, yy, z));
                    boolean fillable = below.isAir()
                            || !below.getFluidState().isEmpty()
                            || below.is(BlockTags.LEAVES)
                            || below.is(BlockTags.REPLACEABLE);
                    if (fillable) {
                        level.setBlockAndUpdate(new BlockPos(x, yy, z), floorBrick);
                    } else {
                        break; // hit solid ground
                    }
                }
            }
        }

        // Corner pillars topped with sea lanterns for ambiance.
        int[][] corners = {{-2, -2}, {-2, 2}, {2, -2}, {2, 2}};
        for (int[] c : corners) {
            level.setBlockAndUpdate(new BlockPos(cx + c[0], floorY + 1, cz + c[1]), pillar);
            level.setBlockAndUpdate(new BlockPos(cx + c[0], floorY + 2, cz + c[1]), pillar);
            level.setBlockAndUpdate(new BlockPos(cx + c[0], floorY + 3, cz + c[1]), Blocks.SEA_LANTERN.defaultBlockState());
        }

        // The Tribute Stone at the centre, on a gold dais set into the floor.
        level.setBlockAndUpdate(new BlockPos(cx, floorY, cz), Blocks.GOLD_BLOCK.defaultBlockState());
        BlockPos stonePos = new BlockPos(cx, floorY + 1, cz);
        level.setBlockAndUpdate(stonePos, ModBlocks.TRIBUTE_STONE.get().defaultBlockState());

        KingSlayer.LOGGER.info("KingSlayer: Tribute Stone shrine ensured at world spawn {}", stonePos);
    }

    /**
     * Finds the true ground surface at a column: the highest solid block that isn't foliage
     * (skips air, fluids, logs, leaves, and replaceable plants/snow), so trees don't fool it.
     */
    private static int groundY(ServerLevel level, int x, int z) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        for (int yy = top; yy > level.getMinBuildHeight(); yy--) {
            BlockState s = level.getBlockState(m.set(x, yy, z));
            if (!s.isAir()
                    && s.getFluidState().isEmpty()
                    && !s.is(BlockTags.LOGS)
                    && !s.is(BlockTags.LEAVES)
                    && !s.is(BlockTags.REPLACEABLE)) {
                return yy;
            }
        }
        return level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
    }
}
