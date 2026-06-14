package com.koreykeipe.kingslayer.item;

import com.koreykeipe.kingslayer.KingSlayer;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.List;

/**
 * Custom armor material for the wearable combat gear. Netherite-level protection and the
 * vanilla netherite textures, but deliberately <b>no toughness and no knockback resistance</b>
 * — those perks are either unwanted on these pieces or supplied elsewhere (the Anchor Greaves
 * get full knockback immunity from {@link com.koreykeipe.kingslayer.event.ArmorPerkHandler}).
 */
public class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, KingSlayer.MOD_ID);

    public static final RegistryObject<ArmorMaterial> SLAYER_PLATE = ARMOR_MATERIALS.register("slayer_plate",
            () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                        map.put(ArmorItem.Type.BOOTS, 3);
                        map.put(ArmorItem.Type.LEGGINGS, 6);
                        map.put(ArmorItem.Type.CHESTPLATE, 8);
                        map.put(ArmorItem.Type.HELMET, 3);
                        map.put(ArmorItem.Type.BODY, 11);
                    }),
                    15,                                 // enchantment value (netherite-tier)
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(Items.NETHERITE_INGOT),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.withDefaultNamespace("netherite"))),
                    0.0F,                               // toughness — removed
                    0.0F                                // knockback resistance — removed
            ));

    public static void register(IEventBus eventBus) {
        ARMOR_MATERIALS.register(eventBus);
    }
}
