package com.koreykeipe.kingslayer.item;

import com.koreykeipe.kingslayer.util.ModTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ForgeTier;

public class ModToolTeirs {
    public static final Tier SLAYER = new ForgeTier(1500, 5,3f, 25,
            ModTags.Blocks.NEEDS_SLAYER_TOOL, () -> Ingredient.of(ModItems.CROWN.get()),
            ModTags.Blocks.INCORRECT_FOR_SLAYER_TOOL);
}
