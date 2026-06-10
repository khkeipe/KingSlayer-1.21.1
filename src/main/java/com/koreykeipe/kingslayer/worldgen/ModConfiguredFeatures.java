package com.koreykeipe.kingslayer.worldgen;

import com.koreykeipe.kingslayer.KingSlayer;
import com.koreykeipe.kingslayer.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.*;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;

/**
 * One configured feature per crate tier. The feature only describes <em>what</em>
 * to place; {@link ModPlacedFeatures} decides <em>where</em> (rarity, depth, water,
 * biome) and {@link ModBiomeModifiers} decides which biomes receive it.
 *
 * <p>Design: the Broken tier uses a {@link Feature#BLOCK_PILE} "crashed supply"
 * wreckage mound (crate buried in debris), while the more valuable tiers place a
 * single clean crate via {@link Feature#SIMPLE_BLOCK} so the prize is always
 * actually present and findable.</p>
 */
public class ModConfiguredFeatures {

    public static final ResourceKey<ConfiguredFeature<?,?>> BROKEN_CRATE_KEY = registerKey("broken_crate");
    public static final ResourceKey<ConfiguredFeature<?,?>> COMMON_CRATE_KEY = registerKey("common_crate");
    public static final ResourceKey<ConfiguredFeature<?,?>> RARE_CRATE_KEY   = registerKey("rare_crate");
    public static final ResourceKey<ConfiguredFeature<?,?>> EPIC_CRATE_KEY   = registerKey("epic_crate");

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {

        // BROKEN — a "crashed supply" wreckage pile: the crate mixed into debris.
        // Weights are per-block within the pile; the crate is the most likely block
        // so a pile almost always contains at least one.
        register(context, BROKEN_CRATE_KEY, Feature.BLOCK_PILE,
                new BlockPileConfiguration(
                        new WeightedStateProvider(
                                SimpleWeightedRandomList.<BlockState>builder()
                                        .add(ModBlocks.BROKEN_CRATE.get().defaultBlockState(), 3)
                                        .add(Blocks.GRAVEL.defaultBlockState(), 2)
                                        .add(Blocks.COARSE_DIRT.defaultBlockState(), 2)
                                        .add(Blocks.STONE.defaultBlockState(), 3)
                        )));

        // COMMON / RARE / EPIC — a single clean crate block; placement does the rest.
        register(context, COMMON_CRATE_KEY, Feature.SIMPLE_BLOCK,
                new SimpleBlockConfiguration(
                        BlockStateProvider.simple(ModBlocks.COMMON_CRATE.get().defaultBlockState())));

        register(context, RARE_CRATE_KEY, Feature.SIMPLE_BLOCK,
                new SimpleBlockConfiguration(
                        BlockStateProvider.simple(ModBlocks.RARE_CRATE.get().defaultBlockState())));

        register(context, EPIC_CRATE_KEY, Feature.SIMPLE_BLOCK,
                new SimpleBlockConfiguration(
                        BlockStateProvider.simple(ModBlocks.EPIC_CRATE.get().defaultBlockState())));
    }

    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(KingSlayer.MOD_ID, name));
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(BootstrapContext<ConfiguredFeature<?, ?>> context,
                                                                                          ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }
}
