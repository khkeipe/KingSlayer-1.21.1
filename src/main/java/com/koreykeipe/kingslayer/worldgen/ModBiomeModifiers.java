package com.koreykeipe.kingslayer.worldgen;

import com.koreykeipe.kingslayer.KingSlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ForgeBiomeModifiers;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.stream.Stream;

/**
 * Injects each crate tier's placed feature into the biomes that gate it.
 *
 * <p>All four tiers are surface features ({@link GenerationStep.Decoration#VEGETAL_DECORATION}).
 * Difficulty is expressed through biome eligibility:</p>
 * <ul>
 *   <li><b>Broken</b> — every gated biome (gentle, dangerous and mountains) so basic resources are always nearby.</li>
 *   <li><b>Common</b> — gentle, common, easy-to-reach biomes.</li>
 *   <li><b>Rare</b> — "dangerous" biomes: jungles, swamps, badlands, dark forest, desert.</li>
 *   <li><b>Epic</b> — mountains and peaks: the highest, hardest-to-reach terrain.</li>
 * </ul>
 * The ~4x smaller-biome worldgen override makes these gated biomes reliably appear
 * inside the small world border.
 */
public class ModBiomeModifiers {

    public static final ResourceKey<BiomeModifier> ADD_BROKEN_CRATE = registerKey("add_broken_crate");
    public static final ResourceKey<BiomeModifier> ADD_COMMON_CRATE = registerKey("add_common_crate");
    public static final ResourceKey<BiomeModifier> ADD_RARE_CRATE   = registerKey("add_rare_crate");
    public static final ResourceKey<BiomeModifier> ADD_EPIC_CRATE   = registerKey("add_epic_crate");

    public static void bootstrap(BootstrapContext<BiomeModifier> context) {
        var placed = context.lookup(Registries.PLACED_FEATURE);
        var biomes = context.lookup(Registries.BIOME);

        // --- Gentle / common / easy-to-reach (Common) ---
        List<ResourceKey<Biome>> gentle = List.of(
                Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.FOREST, Biomes.BIRCH_FOREST,
                Biomes.FLOWER_FOREST, Biomes.MEADOW, Biomes.TAIGA, Biomes.SNOWY_TAIGA,
                Biomes.SNOWY_PLAINS, Biomes.SAVANNA, Biomes.BEACH, Biomes.STONY_SHORE);

        // --- Dangerous surface biomes (Rare) ---
        List<ResourceKey<Biome>> dangerous = List.of(
                Biomes.DARK_FOREST, Biomes.SWAMP, Biomes.MANGROVE_SWAMP, Biomes.JUNGLE,
                Biomes.BAMBOO_JUNGLE, Biomes.SPARSE_JUNGLE, Biomes.BADLANDS, Biomes.WOODED_BADLANDS,
                Biomes.ERODED_BADLANDS, Biomes.WINDSWEPT_SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.DESERT);

        // --- Mountains / peaks — hardest to reach (Epic) ---
        List<ResourceKey<Biome>> mountains = List.of(
                Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_FOREST,
                Biomes.SNOWY_SLOPES, Biomes.GROVE, Biomes.STONY_PEAKS, Biomes.JAGGED_PEAKS, Biomes.FROZEN_PEAKS);

        // Broken crates are everywhere — gentle, dangerous AND mountains — so basic
        // resources are always close at hand no matter where a player lands.
        List<ResourceKey<Biome>> everywhere =
                Stream.of(gentle, dangerous, mountains).flatMap(List::stream).toList();

        HolderSet<Biome> gentleBiomes    = HolderSet.direct(biomes::getOrThrow, gentle);
        HolderSet<Biome> dangerousBiomes = HolderSet.direct(biomes::getOrThrow, dangerous);
        HolderSet<Biome> mountainBiomes  = HolderSet.direct(biomes::getOrThrow, mountains);
        HolderSet<Biome> allBiomes       = HolderSet.direct(biomes::getOrThrow, everywhere);

        register(context, ADD_BROKEN_CRATE, allBiomes,       placed.getOrThrow(ModPlacedFeatures.BROKEN_CRATE_PLACED_KEY));
        register(context, ADD_COMMON_CRATE, gentleBiomes,    placed.getOrThrow(ModPlacedFeatures.COMMON_CRATE_PLACED_KEY));
        register(context, ADD_RARE_CRATE,   dangerousBiomes, placed.getOrThrow(ModPlacedFeatures.RARE_CRATE_PLACED_KEY));
        register(context, ADD_EPIC_CRATE,   mountainBiomes,  placed.getOrThrow(ModPlacedFeatures.EPIC_CRATE_PLACED_KEY));
    }

    private static void register(BootstrapContext<BiomeModifier> context, ResourceKey<BiomeModifier> key,
                                 HolderSet<Biome> biomes, Holder<net.minecraft.world.level.levelgen.placement.PlacedFeature> feature) {
        context.register(key, new ForgeBiomeModifiers.AddFeaturesBiomeModifier(
                biomes, HolderSet.direct(feature), GenerationStep.Decoration.VEGETAL_DECORATION));
    }

    private static ResourceKey<BiomeModifier> registerKey(String name) {
        return ResourceKey.create(ForgeRegistries.Keys.BIOME_MODIFIERS, ResourceLocation.fromNamespaceAndPath(KingSlayer.MOD_ID, name));
    }
}
