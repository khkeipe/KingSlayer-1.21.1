package com.koreykeipe.kingslayer.worldgen;

import com.koreykeipe.kingslayer.KingSlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.*;

import java.util.List;

/**
 * Placement rules for each crate tier — the "how rare" half of the pipeline.
 *
 * <p>All tiers are placed on the <strong>surface</strong> (top solid block, never
 * floating on water). Tier difficulty is gated by <em>biome</em> rather than depth:
 * {@link ModBiomeModifiers} restricts the higher tiers to dangerous biomes and
 * mountain peaks, so the only thing changing here per tier is the spawn rarity.</p>
 */
public class ModPlacedFeatures {

    public static final ResourceKey<PlacedFeature> BROKEN_CRATE_PLACED_KEY = registerKey("broken_crate_placed");
    public static final ResourceKey<PlacedFeature> COMMON_CRATE_PLACED_KEY = registerKey("common_crate_placed");
    public static final ResourceKey<PlacedFeature> RARE_CRATE_PLACED_KEY   = registerKey("rare_crate_placed");
    public static final ResourceKey<PlacedFeature> EPIC_CRATE_PLACED_KEY   = registerKey("epic_crate_placed");

    public static final ResourceKey<PlacedFeature> DECOR_CAMP_PLACED_KEY   = registerKey("decor_camp_placed");
    public static final ResourceKey<PlacedFeature> DECOR_BATTLE_PLACED_KEY = registerKey("decor_battle_placed");
    public static final ResourceKey<PlacedFeature> DECOR_GRAVE_PLACED_KEY  = registerKey("decor_grave_placed");
    public static final ResourceKey<PlacedFeature> DECOR_RUINS_PLACED_KEY  = registerKey("decor_ruins_placed");

    public static final ResourceKey<PlacedFeature> KNIGHT_TENT_PLACED_KEY  = registerKey("knight_tent_placed");
    public static final ResourceKey<PlacedFeature> BIG_TENT_PLACED_KEY      = registerKey("big_tent_placed");
    public static final ResourceKey<PlacedFeature> OUTPOST_PLACED_KEY       = registerKey("outpost_01_placed");
    public static final ResourceKey<PlacedFeature> CRYPT_PLACED_KEY         = registerKey("crypt_placed");
    public static final ResourceKey<PlacedFeature> GRAVEYARD_PLACED_KEY     = registerKey("graveyard_placed");
    public static final ResourceKey<PlacedFeature> SHIP_PLACED_KEY          = registerKey("ship_placed");

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        var cf = context.lookup(Registries.CONFIGURED_FEATURE);

        // Lower the rarity number = more common. Crates are intentionally abundant so
        // players start making crafting moments immediately instead of grinding.
        // Broken stays on dry land only; Common/Rare/Epic may also land on the ocean
        // or river floor (their aquatic biomes are added in ModBiomeModifiers) — a
        // tempting crate guarded by the risk of drowning.
        register(context, BROKEN_CRATE_PLACED_KEY, cf.getOrThrow(ModConfiguredFeatures.BROKEN_CRATE_KEY),
                onSurface(16));
        register(context, COMMON_CRATE_PLACED_KEY, cf.getOrThrow(ModConfiguredFeatures.COMMON_CRATE_KEY),
                onSurfaceOrSeabed(20));
        // Rare/Epic appear only in their gated biomes (see ModBiomeModifiers), so a
        // modest rarity here still makes them scarce overall.
        register(context, RARE_CRATE_PLACED_KEY, cf.getOrThrow(ModConfiguredFeatures.RARE_CRATE_KEY),
                onSurfaceOrSeabed(16));
        register(context, EPIC_CRATE_PLACED_KEY, cf.getOrThrow(ModConfiguredFeatures.EPIC_CRATE_KEY),
                onSurfaceOrSeabed(16));

