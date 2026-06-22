package com.koreykeipe.kingslayer.worldgen;

import com.koreykeipe.kingslayer.airdrop.AirdropConfig;
import com.koreykeipe.kingslayer.block.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.Locale;
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

        boolean placed = template.placeInWorld(level, origin, origin, settings, ctx.random(), Block.UPDATE_CLIENTS);
        if (placed) {
            resolveMarkers(level, template, origin, settings, ctx.random());
        }
        return placed;
    }

    // -------------------------------------------------------------------------
    // Data-marker content — DATA-mode structure blocks the build author scatters,
    // resolved at generation so each placement can randomise crates/spawners.
    //   kcs:crate              → crate (random tier), rolled against the config chance
    //   kcs:crate:<tier>       → that tier (broken|common|rare|epic|random)
    //   kcs:spawner:<entityId> → a mob spawner for that entity (e.g. minecraft:zombie)
    // Any other / leftover structure block is cleared to air so none leak into the world.
    // -------------------------------------------------------------------------

    private static void resolveMarkers(WorldGenLevel level, StructureTemplate template,
                                       BlockPos origin, StructurePlaceSettings settings, RandomSource rand) {
        for (StructureTemplate.StructureBlockInfo info :
                template.filterBlocks(origin, settings, Blocks.STRUCTURE_BLOCK)) {
            String meta = info.nbt() != null ? info.nbt().getString("metadata") : "";
            BlockPos pos = info.pos();
            if (meta.startsWith("kcs:pool:")) {
                resolvePool(level, pos, meta.substring("kcs:pool:".length()), rand);
            } else if (meta.equals("kcs:crate") || meta.startsWith("kcs:crate:")) {
                resolveCrate(level, pos, meta, rand);
            } else if (meta.startsWith("kcs:spawner:")) {
                resolveSpawner(level, pos, meta.substring("kcs:spawner:".length()), rand);
            } else if (meta.startsWith("kcs:block:")) {
                resolveBlock(level, pos, meta.substring("kcs:block:".length()));
            } else {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    /**
     * Resolves a weighted pool marker so one spot can randomly be a crate, a spawner, a plain
     * block, or nothing — varying every generation.
     *
     * <p>Format: {@code kcs:pool:<entry>;<entry>;...} where each entry is an outcome with an
     * optional {@code @weight} (default 1). One entry is chosen at random by weight. Outcomes:
     * {@code crate}, {@code crate:<tier>}, {@code spawner:<entityId>}, {@code block:<blockId>},
     * {@code air}. Example:</p>
     * <pre>kcs:pool:crate:mystery@2;spawner:minecraft:zombie@1;block:minecraft:gold_block@3;air@4</pre>
     */
    private static void resolvePool(WorldGenLevel level, BlockPos pos, String spec, RandomSource rand) {
        java.util.List<String> outcomes = new java.util.ArrayList<>();
        java.util.List<Integer> weights = new java.util.ArrayList<>();
        int total = 0;
        for (String entry : spec.split(";")) {
            String e = entry.trim();
            if (e.isEmpty()) continue;
            int at = e.lastIndexOf('@');
            String outcome = at >= 0 ? e.substring(0, at) : e;
            int weight = 1;
            if (at >= 0) {
                try { weight = Integer.parseInt(e.substring(at + 1).trim()); } catch (NumberFormatException ignored) {}
            }
            if (weight <= 0) continue;
            outcomes.add(outcome);
            weights.add(weight);
            total += weight;
        }
        if (total <= 0) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            return;
        }
        int r = rand.nextInt(total);
        String chosen = outcomes.get(outcomes.size() - 1);
        for (int i = 0; i < outcomes.size(); i++) {
            r -= weights.get(i);
            if (r < 0) { chosen = outcomes.get(i); break; }
        }
        resolveOutcome(level, pos, chosen, rand);
    }

    /** Places one deterministic outcome (the shared core used by pools). */
    private static void resolveOutcome(WorldGenLevel level, BlockPos pos, String outcome, RandomSource rand) {
        String o = outcome.trim();
        String lower = o.toLowerCase(Locale.ROOT);
        if (lower.isEmpty() || lower.equals("air") || lower.equals("none")
                || lower.equals("empty") || lower.equals("nothing")) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        } else if (lower.equals("crate") || lower.startsWith("crate:")) {
            String tier = lower.startsWith("crate:") ? lower.substring("crate:".length()) : "random";
            Block crate = crateForTier(tier);
            if (crate == null) crate = randomCrate(rand);
            level.setBlock(pos, crate.defaultBlockState(), Block.UPDATE_CLIENTS);
        } else if (lower.startsWith("spawner:")) {
            resolveSpawner(level, pos, o.substring("spawner:".length()), rand);
        } else if (lower.startsWith("block:")) {
            resolveBlock(level, pos, o.substring("block:".length()));
        } else {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Places a plain block by id (default state); unknown id → air. */
    private static void resolveBlock(WorldGenLevel level, BlockPos pos, String blockId) {
        ResourceLocation rl = ResourceLocation.tryParse(blockId.trim());
        Block b = rl != null ? BuiltInRegistries.BLOCK.getOptional(rl).orElse(null) : null;
        level.setBlock(pos, (b != null ? b : Blocks.AIR).defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    private static void resolveCrate(WorldGenLevel level, BlockPos pos, String meta, RandomSource rand) {
        String[] parts = meta.split(":"); // kcs:crate[:tier]
        String tier = parts.length >= 3 ? parts[2].toLowerCase(Locale.ROOT) : "random";
        Block crate = crateForTier(tier);

        // Bare / random / unknown tier = "scatter" loot: roll the configured chance (so placement
        // varies), and pick a random tier when it lands. An EXPLICIT tier is a deliberate
        // centrepiece crate, so it always places.
        if (crate == null) {
            if (rand.nextFloat() >= AirdropConfig.STRUCTURE_CRATE_CHANCE.get()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                return;
            }
            crate = randomCrate(rand);
        }
        level.setBlock(pos, crate.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    /** Maps a tier tag to its crate block, or null for "random"/unknown (scatter behaviour). */
    private static Block crateForTier(String tier) {
        return switch (tier) {
            case "broken"    -> ModBlocks.BROKEN_CRATE.get();
            case "common"    -> ModBlocks.COMMON_CRATE.get();
            case "rare"      -> ModBlocks.RARE_CRATE.get();
            case "epic"      -> ModBlocks.EPIC_CRATE.get();
            case "mystery"   -> ModBlocks.MYSTERY_CRATE.get();
            case "bounty"    -> ModBlocks.BOUNTY_CRATE.get();
            case "broken_ad" -> ModBlocks.BROKEN_AD_CRATE.get();
            case "common_ad" -> ModBlocks.COMMON_AD_CRATE.get();
            case "rare_ad"   -> ModBlocks.RARE_AD_CRATE.get();
            case "epic_ad"   -> ModBlocks.EPIC_AD_CRATE.get();
            default          -> null; // "random" or unrecognised
        };
    }

    /** Weighted random crate tier for bare {@code kcs:crate}: broken 40%, common 30%, rare 20%, epic 10%. */
    private static Block randomCrate(RandomSource rand) {
        int r = rand.nextInt(10);
        if (r < 4) return ModBlocks.BROKEN_CRATE.get();
        if (r < 7) return ModBlocks.COMMON_CRATE.get();
        if (r < 9) return ModBlocks.RARE_CRATE.get();
        return ModBlocks.EPIC_CRATE.get();
    }

    private static void resolveSpawner(WorldGenLevel level, BlockPos pos, String entityId, RandomSource rand) {
        level.setBlock(pos, Blocks.SPAWNER.defaultBlockState(), Block.UPDATE_CLIENTS);
        ResourceLocation rl = ResourceLocation.tryParse(entityId);
        EntityType<?> type = rl != null
                ? BuiltInRegistries.ENTITY_TYPE.getOptional(rl).orElse(EntityType.ZOMBIE)
                : EntityType.ZOMBIE;
        if (level.getBlockEntity(pos) instanceof SpawnerBlockEntity sbe) {
            sbe.setEntityId(type, rand);
        }
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
