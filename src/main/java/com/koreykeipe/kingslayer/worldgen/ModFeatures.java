package com.koreykeipe.kingslayer.worldgen;

import com.koreykeipe.kingslayer.KingSlayer;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Registry for the mod's custom worldgen features.
 */
public class ModFeatures {

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, KingSlayer.MOD_ID);

    /** Guaranteed crate + themed debris pile. See {@link CrateDebrisFeature}. */
    public static final DeferredHolder<Feature<?>, Feature<CrateDebrisConfiguration>> CRATE_DEBRIS =
            FEATURES.register("crate_debris", () -> new CrateDebrisFeature(CrateDebrisConfiguration.CODEC));

    /** Stamps a saved NBT structure template. See {@link TemplateFeature}. */
    public static final DeferredHolder<Feature<?>, Feature<TemplateConfiguration>> TEMPLATE =
            FEATURES.register("template", () -> new TemplateFeature(TemplateConfiguration.CODEC));

    public static void register(IEventBus eventBus) {
        FEATURES.register(eventBus);
    }
}