        // Decorative King's-realm piles — dry land, scattered for atmosphere as you explore.
        register(context, DECOR_CAMP_PLACED_KEY, cf.getOrThrow(ModConfiguredFeatures.DECOR_CAMP_KEY),
                onSurface(30));
        register(context, DECOR_BATTLE_PLACED_KEY, cf.getOrThrow(ModConfiguredFeatures.DECOR_BATTLE_KEY),
                onSurface(30));
        register(context, DECOR_GRAVE_PLACED_KEY, cf.getOrThrow(ModConfiguredFeatures.DECOR_GRAVE_KEY),
                onSurface(25));
        register(context, DECOR_RUINS_PLACED_KEY, cf.getOrThrow(ModConfiguredFeatures.DECOR_RUINS_KEY),
                onSurface(25));

        // Custom NBT structures. The two tents share the decor pool; halved to ~once per
        // 60 chunks each so they read as occasional landmarks, not a sea of tents.
        register(context, KNIGHT_TENT_PLACED_KEY, cf.getOrThrow(ModConfiguredFeatures.KNIGHT_TENT_KEY),
                onSurface(60));
        register(context, BIG_TENT_PLACED_KEY, cf.getOrThrow(ModConfiguredFeatures.BIG_TENT_KEY),
                onSurface(80));
        // Outpost tower — quartered to a genuinely rare find (they were clustering badly).
        register(context, OUTPOST_PLACED_KEY, cf.getOrThrow(ModConfiguredFeatures.OUTPOST_KEY),
                onSurface(176));

        // Crypt — quartered (was generating right on top of itself); now a scarce, special find.
        register(context, CRYPT_PLACED_KEY, cf.getOrThrow(ModConfiguredFeatures.CRYPT_KEY),
                onSurface(80));

        // Graveyard — same scarcity as the crypt.
        register(context, GRAVEYARD_PLACED_KEY, cf.getOrThrow(ModConfiguredFeatures.GRAVEYARD_KEY),
                onSurface(100));

        // Ship — floats on the sea surface; injected only into ocean biomes (ModBiomeModifiers).
        register(context, SHIP_PLACED_KEY, cf.getOrThrow(ModConfiguredFeatures.SHIP_KEY),
                onWaterSurface(100));
    }

    /**
     * Floating placement for water builds. Uses the WORLD_SURFACE heightmap, which counts fluid,
     * so the origin lands on the <em>water</em> surface rather than the seabed that
     * {@link #onSurface} targets. No water-depth filter — being in water is the whole point.
     */
    private static List<PlacementModifier> onWaterSurface(int rarity) {
        return List.of(
                RarityFilter.onAverageOnceEvery(rarity),
                InSquarePlacement.spread(),
                HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG),
                BiomeFilter.biome());
    }

    /**
     * Standard surface placement: scatter once per N chunks, land on the topmost
     * solid block, never on open water. Biome eligibility is decided by the biome
     * modifier that injects this feature.
     */
    private static List<PlacementModifier> onSurface(int rarity) {
        return List.of(
                RarityFilter.onAverageOnceEvery(rarity),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_TOP_SOLID,
                SurfaceWaterDepthFilter.forMaxDepth(0),
                BiomeFilter.biome());
    }

    /**
     * Like {@link #onSurface} but without the water-depth filter, so the crate may
     * also settle on a submerged floor (ocean/river bed). On dry land it behaves
     * identically — HEIGHTMAP_TOP_SOLID still lands it on the topmost solid block.
     */
    private static List<PlacementModifier> onSurfaceOrSeabed(int rarity) {
        return List.of(
                RarityFilter.onAverageOnceEvery(rarity),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_TOP_SOLID,
                BiomeFilter.biome());
    }

    private static ResourceKey<PlacedFeature> registerKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(KingSlayer.MOD_ID, name));
    }

    private static void register(BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key, Holder<ConfiguredFeature<?, ?>> configuration,
                                 List<PlacementModifier> modifiers) {
        context.register(key, new PlacedFeature(configuration, List.copyOf(modifiers)));
    }
}
