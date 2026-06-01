package com.koreykeipe.kingslayer.damage;

import com.koreykeipe.kingslayer.KingSlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

/**
 * Central registry of custom damage type keys for the KingSlayer mod.
 *
 * <p>Each key here MUST have a matching JSON file at
 * {@code data/kcs_kingslayer/damage_type/<id>.json}.
 * The death message translation key is {@code death.attack.<message_id>}
 * where {@code message_id} comes from the JSON, not the resource location.</p>
 */
public final class ModDamageTypes {

    /**
     * Nether gas — applied to players every few seconds while inside the Nether.
     * Death message key: {@code death.attack.nether_gas} in {@code en_us.json}.
     */
    public static final ResourceKey<DamageType> NETHER_GAS = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(KingSlayer.MOD_ID, "nether_gas")
    );

    private ModDamageTypes() {}
}
