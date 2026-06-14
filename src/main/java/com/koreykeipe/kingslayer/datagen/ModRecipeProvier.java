package com.koreykeipe.kingslayer.datagen;

import com.koreykeipe.kingslayer.KingSlayer;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvier extends RecipeProvider {

    public ModRecipeProvier(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {
        // Alternate TNT recipe — a cheap 2x2 shapeless of paper + gunpowder, a sink for the
        // paper/gunpowder that piles up from crates.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, Items.TNT)
                .requires(Items.GUNPOWDER, 2)
                .requires(Items.PAPER, 2)
                .unlockedBy("has_gunpowder", has(Items.GUNPOWDER))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(KingSlayer.MOD_ID, "tnt_from_paper_gunpowder"));
    }
}
