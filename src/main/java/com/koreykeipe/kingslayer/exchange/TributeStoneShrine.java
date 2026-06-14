package com.koreykeipe.kingslayer.exchange;

import com.koreykeipe.kingslayer.KingSlayer;
import com.koreykeipe.kingslayer.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Builds (once) a small indestructible shrine housing the Tribute Stone at world spawn,
 * so the exchange hub is always dead-center and impossible to miss. Called on server
 * start; it no-ops if the shrine is already present.
 */
public final class TributeStoneShrine {

    private TributeStoneShrine() {}

    public static void ensureAtSpawn(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        int cx = spawn.getX();
        int cz = spawn.getZ();
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, cx, cz);
        BlockPos center = new BlockPos(cx, y, cz);

        // Already built — leave it alone.
        if (level.getBlockState(center).is(ModBlocks.TRIBUTE_STONE.get())) return;

        BlockState floorBrick   = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        BlockState floorCracked = Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
        BlockState pillar       = Blocks.REINFORCED_DEEPSLATE.defaultBlockState();

        // 5x5 platform at y-1 (a weathered mix of deepslate & cracked deepslate brick),
        // with headroom cleared above.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockState f = level.random.nextInt(4) == 0 ? floorCracked : floorBrick;
                level.setBlockAndUpdate(new BlockPos(cx + dx, y - 1, cz + dz), f);
                for (int dy = 0; dy < 3; dy++) {
                    level.setBlockAndUpdate(new BlockPos(cx + dx, y + dy, cz + dz), Blocks.AIR.defaultBlockState());
                }
            }
        }

        // Corner pillars topped with sea lanterns for ambiance.
        int[][] corners = {{-2, -2}, {-2, 2}, {2, -2}, {2, 2}};
        for (int[] c : corners) {
            level.setBlockAndUpdate(new BlockPos(cx + c[0], y, cz + c[1]), pillar);
            level.setBlockAndUpdate(new BlockPos(cx + c[0], y + 1, cz + c[1]), pillar);
            level.setBlockAndUpdate(new BlockPos(cx + c[0], y + 2, cz + c[1]), Blocks.SEA_LANTERN.defaultBlockState());
        }

        // The Tribute Stone at the center, on a gold dais.
        level.setBlockAndUpdate(center.below(), Blocks.GOLD_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(center, ModBlocks.TRIBUTE_STONE.get().defaultBlockState());

        KingSlayer.LOGGER.info("KingSlayer: Tribute Stone shrine ensured at world spawn {}", center);
    }
}
