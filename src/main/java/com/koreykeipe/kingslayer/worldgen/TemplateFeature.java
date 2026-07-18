package com.koreykeipe.kingslayer.worldgen;

import com.koreykeipe.kingslayer.airdrop.AirdropConfig;
import com.koreykeipe.kingslayer.block.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
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

        // Reject wet spots. The placement's own water filter only checks the CENTRE column, so a
        // big footprint could still hang over a lake — and a buried build on a low shore floods.
        // maxWater = -1 opts out entirely (rafts / ships / underwater builds).
        if (cfg.maxWater() >= 0 && submergedColumns(level, footprint) > cfg.maxWater()) {
            return false;
        }

        // Inverse gate for water builds: the whole footprint must be over water, so a ship can't
        // end up beached on a shoreline or straddling an island.
        if (cfg.requireWater() && submergedColumns(level, footprint) < sampleCount(footprint)) {
            return false;
        }

        // Seat the build flush with the terrain before stamping: carve out any hillside that
        // would intersect it, and fill underneath so it never floats over a slope or overhang.
        if (cfg.level()) {
            levelFootprint(level, footprint);
        }

        boolean placed = template.placeInWorld(level, origin, origin, settings, ctx.random(), Block.UPDATE_CLIENTS);
        if (placed) {
            // Re-skin first, then resolve markers — marker-placed content is an explicit author
            // choice and shouldn't be re-themed.
            if (cfg.biomePalette()) {
                applyBiomePalette(level, template, origin, settings);
            }
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
            // Convenience: a bare block id (no "block:" prefix) is treated as a block outcome,
            // so entries like "minecraft:mud@2" work as written. Anything else → air.
            ResourceLocation rl = ResourceLocation.tryParse(o);
            if (rl != null && BuiltInRegistries.BLOCK.getOptional(rl).isPresent()) {
                resolveBlock(level, pos, o);
            } else {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
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
        ResourceLocation rl = ResourceLocation.tryParse(entityId.trim());
        EntityType<?> type = rl != null
                ? BuiltInRegistries.ENTITY_TYPE.getOptional(rl).orElse(EntityType.ZOMBIE)
                : EntityType.ZOMBIE;

        CompoundTag tag = spawnerNbt(type, pos);

        // Belt-and-braces: the two chunk states need different treatment, and getting it wrong
        // silently yields an EMPTY spawner. Do BOTH rather than pick one.

        // 1) Worldgen: the chunk is a ProtoChunk, so setBlock only left a placeholder block
        //    entity and getBlockEntity() returns null. Writing the NBT into the chunk gets it
        //    applied when the chunk is promoted to a full LevelChunk.
        level.getChunk(pos).setBlockEntityNbt(tag);

        // 2) Loaded chunk (e.g. /place feature): a real block entity already exists, and the
        //    pending NBT above would never be applied to it. Assign the mob directly FIRST so
        //    the spawner is never empty even if the fuller NBT load fails to parse, then layer
        //    the rest (light rules, spawn/player ranges) on top.
        if (level.getBlockEntity(pos) instanceof SpawnerBlockEntity sbe) {
            sbe.setEntityId(type, rand);
            sbe.loadWithComponents(tag, level.registryAccess());
            sbe.setChanged();
        }
    }

    /**
     * Builds mob-spawner block-entity NBT for the given entity, with {@code custom_spawn_rules}
     * opening both light limits to 0-15 so the spawner works in <em>any</em> light — daytime,
     * torch-lit rooms, or open sky. (It still needs a player within ~16 blocks, a non-Peaceful
     * difficulty, and open space to spawn into.)
     */
    private static CompoundTag spawnerNbt(EntityType<?> type, BlockPos pos) {
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        CompoundTag entityTag = new CompoundTag();
        entityTag.putString("id", typeId.toString());

        CompoundTag anyLight = new CompoundTag();
        anyLight.put("block_light_limit", lightRange());
        anyLight.put("sky_light_limit", lightRange());

        CompoundTag spawnData = new CompoundTag();
        spawnData.put("entity", entityTag);
        spawnData.put("custom_spawn_rules", anyLight);

        CompoundTag potentialData = new CompoundTag();
        potentialData.put("entity", entityTag.copy());
        potentialData.put("custom_spawn_rules", anyLight.copy());
        CompoundTag potential = new CompoundTag();
        potential.putInt("weight", 1);
        potential.put("data", potentialData);
        ListTag potentials = new ListTag();
        potentials.add(potential);

        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:mob_spawner");
        tag.putInt("x", pos.getX());
        tag.putInt("y", pos.getY());
        tag.putInt("z", pos.getZ());
        tag.put("SpawnData", spawnData);
        tag.put("SpawnPotentials", potentials);

        // Widen the search area so a spawner in a cramped/buried room can still find open space
        // (mobs may appear outside the structure). NOTE: the vanilla spawner reads
        // MaxNearbyEntities and RequiredPlayerRange as a PAIR — writing one without the other
        // leaves the player range at 0 and the spawner never activates, so always write both.
        tag.putShort("SpawnRange", (short) (int) AirdropConfig.SPAWNER_SPAWN_RANGE.get());
        tag.putShort("MaxNearbyEntities", (short) (int) AirdropConfig.SPAWNER_MAX_NEARBY.get());
        tag.putShort("RequiredPlayerRange", (short) (int) AirdropConfig.SPAWNER_PLAYER_RANGE.get());
        return tag;
    }

    /** An inclusive 0-15 light range — i.e. "any light level is valid". */
    private static CompoundTag lightRange() {
        CompoundTag range = new CompoundTag();
        range.putInt("min_inclusive", 0);
        range.putInt("max_inclusive", 15);
        return range;
    }

    /**
     * Re-skins the build to suit the biome it landed in. Runs AFTER placement and uses
     * {@link StructureTemplate#filterBlocks} so it only rewrites the template's OWN blocks —
     * surrounding terrain is never touched — and {@link Block#withPropertiesOf} so stairs keep
     * their facing/half and logs keep their axis. (A fixed-state swap would flatten them, which
     * is why this doesn't use a vanilla RuleProcessor.)
     */
    private static void applyBiomePalette(WorldGenLevel level, StructureTemplate template,
                                          BlockPos origin, StructurePlaceSettings settings) {
        java.util.Map<Block, Block> swaps = paletteFor(level.getBiome(origin));
        if (swaps.isEmpty()) return;
        for (java.util.Map.Entry<Block, Block> e : swaps.entrySet()) {
            for (StructureTemplate.StructureBlockInfo info :
                    template.filterBlocks(origin, settings, e.getKey())) {
                BlockState current = level.getBlockState(info.pos());
                if (current.is(e.getKey())) {
                    level.setBlock(info.pos(), e.getValue().withPropertiesOf(current), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    /** Block mapping for a biome, or empty to leave the authored palette alone. */
    private static java.util.Map<Block, Block> paletteFor(Holder<Biome> biome) {
        // Arid land: turf/soil → sand family, so a grassy build doesn't import a lawn.
        if (biome.is(Biomes.DESERT))          return aridSwaps(Blocks.SAND, Blocks.SANDSTONE);
        if (biome.is(BiomeTags.IS_BADLANDS))  return aridSwaps(Blocks.RED_SAND, Blocks.RED_SANDSTONE);

        // Ocean temperature: re-timber ships so tropical and cold seas read differently.
        if (biome.is(Biomes.WARM_OCEAN) || biome.is(Biomes.LUKEWARM_OCEAN)
                || biome.is(Biomes.DEEP_LUKEWARM_OCEAN)) {
            return timberSwaps(Blocks.OAK_LOG, Blocks.STRIPPED_OAK_LOG, Blocks.OAK_SLAB, Blocks.OAK_STAIRS);
        }
        if (biome.is(Biomes.COLD_OCEAN) || biome.is(Biomes.DEEP_COLD_OCEAN)) {
            return timberSwaps(Blocks.SPRUCE_LOG, Blocks.STRIPPED_SPRUCE_LOG, Blocks.SPRUCE_SLAB, Blocks.SPRUCE_STAIRS);
        }
        return java.util.Map.of(); // temperate ocean / anywhere else — keep the build as authored
    }

    private static java.util.Map<Block, Block> aridSwaps(Block top, Block sub) {
        return java.util.Map.of(
                Blocks.GRASS_BLOCK, top,
                Blocks.PODZOL,      top,
                Blocks.DIRT_PATH,   top,
                Blocks.DIRT,        sub,
                Blocks.COARSE_DIRT, sub,
                Blocks.ROOTED_DIRT, sub);
    }

    /** Dark-oak hull → another wood set (the ship's dark oak family). */
    private static java.util.Map<Block, Block> timberSwaps(Block log, Block stripped, Block slab, Block stairs) {
        return java.util.Map.of(
                Blocks.DARK_OAK_LOG,          log,
                Blocks.STRIPPED_DARK_OAK_LOG, stripped,
                Blocks.DARK_OAK_SLAB,         slab,
                Blocks.DARK_OAK_STAIRS,       stairs);
    }

    /**
     * Counts sampled footprint columns that have fluid standing on them. Compares the world
     * surface (which includes water) against the ocean floor (which doesn't) — if the surface is
     * higher, that column is underwater.
     */
    /** How many columns {@link #submergedColumns} samples — the stride must match (every 2 blocks). */
    private static int sampleCount(BoundingBox bb) {
        int nx = ((bb.maxX() - bb.minX()) / 2) + 1;
        int nz = ((bb.maxZ() - bb.minZ()) / 2) + 1;
        return nx * nz;
    }

    private static int submergedColumns(WorldGenLevel level, BoundingBox bb) {
        int wet = 0;
        for (int x = bb.minX(); x <= bb.maxX(); x += 2) {
            for (int z = bb.minZ(); z <= bb.maxZ(); z += 2) {
                int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                int floor   = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
                if (surface > floor) wet++;
            }
        }
        return wet;
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
