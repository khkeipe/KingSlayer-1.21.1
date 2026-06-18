package com.koreykeipe.kingslayer.worldgen;

import com.koreykeipe.kingslayer.KingSlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;
import java.util.stream.Stream;

/**
 * Injects each crate tier's placed feature into the biomes that gate it.
 *
 * <p>All four tiers are surface features ({@link GenerationStep.Decoration#VEGETAL_DECORATION}).
 * Difficulty is expressed through biome eligibility:</p>
 * <ul>
 *   <li><b>Broken</b> — every overworld biome (via the {@code is_overworld} tag, dry land only) so basic resources are always nearby.</li>
 *   <li><b>Common</b> — gentle, common, easy-to-reach biomes.</li>
 *   <li><b>Rare</b> — "dangerous" biomes: jungles, swamps, badlands, dark forest, desert; plus deep ocean floors.</li>
 *   <li><b>Epic</b> — mountains and peaks; plus the deepest, coldest ocean floors.</li>
 * </ul>
 * Common and above also generate on submerged ocean/river floors — the crate is the
 * lure, drowning the risk. Broken stays on dry land only.
 * The ~4x smaller-biome worldgen override makes these gated biomes reliably appear
 * inside the small world border.
 */
public class ModBiomeModifiers {

    public static final ResourceKey<BiomeModifier> ADD_BROKEN_CRATE = registerKey("add_broken_crate");
    public static final ResourceKey<BiomeModifier> ADD_COMMON_CRATE = registerKey("add_common_crate");
    public static final ResourceKey<BiomeModifier> ADD_RARE_CRATE   = registerKey("add_rare_crate");
    public static final ResourceKey<BiomeModifier> ADD_EPIC_CRATE   = registerKey("add_epic_crate");
    public static final ResourceKey<BiomeModifier> ADD_DECOR        = registerKey("add_decor");

    public static void bootstrap(BootstrapContext<BiomeModifier> context) {
        var placed = context.lookup(Registries.PLACED_FEATURE);
        var biomes = context.lookup(Registries.BIOME);

        // --- Gentle / common / easy-to-reach (Common) ---
        List<ResourceKey<Biome>> gentle = List.of(
                Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.FOREST, Biomes.BIRCH_FOREST,
                Biomes.OLD_GROWTH_BIRCH_FOREST, Biomes.FLOWER_FOREST, Biomes.MEADOW, Biomes.CHERRY_GROVE,
                Biomes.TAIGA, Biomes.SNOWY_TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA,
                Biomes.SNOWY_PLAINS, Biomes.SAVANNA, Biomes.BEACH, Biomes.SNOWY_BEACH,
                Biomes.STONY_SHORE, Biomes.MUSHROOM_FIELDS);

        // --- Dangerous surface biomes (Rare) ---
        List<ResourceKey<Biome>> dangerous = List.of(
                Biomes.DARK_FOREST, Biomes.SWAMP, Biomes.MANGROVE_SWAMP, Biomes.JUNGLE,
                Biomes.BAMBOO_JUNGLE, Biomes.SPARSE_JUNGLE, Biomes.BADLANDS, Biomes.WOODED_BADLANDS,
                Biomes.ERODED_BADLANDS, Biomes.WINDSWEPT_SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.DESERT,
                Biomes.ICE_SPIKES);

        // --- Mountains / peaks — hardest to reach (Epic) ---
        List<ResourceKey<Biome>> mountains = List.of(
                Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_FOREST,
                Biomes.SNOWY_SLOPES, Biomes.GROVE, Biomes.STONY_PEAKS, Biomes.JAGGED_PEAKS, Biomes.FROZEN_PEAKS);

        // --- Aquatic floors — crate sits on the seabed; the dive is the danger ---
        // Risk scales with depth: rivers/shallow oceans are an easy dip, deep oceans
        // a real drowning gamble. Broken stays on land (omitted here on purpose).
        List<ResourceKey<Biome>> shallowWater = List.of(
                Biomes.RIVER, Biomes.FROZEN_RIVER, Biomes.OCEAN, Biomes.WARM_OCEAN,
                Biomes.LUKEWARM_OCEAN, Biomes.COLD_OCEAN, Biomes.FROZEN_OCEAN);
        List<ResourceKey<Biome>> rareDeepWater  = List.of(Biomes.DEEP_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN);
        List<ResourceKey<Biome>> epicDeepWater  = List.of(Biomes.DEEP_COLD_OCEAN, Biomes.DEEP_FROZEN_OCEAN);

        // Common/Rare/Epic each pick up an aquatic floor set matched to their dive risk.
        List<ResourceKey<Biome>> commonAll =
                Stream.of(gentle, shallowWater).flatMap(List::stream).toList();
        List<ResourceKey<Biome>> rareAll =
                Stream.of(dangerous, rareDeepWater).flatMap(List::stream).toList();
        List<ResourceKey<Biome>> epicAll =
                Stream.of(mountains, epicDeepWater).flatMap(List::stream).toList();

        // Broken crates target the entire is_overworld biome tag, so EVERY overworld
        // biome is covered with zero gaps. The placement's water filter keeps them on
        // dry land, and surface placement never triggers inside cave biomes.
        register(context, ADD_BROKEN_CRATE, biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                placed.getOrThrow(ModPlacedFeatures.BROKEN_CRATE_PLACED_KEY));
        register(context, ADD_COMMON_CRATE, HolderSet.direct(biomes::getOrThrow, commonAll),
                placed.getOrThrow(ModPlacedFeatures.COMMON_CRATE_PLACED_KEY));
        register(context, ADD_RARE_CRATE, HolderSet.direct(biomes::getOrThrow, rareAll),
                placed.getOrThrow(ModPlacedFeatures.RARE_CRATE_PLACED_KEY));
        register(context, ADD_EPIC_CRATE, HolderSet.direct(biomes::getOrThrow, epicAll),
                placed.getOrThrow(ModPlacedFeatures.EPIC_CRATE_PLACED_KEY));

        // Decorative King's-realm piles: sprinkled across every overworld biome (dry land).
        context.register(ADD_DECOR, new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(
                        placed.getOrThrow(ModPlacedFeatures.DECOR_CAMP_PLACED_KEY),
                        placed.getOrThrow(ModPlacedFeatures.DECOR_BATTLE_PLACED_KEY),
                        placed.getOrThrow(ModPlacedFeatures.DECOR_GRAVE_PLACED_KEY),
                        placed.getOrThrow(ModPlacedFeatures.DECOR_RUINS_PLACED_KEY),
                        placed.getOrThrow(ModPlacedFeatures.KNIGHT_TENT_PLACED_KEY),
                        placed.getOrThrow(ModPlacedFeatures.BIG_TENT_PLACED_KEY),
                        placed.getOrThrow(ModPlacedFeatures.OUTPOST_PLACED_KEY),
                        placed.getOrThrow(ModPlacedFeatures.CRYPT_PLACED_KEY)),
                GenerationStep.Decoration.VEGETAL_DECORATION));
    }

    private static void register(BootstrapContext<BiomeModifier> context, ResourceKey<BiomeModifier> key,
                                 HolderSet<Biome> biomes, Holder<net.minecraft.world.level.levelgen.placement.PlacedFeature> feature) {
        context.register(key, new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes, HolderSet.direct(feature), GenerationStep.Decoration.VEGETAL_DECORATION));
    }

    private static ResourceKey<BiomeModifier> registerKey(String name) {
        return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ResourceLocation.fromNamespaceAndPath(KingSlayer.MOD_ID, name));
    }
}
