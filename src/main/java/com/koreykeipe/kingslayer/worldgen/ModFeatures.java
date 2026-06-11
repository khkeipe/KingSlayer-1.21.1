package com.koreykeipe.kingslayer.worldgen;

import com.koreykeipe.kingslayer.KingSlayer;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for the mod's custom worldgen features.
 */
public class ModFeatures {

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(ForgeRegistries.FEATURES, KingSlayer.MOD_ID);

    /** Guaranteed crate + themed debris pile. See {@link CrateDebrisFeature}. */
    public static final RegistryObject<Feature<CrateDebrisConfiguration>> CRATE_DEBRIS =
            FEATURES.register("crate_debris", () -> new CrateDebrisFeature(CrateDebrisConfiguration.CODEC));

    /** Stamps a saved NBT structure template. See {@link TemplateFeature}. */
    public static final RegistryObject<Feature<TemplateConfiguration>> TEMPLATE =
            FEATURES.register("template", () -> new TemplateFeature(TemplateConfiguration.CODEC));

    public static void register(IEventBus eventBus) {
        FEATURES.register(eventBus);
    }
}
