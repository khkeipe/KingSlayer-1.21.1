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
 */
public record TemplateConfiguration(ResourceLocation template, float integrity) implements FeatureConfiguration {

    public static final Codec<TemplateConfiguration> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ResourceLocation.CODEC.fieldOf("template").forGetter(TemplateConfiguration::template),
            Codec.floatRange(0f, 1f).optionalFieldOf("integrity", 1.0f).forGetter(TemplateConfiguration::integrity)
    ).apply(inst, TemplateConfiguration::new));
}
