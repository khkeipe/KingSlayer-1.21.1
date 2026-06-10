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
                        LootItemRandomChanceCondition.randomChance(.75f).build() }, ModItems.SLAYER_HOE.get()));
        this.add("slayer_shovel_from_broken_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.BROKEN_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.75f).build() }, ModItems.SLAYER_SHOVEL.get()));
        this.add("flint_from_broken_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.BROKEN_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.8f).build() }, Items.FLINT, 2, 5));
        this.add("feather_from_broken_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.BROKEN_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.5f).build() }, Items.FEATHER, 1, 5));
        this.add("carrot_from_broken_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.BROKEN_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.6f).build() }, Items.CARROT, 2, 5));
        this.add("potato_from_broken_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.BROKEN_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.6f).build() }, Items.POTATO, 2, 5));
        this.add("beat_from_broken_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.BROKEN_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.8f).build() }, Items.BEETROOT, 2, 5));
        this.add("apple_from_broken_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.BROKEN_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.2f).build() }, Items.APPLE, 1, 3));

        // Common Crate Loot
        this.add("slayer_pickaxe_from_common_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.COMMON_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.85f).build() }, ModItems.SLAYER_PICKAXE.get()));
        this.add("raw_iron_from_common_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.COMMON_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.6f).build() }, Items.RAW_IRON, 1, 3));
        this.add("raw_gold_from_common_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.COMMON_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.5f).build() }, Items.RAW_GOLD, 3, 8));
        this.add("leather_from_common_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.COMMON_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.6f).build() }, Items.LEATHER, 1, 3));
        this.add("bread_from_common_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.COMMON_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.5f).build() }, Items.BREAD, 2, 6));
        this.add("fish_from_common_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.COMMON_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.5f).build() }, Items.COD, 2, 6));
        this.add("stew_from_common_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.COMMON_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.6f).build() }, Items.MUSHROOM_STEW, 1, 3));
        this.add("paper_from_common_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.COMMON_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.5f).build() }, Items.PAPER, 10, 30));



        // Rare Crate Loot
        this.add("slayer_sword_from_rare_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.RARE_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.6f).build() }, ModItems.SLAYER_SWORD.get()));
        this.add("iron_from_rare_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.RARE_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.6f).build() }, Items.IRON_INGOT, 1, 5));
        this.add("beef_from_rare_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.RARE_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.6f).build() }, Items.BEEF, 1, 5));
        this.add("chicken_from_rare_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.RARE_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.6f).build() }, Items.CHICKEN, 1, 5));
        this.add("apple_from_rare_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.RARE_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.6f).build() }, Items.APPLE, 1, 5));
        this.add("gunpowder_rare_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.RARE_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.5f).build() }, Items.GUNPOWDER, 6, 12));


        // Epic Crate Loot

        this.add("slayer_axe_from_epic_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.EPIC_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.6f).build() }, ModItems.SLAYER_AXE.get()));
        this.add("cookies_from_epic_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.EPIC_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.8f).build() }, Items.COOKIE, 6, 16));
        this.add("cake_from_epic_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.EPIC_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.5f).build() }, Items.CAKE, 2, 4));
        this.add("pie_from_epic_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.EPIC_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.6f).build() }, Items.PUMPKIN_PIE, 3, 6));
        this.add("diamonds_from_epic_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.EPIC_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.5f).build() }, Items.DIAMOND, 1, 6));
        this.add("golden_apple_from_epic_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.EPIC_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.1f).build() }, Items.GOLDEN_APPLE, 1, 1));


        // Broken Airdrop Crate Loot
        // 3rd arg = minCount, 4th arg = maxCount — rolls a uniform random count in that range.
        // Omitting them (or using the 2-arg constructor) defaults to exactly 1.
        this.add("golden_apple_from_broken_airdop",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.BROKEN_AD_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(1f).build() }, Items.GOLDEN_APPLE, 0, 3));
        this.add("wind_from_broken_airdop",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.BROKEN_AD_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.6f).build() }, Items.WIND_CHARGE, 6, 16));

        // Common Airdrop Crate Loot
        this.add("golden_apple_from_common_airdop",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.COMMON_AD_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(1f).build() }, Items.GOLDEN_APPLE, 1, 2));
        this.add("pearl_from_common_airdop",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.COMMON_AD_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.6f).build() }, Items.ENDER_PEARL, 1, 3));
        this.add("trident_from_common_airdop",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.COMMON_AD_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.6f).build() }, Items.TRIDENT, 1, 1));

        // Rare Airdrop Crate Loot
        this.add("golden_apple_from_rare_airdop",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.RARE_AD_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(1f).build() }, Items.GOLDEN_APPLE, 1, 3));
        this.add("mace_from_epic_airdop",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.RARE_AD_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.6f).build() }, Items.MACE, 1, 1));

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

        // ------------------------------------------------------------------
        // Bounty Crate Loot — the special reward for slaying THE MARKED.
        // Handed straight to the killer's inventory; they place + break it to claim.
        // This is the richest crate in the game. Tweak chances / items / counts freely.
        // ------------------------------------------------------------------

        // Guaranteed sustenance package

        this.add("golden_apple_from_bounty",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.BOUNTY_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(1f).build() }, Items.GOLDEN_APPLE, 2, 4));
        this.add("golden_carrot_from_bounty",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.BOUNTY_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(1f).build() }, Items.GOLDEN_CARROT, 4, 8));

        // 85% — top-tier enchanted Slayer Sword
        this.add("slayer_sword_from_bounty",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.BOUNTY_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(0.85f).build()
                }, ModItems.SLAYER_SWORD.get(), 1, 1,
                        Map.of(
                                ResourceLocation.withDefaultNamespace("sharpness"), 4,
                                ResourceLocation.withDefaultNamespace("fire_aspect"), 2,
                                ResourceLocation.withDefaultNamespace("unbreaking"), 3
                        )));

        // 60% — enchanted Slayer Bat
        this.add("slayer_bat_from_bounty",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.BOUNTY_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(0.6f).build()
                }, ModItems.SLAYER_BAT.get(), 1, 1,
                        Map.of(
                                ResourceLocation.withDefaultNamespace("knockback"), 5,
                                ResourceLocation.withDefaultNamespace("unbreaking"), 3
                        )));

        // 70% — enchanted diamond chestplate
        this.add("diamond_chestplate_from_bounty",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.BOUNTY_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(0.7f).build()
                }, Items.DIAMOND_CHESTPLATE, 1, 1,
                        Map.of(
                                ResourceLocation.withDefaultNamespace("protection"), 4,
                                ResourceLocation.withDefaultNamespace("unbreaking"), 3
                        )));
    }
}
