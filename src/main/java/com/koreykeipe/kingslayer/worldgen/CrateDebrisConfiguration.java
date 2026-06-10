package com.koreykeipe.kingslayer.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Configuration for {@link CrateDebrisFeature}.
 *
 * @param crate  the crate block placed (guaranteed) at the center
 * @param debris weighted blocks scattered around it as a themed wreckage pile
 * @param radius how far the debris ring extends from the crate (in blocks)
 * @param chance per-cell probability that a debris block is placed
 */
public record CrateDebrisConfiguration(BlockState crate, BlockStateProvider debris, int radius, float chance)
        implements FeatureConfiguration {

    public static final Codec<CrateDebrisConfiguration> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            BlockState.CODEC.fieldOf("crate").forGetter(CrateDebrisConfiguration::crate),
            BlockStateProvider.CODEC.fieldOf("debris").forGetter(CrateDebrisConfiguration::debris),
            Codec.intRange(0, 8).fieldOf("radius").forGetter(CrateDebrisConfiguration::radius),
            Codec.floatRange(0f, 1f).fieldOf("chance").forGetter(CrateDebrisConfiguration::chance)
    ).apply(inst, CrateDebrisConfiguration::new));
}
