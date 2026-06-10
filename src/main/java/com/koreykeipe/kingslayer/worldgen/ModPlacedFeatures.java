package com.koreykeipe.kingslayer.worldgen;

import com.koreykeipe.kingslayer.KingSlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        var cf = context.lookup(Registries.CONFIGURED_FEATURE);

        // Lower the rarity number = more common. Broken is the bulk of world crates.
        register(context, BROKEN_CRATE_PLACED_KEY, cf.getOrThrow(ModConfiguredFeatures.BROKEN_CRATE_KEY),
                onSurface(6));
        register(context, COMMON_CRATE_PLACED_KEY, cf.getOrThrow(ModConfiguredFeatures.COMMON_CRATE_KEY),
                onSurface(12));
        // Rare/Epic appear only in their gated biomes (see ModBiomeModifiers), so a
        // modest rarity here still makes them scarce overall.
        register(context, RARE_CRATE_PLACED_KEY, cf.getOrThrow(ModConfiguredFeatures.RARE_CRATE_KEY),
                onSurface(10));
        register(context, EPIC_CRATE_PLACED_KEY, cf.getOrThrow(ModConfiguredFeatures.EPIC_CRATE_KEY),
                onSurface(14));
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

    private static ResourceKey<PlacedFeature> registerKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(KingSlayer.MOD_ID, name));
    }

    private static void register(BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key, Holder<ConfiguredFeature<?, ?>> configuration,
                                 List<PlacementModifier> modifiers) {
        context.register(key, new PlacedFeature(configuration, List.copyOf(modifiers)));
    }
}
