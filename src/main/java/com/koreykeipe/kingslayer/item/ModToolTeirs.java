package com.koreykeipe.kingslayer.item;

import com.koreykeipe.kingslayer.util.ModTags;
import net.neoforged.neoforge.common.SimpleTier;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

public class ModToolTeirs {
    // NeoForge/1.21 use vanilla SimpleTier: (incorrectBlocksForDrops, uses, speed, attack, enchantValue, repair).
    public static final Tier SLAYER = new SimpleTier(
            ModTags.Blocks.INCORRECT_FOR_SLAYER_TOOL, 1500, 5.0F, 3.0F, 25,
            () -> Ingredient.of(ModItems.CROWN.get()));
}
