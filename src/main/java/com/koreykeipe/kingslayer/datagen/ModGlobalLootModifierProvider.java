package com.koreykeipe.kingslayer.datagen;

import com.koreykeipe.kingslayer.KingSlayer;
import com.koreykeipe.kingslayer.block.ModBlocks;
import com.koreykeipe.kingslayer.item.ModItems;
import com.koreykeipe.kingslayer.loot.AddItemModifier;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraftforge.common.data.GlobalLootModifierProvider;
import net.minecraftforge.common.loot.LootTableIdCondition;

import java.util.concurrent.CompletableFuture;

public class ModGlobalLootModifierProvider extends GlobalLootModifierProvider {
    public ModGlobalLootModifierProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, KingSlayer.MOD_ID, registries);
    }

    @Override
    protected void start(HolderLookup.Provider registries) {

        // Broken Crate Loot
        this.add("slayer_hoe_from_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.BROKEN_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.5f).build() }, ModItems.SLAYER_HOE.get()));
        this.add("slayer_shovel_from_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.BROKEN_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.5f).build() }, ModItems.SLAYER_SHOVEL.get()));

        // Common Crate Loot
        this.add("slayer_pickaxe_from_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.COMMON_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.5f).build() }, ModItems.SLAYER_PICKAXE.get()));


        // Rare Crate Loot
        this.add("slayer_axe_from_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.RARE_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.2f).build() }, ModItems.SLAYER_AXE.get()));


        // Epic Crate Loot
        this.add("slayer_sword_from_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.EPIC_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.2f).build() }, ModItems.SLAYER_SWORD.get()));

        // Broken Airdrop Crate Loot
        // 3rd arg = minCount, 4th arg = maxCount — rolls a uniform random count in that range.
        // Omitting them (or using the 2-arg constructor) defaults to exactly 1.
        this.add("golden_apple_from_broken_airdop",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.BROKEN_AD_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(1f).build() }, Items.GOLDEN_APPLE, 1, 3));
        this.add("golden_apple_from_common_airdop",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.COMMON_AD_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(1f).build() }, Items.GOLDEN_APPLE, 3, 4));
        this.add("golden_apple_from_rare_airdop",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.RARE_AD_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(1f).build() }, Items.ENCHANTED_GOLDEN_APPLE, 1, 2));
        this.add("golden_apple_from_epic_airdop",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.EPIC_AD_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(1f).build() }, Items.ENCHANTED_GOLDEN_APPLE, 3, 5));

        add("crown_from_zombie", new AddItemModifier(new LootItemCondition[] {
                new LootTableIdCondition.Builder(ResourceLocation.withDefaultNamespace("entities/zombie"))
                        .and(LootItemRandomChanceCondition.randomChance(0.8f)).build() }, // modified by the creeper's own loot table
                ModItems.CROWN.get()));
    }
}
