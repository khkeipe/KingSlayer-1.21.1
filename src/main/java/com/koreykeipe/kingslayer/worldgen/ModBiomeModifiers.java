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

/**
 * Injects each crate tier's placed feature into the appropriate biomes.
 *
 * <p>Surface tiers (broken/common) go into a broad set of gentle, common biomes so
 * they reliably appear inside the small, spawn-centered world border. The deeper
 * tiers (rare/epic) are added to those same biomes <em>plus</em> the vertical cave
 * biomes — their {@link GenerationStep.Decoration#UNDERGROUND_DECORATION} placement
 * and Y-band do the real gating, so "go deeper for better loot" holds in any border.</p>
 */
public class ModBiomeModifiers {

    public static final ResourceKey<BiomeModifier> ADD_BROKEN_CRATE = registerKey("add_broken_crate");
    public static final ResourceKey<BiomeModifier> ADD_COMMON_CRATE = registerKey("add_common_crate");
    public static final ResourceKey<BiomeModifier> ADD_RARE_CRATE   = registerKey("add_rare_crate");
    public static final ResourceKey<BiomeModifier> ADD_EPIC_CRATE   = registerKey("add_epic_crate");

    public static void bootstrap(BootstrapContext<BiomeModifier> context) {
        var placed = context.lookup(Registries.PLACED_FEATURE);
        var biomes = context.lookup(Registries.BIOME);

        // Broad, common, generally-accessible surface biomes.
        Holder<Biome> plains       = biomes.getOrThrow(Biomes.PLAINS);
        Holder<Biome> forest       = biomes.getOrThrow(Biomes.FOREST);
        Holder<Biome> birch        = biomes.getOrThrow(Biomes.BIRCH_FOREST);
        Holder<Biome> darkForest   = biomes.getOrThrow(Biomes.DARK_FOREST);
        Holder<Biome> flowerForest = biomes.getOrThrow(Biomes.FLOWER_FOREST);
        Holder<Biome> meadow       = biomes.getOrThrow(Biomes.MEADOW);
        Holder<Biome> taiga        = biomes.getOrThrow(Biomes.TAIGA);
        Holder<Biome> snowyPlains  = biomes.getOrThrow(Biomes.SNOWY_PLAINS);
        Holder<Biome> savanna      = biomes.getOrThrow(Biomes.SAVANNA);
        Holder<Biome> swamp        = biomes.getOrThrow(Biomes.SWAMP);
        Holder<Biome> jungle       = biomes.getOrThrow(Biomes.JUNGLE);
        Holder<Biome> desert       = biomes.getOrThrow(Biomes.DESERT);
        Holder<Biome> stonyShore   = biomes.getOrThrow(Biomes.STONY_SHORE);

        // Vertical cave biomes — reliably present underground regardless of surface biome.
        Holder<Biome> dripstone    = biomes.getOrThrow(Biomes.DRIPSTONE_CAVES);
        Holder<Biome> lushCaves    = biomes.getOrThrow(Biomes.LUSH_CAVES);
        Holder<Biome> deepDark     = biomes.getOrThrow(Biomes.DEEP_DARK);

        HolderSet<Biome> surfaceBiomes = HolderSet.direct(
                plains, forest, birch, darkForest, flowerForest, meadow, taiga,
                snowyPlains, savanna, swamp, jungle, desert, stonyShore);

        HolderSet<Biome> rareBiomes = HolderSet.direct(
                plains, forest, birch, darkForest, flowerForest, meadow, taiga,
                snowyPlains, savanna, swamp, jungle, desert, stonyShore,
                dripstone, lushCaves);

        HolderSet<Biome> epicBiomes = HolderSet.direct(
                plains, forest, birch, darkForest, flowerForest, meadow, taiga,
                snowyPlains, savanna, swamp, jungle, desert, stonyShore,
                dripstone, lushCaves, deepDark);

        context.register(ADD_BROKEN_CRATE, new ForgeBiomeModifiers.AddFeaturesBiomeModifier(
                surfaceBiomes,
                HolderSet.direct(placed.getOrThrow(ModPlacedFeatures.BROKEN_CRATE_PLACED_KEY)),
                GenerationStep.Decoration.VEGETAL_DECORATION));

        context.register(ADD_COMMON_CRATE, new ForgeBiomeModifiers.AddFeaturesBiomeModifier(
                surfaceBiomes,
                HolderSet.direct(placed.getOrThrow(ModPlacedFeatures.COMMON_CRATE_PLACED_KEY)),
                GenerationStep.Decoration.VEGETAL_DECORATION));

        context.register(ADD_RARE_CRATE, new ForgeBiomeModifiers.AddFeaturesBiomeModifier(
                rareBiomes,
                HolderSet.direct(placed.getOrThrow(ModPlacedFeatures.RARE_CRATE_PLACED_KEY)),
                GenerationStep.Decoration.UNDERGROUND_DECORATION));

        context.register(ADD_EPIC_CRATE, new ForgeBiomeModifiers.AddFeaturesBiomeModifier(
                epicBiomes,
                HolderSet.direct(placed.getOrThrow(ModPlacedFeatures.EPIC_CRATE_PLACED_KEY)),
                GenerationStep.Decoration.UNDERGROUND_DECORATION));
    }

    private static ResourceKey<BiomeModifier> registerKey(String name) {
        return ResourceKey.create(ForgeRegistries.Keys.BIOME_MODIFIERS, ResourceLocation.fromNamespaceAndPath(KingSlayer.MOD_ID, name));
    }
}
