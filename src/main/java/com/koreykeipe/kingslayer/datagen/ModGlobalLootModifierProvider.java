package com.koreykeipe.kingslayer.datagen;

import com.koreykeipe.kingslayer.KingSlayer;
import com.koreykeipe.kingslayer.block.ModBlocks;
import com.koreykeipe.kingslayer.item.ModItems;
import com.koreykeipe.kingslayer.loot.AddItemModifier;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
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

        this.add("slayer_pickaxe_from_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.COMMON_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.5f).build() }, ModItems.SLAYER_PICKAXE.get()));
        this.add("slayer_axe_from_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.RARE_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.2f).build() }, ModItems.SLAYER_AXE.get()));
        this.add("slayer_shovel_from_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.BROKEN_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.5f).build() }, ModItems.SLAYER_SHOVEL.get()));
        this.add("slayer_sword_from_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.EPIC_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.2f).build() }, ModItems.SLAYER_SWORD.get()));
        this.add("slayer_hoe_from_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.BROKEN_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.5f).build() }, ModItems.SLAYER_HOE.get()));

        add("crown_from_zombie", new AddItemModifier(new LootItemCondition[] {
                new LootTableIdCondition.Builder(ResourceLocation.withDefaultNamespace("entities/zombie"))
                        .and(LootItemRandomChanceCondition.randomChance(0.8f)).build() }, // modified by the creeper's own loot table
                ModItems.CROWN.get()));
    }
}
