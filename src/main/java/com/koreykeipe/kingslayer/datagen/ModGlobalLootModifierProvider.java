package com.koreykeipe.kingslayer.datagen;

import com.koreykeipe.kingslayer.KingSlayer;
import com.koreykeipe.kingslayer.block.ModBlocks;
import com.koreykeipe.kingslayer.item.ModItems;
import com.koreykeipe.kingslayer.loot.AddItemModifier;
import com.koreykeipe.kingslayer.loot.OneOfItemModifier;
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
        oneOf("broken_ad_special", ModBlocks.BROKEN_CRATE.get(), 0.75f,
                new OneOfItemModifier.Choice(ModItems.SLAYER_SHOVEL.get(), 1),
                new OneOfItemModifier.Choice(ModItems.SLAYER_HOE.get(),1));

        this.add("flint_from_broken_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.BROKEN_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.8f).build() }, Items.FLINT, 2, 5));
        this.add("feather_from_broken_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.BROKEN_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.5f).build() }, Items.FEATHER, 1, 5));
        oneOf("food_from_broken_crate",ModBlocks.BROKEN_CRATE.get(),.8f,
                new OneOfItemModifier.Choice(Items.CARROT, 1, 2, 5),
                new OneOfItemModifier.Choice(Items.POTATO, 1, 2, 5),
                new OneOfItemModifier.Choice(Items.BEETROOT, 2, 2, 5),
                new OneOfItemModifier.Choice(Items.APPLE, 1, 1, 3));

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
        this.add("paper_from_common_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.COMMON_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.5f).build() }, Items.PAPER, 10, 30));

        oneOf("food_from_common_crate",ModBlocks.COMMON_CRATE.get(),.8f,
                new OneOfItemModifier.Choice(Items.BREAD, 1, 3, 6),
                new OneOfItemModifier.Choice(Items.COD, 2, 2, 5),
                new OneOfItemModifier.Choice(Items.MUSHROOM_STEW, 1, 1, 3),
                new OneOfItemModifier.Choice(Items.SUSPICIOUS_STEW, 1, 1, 1));

        // Rare Crate Loot
        this.add("slayer_sword_from_rare_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.RARE_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.6f).build() }, ModItems.SLAYER_SWORD.get()));
        this.add("iron_from_rare_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.RARE_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.6f).build() }, Items.IRON_INGOT, 1, 5));
        this.add("gunpowder_rare_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.RARE_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.5f).build() }, Items.GUNPOWDER, 6, 12));

        oneOf("food_from_rare_crate",ModBlocks.RARE_CRATE.get(),.8f,
                new OneOfItemModifier.Choice(Items.BEEF, 1, 2, 6),
                new OneOfItemModifier.Choice(Items.CHICKEN, 2, 2, 5),
                new OneOfItemModifier.Choice(Items.APPLE, 1, 1, 3),
                new OneOfItemModifier.Choice(Items.PORKCHOP, 1, 1, 4));

        // Epic Crate Loot

        this.add("slayer_axe_from_epic_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.EPIC_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.6f).build() }, ModItems.SLAYER_AXE.get()));

        oneOf("food_from_epic_crate",ModBlocks.EPIC_CRATE.get(),.8f,
                new OneOfItemModifier.Choice(Items.COOKIE, 2, 6, 16),
                new OneOfItemModifier.Choice(Items.CAKE, 1, 1, 2),
                new OneOfItemModifier.Choice(Items.PUMPKIN_PIE, 1, 2, 6),
                new OneOfItemModifier.Choice(Items.SWEET_BERRIES, 1, 1, 5));

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
        // One bonus item from the group (add more Choices here later).
        oneOf("broken_ad_special", ModBlocks.BROKEN_AD_CRATE.get(), 0.8f,
                new OneOfItemModifier.Choice(Items.WIND_CHARGE, 2, 6, 16),
                new OneOfItemModifier.Choice(Items.FIRE_CHARGE, 1, 6, 10),
                new OneOfItemModifier.Choice(Items.ENDER_PEARL, 1,1,3));

        // Common Airdrop Crate Loot
        this.add("golden_apple_from_common_airdop",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.COMMON_AD_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(1f).build() }, Items.GOLDEN_APPLE, 1, 2));
        // One special item from the group (pearl OR trident). Add more Choices freely.
        oneOf("common_ad_special", ModBlocks.COMMON_AD_CRATE.get(), 0.6f,
                new OneOfItemModifier.Choice(Items.ENDER_PEARL, 2, 1, 3),
                new OneOfItemModifier.Choice(Items.TRIDENT, 1,
                        Map.of(ResourceLocation.withDefaultNamespace("loyalty"), 2)));

        // Rare Airdrop Crate Loot
        this.add("golden_apple_from_rare_airdop",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.RARE_AD_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(1f).build() }, Items.GOLDEN_APPLE, 1, 3));
        // One special item from the group (add more Choices here later).
        oneOf("rare_ad_special", ModBlocks.RARE_AD_CRATE.get(), 0.6f,
                new OneOfItemModifier.Choice(Items.MACE, 1));

        //Epic Airdrop Crate Loot
        this.add("golden_apple_from_epic_airdop",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.EPIC_AD_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(1f).build() }, Items.GOLDEN_APPLE, 2, 5));

        // ONE enchanted reward from the group (Slayer Bat OR shield). Add more Choices freely.
        oneOf("epic_ad_special", ModBlocks.EPIC_AD_CRATE.get(), 0.9f,
                new OneOfItemModifier.Choice(ModItems.SLAYER_BAT.get(), 2,
                        Map.of(ResourceLocation.withDefaultNamespace("knockback"), 4)),
                new OneOfItemModifier.Choice(Items.SHIELD, 2,
                        Map.of(ResourceLocation.withDefaultNamespace("knockback_resistance"), 4)));

        // ------------------------------------------------------------------
        // Bounty Crate Loot — the special reward for slaying THE MARKED.
        // Handed straight to the killer's inventory; they place + break it to claim.
        // This is the richest crate in the game. Tweak chances / items / counts freely.
        // ------------------------------------------------------------------
        this.add("golden_apple_from_bounty",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.BOUNTY_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(1f).build() }, Items.GOLDEN_APPLE, 2, 4));
        this.add("golden_carrot_from_bounty",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.BOUNTY_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(1f).build() }, Items.GOLDEN_CARROT, 4, 8));

        // ONE top-tier enchanted reward (not all three). Weighted; add more Choices freely.
        oneOf("bounty_reward", ModBlocks.BOUNTY_CRATE.get(), 0.95f,
                new OneOfItemModifier.Choice(ModItems.SLAYER_SWORD.get(), 2,
                        Map.of(ResourceLocation.withDefaultNamespace("sharpness"), 2,
                               ResourceLocation.withDefaultNamespace("fire_aspect"), 2,
                               ResourceLocation.withDefaultNamespace("unbreaking"), 3)),
                new OneOfItemModifier.Choice(ModItems.SLAYER_BAT.get(), 1,
                        Map.of(ResourceLocation.withDefaultNamespace("knockback"), 5,
                               ResourceLocation.withDefaultNamespace("unbreaking"), 3)),
                new OneOfItemModifier.Choice(Items.DIAMOND_CHESTPLATE, 2,
                        Map.of(ResourceLocation.withDefaultNamespace("protection"), 1,
                               ResourceLocation.withDefaultNamespace("unbreaking"), 3)));

        // ------------------------------------------------------------------
        // Mystery Crate Loot — a curatable grab-bag (bought as a "box" at the Tribute
        // Stone). Mix in hostile spawn eggs, rare custom weapons, and oddities freely.
        // ------------------------------------------------------------------
        // One random hostile egg (not all of them). Add more egg Choices freely.
        oneOf("mystery_egg", ModBlocks.MYSTERY_CRATE.get(), 0.6f,
                new OneOfItemModifier.Choice(Items.ZOMBIE_SPAWN_EGG, 3),
                new OneOfItemModifier.Choice(Items.SKELETON_SPAWN_EGG, 3),
                new OneOfItemModifier.Choice(Items.CREEPER_SPAWN_EGG, 2),
                new OneOfItemModifier.Choice(Items.SPIDER_SPAWN_EGG, 2));
        mysteryCrate("mystery_ender_pearls", Items.ENDER_PEARL,        0.40f, 1, 3);
        mysteryCrate("mystery_xp",           Items.EXPERIENCE_BOTTLE,  0.50f, 2, 6);
        mysteryCrate("mystery_gapple",       Items.GOLDEN_APPLE,       0.25f, 1, 1);
        mysteryCrate("mystery_crown",       ModItems.CROWN.get(),       0.1f, 1, 1);

        // Rare weapon — ONE of the group (not all). ~25% chance, weighted sword:bat 2:1.
        // Add future custom weapons here as extra Choices and they join the same single roll.
        oneOf("mystery_rare_weapon", ModBlocks.MYSTERY_CRATE.get(), 0.25f,
                new OneOfItemModifier.Choice(ModItems.SLAYER_SWORD.get(), 2),
                new OneOfItemModifier.Choice(ModItems.SLAYER_BAT.get(), 1));

        // ------------------------------------------------------------------
        // Crown Fragments from crates — amount climbs with rarity.
        // ------------------------------------------------------------------
        crateFragments("crown_fragment_from_broken_crate", ModBlocks.BROKEN_CRATE.get(), 0.8f, 1, 2);
        crateFragments("crown_fragment_from_common_crate", ModBlocks.COMMON_CRATE.get(), 0.9f, 2, 3);
        crateFragments("crown_fragment_from_rare_crate",   ModBlocks.RARE_CRATE.get(),   1.0f, 3, 5);
        crateFragments("crown_fragment_from_epic_crate",   ModBlocks.EPIC_CRATE.get(),   1.0f, 5, 8);
        crateFragments("crown_fragment_from_broken_ad",    ModBlocks.BROKEN_AD_CRATE.get(), 1.0f, 1, 3);
        crateFragments("crown_fragment_from_common_ad",    ModBlocks.COMMON_AD_CRATE.get(), 1.0f, 2, 4);
        crateFragments("crown_fragment_from_rare_ad",      ModBlocks.RARE_AD_CRATE.get(),   1.0f, 4, 6);
        crateFragments("crown_fragment_from_epic_ad",      ModBlocks.EPIC_AD_CRATE.get(),   1.0f, 6, 10);
        crateFragments("crown_fragment_from_bounty_crate", ModBlocks.BOUNTY_CRATE.get(),    1.0f, 8, 12);

        // ------------------------------------------------------------------
        // Crown Fragments — renewable Tribute Stone currency. Common hostiles drop
        // them nearly every kill so the exchange economy keeps flowing.
        // ------------------------------------------------------------------
        for (String mob : new String[] {
                "zombie", "zombie_villager", "husk", "drowned",
                "skeleton", "stray", "bogged",
                "creeper", "spider", "cave_spider",
                "witch", "pillager", "vindicator", "phantom", "slime" }) {
            fragmentDrop(mob, 0.9f, 1, 2);
        }
    }

    /** Adds a Crown Fragment drop to a crate block's loot. */
    private void crateFragments(String name, net.minecraft.world.level.block.Block crate, float chance, int min, int max) {
        this.add(name, new AddItemModifier(new LootItemCondition[] {
                LootItemBlockStatePropertyCondition.hasBlockStateProperties(crate).build(),
                LootItemRandomChanceCondition.randomChance(chance).build() },
                ModItems.CROWN_FRAGMENT.get(), min, max));
    }

    /** Adds an item drop to the Mystery Crate's loot. */
    private void mysteryCrate(String name, net.minecraft.world.item.Item item, float chance, int min, int max) {
        this.add(name, new AddItemModifier(new LootItemCondition[] {
                LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.MYSTERY_CRATE.get()).build(),
                LootItemRandomChanceCondition.randomChance(chance).build() },
                item, min, max));
    }

    /** Adds a modifier that drops exactly ONE of the given choices (weighted) when {@code chance} passes. */
    private void oneOf(String name, net.minecraft.world.level.block.Block crate, float chance,
                       OneOfItemModifier.Choice... choices) {
        this.add(name, new OneOfItemModifier(new LootItemCondition[] {
                LootItemBlockStatePropertyCondition.hasBlockStateProperties(crate).build(),
                LootItemRandomChanceCondition.randomChance(chance).build() },
                java.util.List.of(choices)));
    }

    /** Adds a global loot modifier dropping Crown Fragments from the given vanilla entity. */
    private void fragmentDrop(String entityPath, float chance, int min, int max) {
        this.add("crown_fragment_from_" + entityPath,
                new AddItemModifier(new LootItemCondition[] {
                        new LootTableIdCondition.Builder(
                                ResourceLocation.withDefaultNamespace("entities/" + entityPath))
                                .and(LootItemRandomChanceCondition.randomChance(chance)).build() },
                        ModItems.CROWN_FRAGMENT.get(), min, max));
    }
}
