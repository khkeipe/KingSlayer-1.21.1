package com.koreykeipe.kingslayer.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.Optional;

/**
 * Stamps a saved NBT structure template into the world at the placement origin,
 * randomly rotated and roughly centered. A simple "Route A" placer that rides the
 * same configured/placed/biome-modifier pipeline as the decorative piles — no
 * jigsaw, no structure-set, no {@code /locate}.
 *
 * <p>If the named template doesn't exist yet (e.g. you haven't built/saved the NBT),
 * this is a harmless no-op, so the slot can be wired up ahead of the build.</p>
 *
 * <p>Templates load from {@code data/<namespace>/structure/<path>.nbt} — note the
 * <strong>singular</strong> {@code structure} folder in 1.21.</p>
 */
public class TemplateFeature extends Feature<TemplateConfiguration> {

    public TemplateFeature(Codec<TemplateConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<TemplateConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        TemplateConfiguration cfg = ctx.config();

        StructureTemplateManager manager = level.getLevel().getStructureManager();
        Optional<StructureTemplate> opt = manager.get(cfg.template());
        if (opt.isEmpty()) return false; // NBT not present yet — nothing to place
        StructureTemplate template = opt.get();

        Rotation rotation = Rotation.getRandom(ctx.random());
        Vec3i size = template.getSize();
        // Centre the (unrotated) footprint on the origin; pivot rotation about that centre.
        BlockPos pivot = new BlockPos(size.getX() / 2, 0, size.getZ() / 2);
        BlockPos origin = ctx.origin().offset(-pivot.getX(), 0, -pivot.getZ());

        // Sink the build so its bulk is underground (crypts/bunkers); only the top layers,
        // which should be authored to pierce the surface, remain exposed. The surrounding
        // hill is left intact (use level=false) and structure_void cells let it wrap the build.
        if (cfg.buryDepth() > 0) {
            origin = origin.below(cfg.buryDepth());
        }

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(rotation)
                .setMirror(Mirror.NONE)
                .setRotationPivot(pivot)
                .setIgnoreEntities(false);
        if (cfg.integrity() < 1.0f) {
            settings.addProcessor(new BlockRotProcessor(cfg.integrity())); // weathered / damaged look
        }

        BoundingBox footprint = template.getBoundingBox(settings, origin);

        // Reject spots that are too steep: a flat shell on a cliff face juts out into open air
        // and can't be saved by leveling or burying. Skip here and worldgen tries elsewhere.
        if (cfg.maxSlope() > 0 && surfaceSpread(level, footprint) > cfg.maxSlope()) {
            return false;
        }

        // Seat the build flush with the terrain before stamping: carve out any hillside that
        // would intersect it, and fill underneath so it never floats over a slope or overhang.
        if (cfg.level()) {
            levelFootprint(level, footprint);
        }

        return template.placeInWorld(level, origin, origin, settings, ctx.random(), Block.UPDATE_CLIENTS);
    }

    /**
     * Height variation (max − min) of the natural worldgen surface sampled across the build's
     * footprint. A small value means flat ground; a large one means a slope or cliff edge.
     */
    private static int surfaceSpread(WorldGenLevel level, BoundingBox bb) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int x = bb.minX(); x <= bb.maxX(); x += 2) {
            for (int z = bb.minZ(); z <= bb.maxZ(); z += 2) {
                int h = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                if (h < min) min = h;
                if (h > max) max = h;
            }
        }
        return max - min;
    }

    /** How far below the build's base we'll pour foundation to reach solid ground. */
    private static final int FILL_MAX = 16;

    /**
     * Levels the terrain inside a structure's world-space footprint so the build sits flush:
     * <ol>
     *   <li>Clears any natural terrain occupying the structure's own volume (so a hill can't
     *       poke up through the floor or roof).</li>
     *   <li>Fills the gap from just under the base down to the first solid block, blending
     *       the foundation to the surrounding ground material.</li>
     * </ol>
     * Operates only within the given bounding box, so a small build stays inside its region.
     */
    private static void levelFootprint(WorldGenLevel level, BoundingBox bb) {
        int baseY = bb.minY();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();

        for (int x = bb.minX(); x <= bb.maxX(); x++) {
            for (int z = bb.minZ(); z <= bb.maxZ(); z++) {

                // 1) Carve out terrain that would intersect the structure's volume.
                for (int y = baseY; y <= bb.maxY(); y++) {
                    m.set(x, y, z);
                    if (!level.getBlockState(m).isAir()) {
                        level.setBlock(m, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }

                // 2) Pick a foundation material that blends with this column's ground.
                BlockState fill = Blocks.DIRT.defaultBlockState();
                for (int y = baseY - 1; y >= baseY - FILL_MAX - 1; y--) {
                    BlockState s = level.getBlockState(m.set(x, y, z));
                    if (isGround(s)) { fill = subsoil(s); break; }
                }

                // 3) Fill any void beneath the base down to solid ground.
                for (int y = baseY - 1; y >= baseY - FILL_MAX; y--) {
                    BlockState s = level.getBlockState(m.set(x, y, z));
                    boolean fillable = s.isAir()
                            || !s.getFluidState().isEmpty()
                            || s.is(BlockTags.LEAVES)
                            || s.is(BlockTags.REPLACEABLE);
                    if (fillable) {
                        level.setBlock(m.immutable(), fill, Block.UPDATE_CLIENTS);
                    } else {
                        break; // reached solid ground
                    }
                }
            }
        }
    }

    /** True solid ground: not air/fluid, not foliage, not loose plant cover. */
    private static boolean isGround(BlockState s) {
        return !s.isAir()
                && s.getFluidState().isEmpty()
                && !s.is(BlockTags.LOGS)
                && !s.is(BlockTags.LEAVES)
                && !s.is(BlockTags.REPLACEABLE);
    }

    /** Swap thin surface cover (grass/podzol/path) for plain dirt so columns don't read as turf-filled. */
    private static BlockState subsoil(BlockState surface) {
        if (surface.is(Blocks.GRASS_BLOCK) || surface.is(Blocks.PODZOL)
                || surface.is(Blocks.MYCELIUM) || surface.is(Blocks.DIRT_PATH)
                || surface.is(Blocks.FARMLAND) || surface.is(Blocks.SNOW_BLOCK)) {
            return Blocks.DIRT.defaultBlockState();
        }
        return surface;
    }
}
