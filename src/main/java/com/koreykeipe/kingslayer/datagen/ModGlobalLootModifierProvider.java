package com.koreykeipe.kingslayer.datagen;

import com.koreykeipe.kingslayer.KingSlayer;
import com.koreykeipe.kingslayer.block.ModBlocks;
import com.koreykeipe.kingslayer.item.ModItems;
import com.koreykeipe.kingslayer.loot.AddItemModifier;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraftforge.common.data.GlobalLootModifierProvider;
import net.minecraftforge.common.loot.LootTableIdCondition;

import java.util.Map;
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
                        LootItemRandomChanceCondition.randomChance(1f).build() }, Items.GOLDEN_APPLE, 0, 3));

        // Common Airdrop Crate Loot
        this.add("golden_apple_from_common_airdop",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.COMMON_AD_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(1f).build() }, Items.GOLDEN_APPLE, 1, 2));

        // Rare Airdrop Crate Loot
        this.add("golden_apple_from_rare_airdop",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.RARE_AD_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(1f).build() }, Items.GOLDEN_APPLE, 1, 3));

        //Epic Airdrop Crate Loot
        this.add("golden_apple_from_epic_airdop",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.EPIC_AD_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(1f).build() }, Items.GOLDEN_APPLE, 2, 5));

        // 30 % chance to get a Slayer Bat with enchantments from an epic crate.
        // The enchantments map uses vanilla resource locations — swap in your mod's ID for custom enchantments.
        this.add("slayer_bat_from_epic_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.EPIC_AD_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(0.8f).build()
                }, ModItems.SLAYER_BAT.get(), 1, 1,
                        Map.of(
                                ResourceLocation.withDefaultNamespace("knockback"), 4
                        )));
        this.add("slayer_shield_from_epic_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.EPIC_AD_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(1f).build()
                }, Items.SHIELD, 1, 1,
                        Map.of(
                                ResourceLocation.withDefaultNamespace("knockback_resistance"), 4
                        )));

        add("crown_from_zombie", new AddItemModifier(new LootItemCondition[] {
                new LootTableIdCondition.Builder(ResourceLocation.withDefaultNamespace("entities/zombie"))
                        .and(LootItemRandomChanceCondition.randomChance(0.8f)).build() }, // modified by the creeper's own loot table
                ModItems.CROWN.get()));
    }
}
