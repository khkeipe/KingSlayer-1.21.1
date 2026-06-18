package com.koreykeipe.kingslayer.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Configuration for {@link TemplateFeature}.
 *
 * @param template  id of the NBT template to stamp, loaded from
 *                  {@code data/<namespace>/structure/<path>.nbt} (note: singular "structure" in 1.21)
 * @param integrity 1.0 = placed intact; below 1.0 randomly omits that fraction of blocks
 *                  for a weathered / battle-damaged look
 * @param level     true = carve out the terrain inside the structure's footprint and fill
 *                  underneath down to solid ground, so the build sits flush instead of
 *                  floating off a slope or being half-swallowed by a hill. Set false for
 *                  builds meant to nestle into the terrain (e.g. half-buried ruins, crypts).
 * @param buryDepth blocks to sink the stamp below the surface. 0 = bottom sits on the surface
 *                  (build grows upward — tents/towers). A positive value lowers the whole build
 *                  so its bulk is underground and only the topmost layers pierce the surface —
 *                  set it to the build's "ground layer" height for a buried crypt. Pair with
 *                  {@code level=false} so the hill above is preserved rather than carved away.
 * @param maxSlope  max allowed height variation (in blocks) of the natural surface across the
 *                  build's footprint. 0 = no check. If the terrain is steeper than this the
 *                  feature skips that spot entirely — keeps builds off cliffs where a flat shell
 *                  would jut out into open air. Smaller = flatter ground required (and rarer).
 */
public record TemplateConfiguration(ResourceLocation template, float integrity, boolean level, int buryDepth, int maxSlope) implements FeatureConfiguration {

    /** Convenience: intact-and-levelled is the common case for hand-built tents/outposts. */
    public TemplateConfiguration(ResourceLocation template, float integrity) {
        this(template, integrity, true, 0, 0);
    }

    /** Convenience: pick the leveling mode but keep the build on the surface (no burying). */
    public TemplateConfiguration(ResourceLocation template, float integrity, boolean level) {
        this(template, integrity, level, 0, 0);
    }

    /** Convenience: leveling mode + bury depth, no slope gate. */
    public TemplateConfiguration(ResourceLocation template, float integrity, boolean level, int buryDepth) {
        this(template, integrity, level, buryDepth, 0);
    }

    public static final Codec<TemplateConfiguration> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ResourceLocation.CODEC.fieldOf("template").forGetter(TemplateConfiguration::template),
            Codec.floatRange(0f, 1f).optionalFieldOf("integrity", 1.0f).forGetter(TemplateConfiguration::integrity),
            Codec.BOOL.optionalFieldOf("level", true).forGetter(TemplateConfiguration::level),
            Codec.intRange(0, 64).optionalFieldOf("bury_depth", 0).forGetter(TemplateConfiguration::buryDepth),
            Codec.intRange(0, 64).optionalFieldOf("max_slope", 0).forGetter(TemplateConfiguration::maxSlope)
    ).apply(inst, TemplateConfiguration::new));
}
