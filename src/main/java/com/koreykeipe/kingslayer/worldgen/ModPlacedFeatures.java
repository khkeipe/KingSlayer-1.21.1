package com.koreykeipe.kingslayer.worldgen;

import com.koreykeipe.kingslayer.KingSlayer;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.*;

import java.util.List;

/**
 * Placement rules for each crate tier — the "where / how rare / how deep" half of
 * the worldgen pipeline.
 *
 * <p>Border-aware design: because the KingSlayer world border is small and
 * spawn-centered, exotic <em>surface</em> biomes often never generate inside it.
 * Tiers are therefore gated by <strong>depth</strong> rather than rare biomes:
 * surface crates are common, and the prize crates are buried progressively deeper,
 * which is reliably reachable by caving in any border.</p>
 */
public class ModPlacedFeatures {

    public static final ResourceKey<PlacedFeature> BROKEN_CRATE_PLACED_KEY = registerKey("broken_crate_placed");
    public static final ResourceKey<PlacedFeature> COMMON_CRATE_PLACED_KEY = registerKey("common_crate_placed");
    public static final ResourceKey<PlacedFeature> RARE_CRATE_PLACED_KEY   = registerKey("rare_crate_placed");
    public static final ResourceKey<PlacedFeature> EPIC_CRATE_PLACED_KEY   = registerKey("epic_crate_placed");

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        var cf = context.lookup(Registries.CONFIGURED_FEATURE);

        // BROKEN — abundant, on the surface. HEIGHTMAP_TOP_SOLID keeps it on solid
        // ground; SurfaceWaterDepthFilter(0) stops it floating on lakes/oceans.
        register(context, BROKEN_CRATE_PLACED_KEY, cf.getOrThrow(ModConfiguredFeatures.BROKEN_CRATE_KEY),
                List.of(RarityFilter.onAverageOnceEvery(8),
                        InSquarePlacement.spread(),
                        PlacementUtils.HEIGHTMAP_TOP_SOLID,
                        SurfaceWaterDepthFilter.forMaxDepth(0),
                        BiomeFilter.biome()));

        // COMMON — same surface treatment, rarer.
        register(context, COMMON_CRATE_PLACED_KEY, cf.getOrThrow(ModConfiguredFeatures.COMMON_CRATE_KEY),
                List.of(RarityFilter.onAverageOnceEvery(20),
                        InSquarePlacement.spread(),
                        PlacementUtils.HEIGHTMAP_TOP_SOLID,
                        SurfaceWaterDepthFilter.forMaxDepth(0),
                        BiomeFilter.biome()));

        // RARE — mid-depth underground; rests on a cave/ground floor between Y 0 and 56.
        register(context, RARE_CRATE_PLACED_KEY, cf.getOrThrow(ModConfiguredFeatures.RARE_CRATE_KEY),
                onCaveFloor(RarityFilter.onAverageOnceEvery(14),
                        VerticalAnchor.absolute(0), VerticalAnchor.absolute(56)));

        // EPIC — deep underground; rewards thorough caving. Y -50 to 16.
        register(context, EPIC_CRATE_PLACED_KEY, cf.getOrThrow(ModConfiguredFeatures.EPIC_CRATE_KEY),
                onCaveFloor(RarityFilter.onAverageOnceEvery(24),
                        VerticalAnchor.absolute(-50), VerticalAnchor.absolute(16)));
    }

    /**
     * Shared placement recipe for underground crates: scatter within a vertical band,
     * scan downward through air to find the floor, then step back up one block so the
     * crate rests on top of it. Attempts that don't land in an air pocket are skipped.
     */
    private static List<PlacementModifier> onCaveFloor(RarityFilter rarity, VerticalAnchor min, VerticalAnchor max) {
        return List.of(
                rarity,
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(min, max),
                EnvironmentScanPlacement.scanningFor(Direction.DOWN, BlockPredicate.solid(),
                        BlockPredicate.ONLY_IN_AIR_PREDICATE, 12),
                RandomOffsetPlacement.vertical(ConstantInt.of(1)),
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
