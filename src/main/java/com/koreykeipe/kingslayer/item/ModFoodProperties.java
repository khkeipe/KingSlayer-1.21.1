package com.koreykeipe.kingslayer.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;

public class ModFoodProperties {
    public static final FoodProperties CROWN = new FoodProperties.Builder()
            .fast()
            .alwaysEdible()
            .effect(new MobEffectInstance(MobEffects.GLOWING,500),1f)
            .build();
}
