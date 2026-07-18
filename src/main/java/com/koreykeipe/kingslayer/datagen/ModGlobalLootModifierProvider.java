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
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ModGlobalLootModifierProvider extends GlobalLootModifierProvider {
    public ModGlobalLootModifierProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, KingSlayer.MOD_ID);
    }

    @Override
    protected void start() {

        // Broken Crate Loot
        oneOf("broken_special", ModBlocks.BROKEN_CRATE.get(), 0.4f,
                new OneOfItemModifier.Choice(ModItems.SLAYER_SHOVEL.get(), 1),
                new OneOfItemModifier.Choice(ModItems.SLAYER_HOE.get(),1));

        this.add("flint_from_broken_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.BROKEN_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.6f).build() }, Items.FLINT, 2, 5));
        this.add("feather_from_broken_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.BROKEN_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.5f).build() }, Items.FEATHER, 1, 5));
        oneOf("food_from_broken_crate",ModBlocks.BROKEN_CRATE.get(),.6f,
                new OneOfItemModifier.Choice(Items.CARROT, 1, 2, 5),
                new OneOfItemModifier.Choice(Items.POTATO, 1, 2, 5),
                new OneOfItemModifier.Choice(Items.BEETROOT, 2, 2, 5),
                new OneOfItemModifier.Choice(Items.APPLE, 1, 1, 3));

        // Common Crate Loot
        this.add("slayer_pickaxe_from_common_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.COMMON_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.5f).build() }, ModItems.SLAYER_PICKAXE.get()));
        this.add("raw_iron_from_common_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.COMMON_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.4f).build() }, Items.RAW_IRON, 1, 3));
        this.add("raw_gold_from_common_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.COMMON_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.4f).build() }, Items.RAW_GOLD, 3, 8));
        this.add("leather_from_common_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.COMMON_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.5f).build() }, Items.LEATHER, 1, 3));
        this.add("paper_from_common_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.COMMON_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.5f).build() }, Items.PAPER, 2, 16));
        this.add("compass_common_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.COMMON_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.2f).build() }, Items.COMPASS));

        oneOf("food_from_common_crate",ModBlocks.COMMON_CRATE.get(),.6f,
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
                        LootItemRandomChanceCondition.randomChance(.5f).build() }, Items.IRON_INGOT, 1, 5));
        this.add("gunpowder_rare_crate",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.RARE_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(.5f).build() }, Items.GUNPOWDER, 6, 12));

        oneOf("food_from_rare_crate",ModBlocks.RARE_CRATE.get(),.6f,
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
        // Combat: the entry-level counter. A couple of Bolas to root runners.
        crateItem("bola_from_broken_ad", ModBlocks.BROKEN_AD_CRATE.get(), 0.35f,
                ModItems.BOLA.get(), 1, 2);

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
        // Combat: a control weapon (one of the group) + circulating counters.
        oneOf("common_ad_combat", ModBlocks.COMMON_AD_CRATE.get(), 0.35f,
                new OneOfItemModifier.Choice(ModItems.YOINK_ROD.get(), 1));
        crateItem("bola_from_common_ad",   ModBlocks.COMMON_AD_CRATE.get(), 0.40f, ModItems.BOLA.get(), 1, 3);
        crateItem("anchor_from_common_ad", ModBlocks.COMMON_AD_CRATE.get(), 0.30f, ModItems.ANCHOR_CHARM.get(), 1, 1);
        crateItem("truesight_from_common_ad", ModBlocks.COMMON_AD_CRATE.get(), 0.20f, ModItems.TRUESIGHT_LENS.get(), 1, 1);

        // Rare Airdrop Crate Loot
        this.add("golden_apple_from_rare_airdop",
                new AddItemModifier(new LootItemCondition[] {
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.RARE_AD_CRATE.get()).build(),
                        LootItemRandomChanceCondition.randomChance(1f).build() }, Items.GOLDEN_APPLE, 1, 3));
        // One special item from the group (add more Choices here later).
        oneOf("rare_ad_special", ModBlocks.RARE_AD_CRATE.get(), 0.6f,
                new OneOfItemModifier.Choice(Items.MACE, 1));
        // Combat: mobility + control specials (one of the group) + counters.
        oneOf("rare_ad_combat", ModBlocks.RARE_AD_CRATE.get(), 0.50f,
                new OneOfItemModifier.Choice(ModItems.GRAPPLE_CROSSBOW.get(), 2),
                new OneOfItemModifier.Choice(ModItems.YOINK_ROD.get(), 2),
                new OneOfItemModifier.Choice(ModItems.WIND_CANNON.get(), 2));
        crateItem("launch_pad_from_rare_ad", ModBlocks.RARE_AD_CRATE.get(), 0.30f, ModBlocks.LAUNCH_PAD.get().asItem(), 1, 2);
        crateItem("bola_from_rare_ad",      ModBlocks.RARE_AD_CRATE.get(), 0.40f, ModItems.BOLA.get(), 2, 3);
        crateItem("anchor_from_rare_ad",    ModBlocks.RARE_AD_CRATE.get(), 0.35f, ModItems.ANCHOR_CHARM.get(), 1, 1);
        crateItem("truesight_from_rare_ad", ModBlocks.RARE_AD_CRATE.get(), 0.25f, ModItems.TRUESIGHT_LENS.get(), 1, 1);
        crateItem("bulwark_from_rare_ad",   ModBlocks.RARE_AD_CRATE.get(), 0.30f, ModItems.BULWARK_LEGGUARDS.get(), 1, 1);
        crateItem("lightning_rod_from_rare_ad", ModBlocks.RARE_AD_CRATE.get(), 0.30f, Items.LIGHTNING_ROD, 1, 1);

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
        // Combat: the heavy specials (one of the group) + the counters that beat them.
        oneOf("epic_ad_combat", ModBlocks.EPIC_AD_CRATE.get(), 0.60f,
                new OneOfItemModifier.Choice(ModItems.STORM_BRAND.get(), 2),
                new OneOfItemModifier.Choice(ModItems.KINGS_MAUL.get(), 2),
                new OneOfItemModifier.Choice(ModItems.WIND_CANNON.get(), 2),
                new OneOfItemModifier.Choice(ModItems.SUNDER_PIKE.get(), 2),
                new OneOfItemModifier.Choice(ModItems.GRAPPLE_CROSSBOW.get(), 1),
                new OneOfItemModifier.Choice(ModItems.SHADOW_CLOAK.get(), 1));
        crateItem("launch_pad_from_epic_ad",    ModBlocks.EPIC_AD_CRATE.get(), 0.35f, ModBlocks.LAUNCH_PAD.get().asItem(), 1, 2);
        crateItem("anchor_from_epic_ad",        ModBlocks.EPIC_AD_CRATE.get(), 0.40f, ModItems.ANCHOR_CHARM.get(), 1, 1);
        crateItem("bola_from_epic_ad",          ModBlocks.EPIC_AD_CRATE.get(), 0.40f, ModItems.BOLA.get(), 2, 4);
        crateItem("truesight_from_epic_ad",     ModBlocks.EPIC_AD_CRATE.get(), 0.30f, ModItems.TRUESIGHT_LENS.get(), 1, 1);
        crateItem("bulwark_from_epic_ad",       ModBlocks.EPIC_AD_CRATE.get(), 0.35f, ModItems.BULWARK_LEGGUARDS.get(), 1, 1);
        crateItem("lightning_rod_from_epic_ad", ModBlocks.EPIC_AD_CRATE.get(), 0.40f, Items.LIGHTNING_ROD, 1, 2);
        crateItem("end_crystal_from_epic_ad",   ModBlocks.EPIC_AD_CRATE.get(), 0.30f, Items.END_CRYSTAL, 1, 2);

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

        // ONE top-tier enchanted reward (not all). Weighted; now includes the custom specials.
        // Guaranteed (1.0) — the bounty is the richest crate in the game and should never whiff.
        oneOf("bounty_reward", ModBlocks.BOUNTY_CRATE.get(), 1.0f,
                new OneOfItemModifier.Choice(ModItems.SLAYER_SWORD.get(), 2,
                        Map.of(ResourceLocation.withDefaultNamespace("sharpness"), 2,
                               ResourceLocation.withDefaultNamespace("fire_aspect"), 2,
                               ResourceLocation.withDefaultNamespace("unbreaking"), 3)),
                new OneOfItemModifier.Choice(ModItems.SLAYER_BAT.get(), 1,
                        Map.of(ResourceLocation.withDefaultNamespace("knockback"), 10,
                               ResourceLocation.withDefaultNamespace("unbreaking"), 2)),
                new OneOfItemModifier.Choice(ModItems.STORM_BRAND.get(), 2,
                        Map.of(ResourceLocation.withDefaultNamespace("sharpness"), 3,
                               ResourceLocation.withDefaultNamespace("unbreaking"), 3)),
                new OneOfItemModifier.Choice(ModItems.KINGS_MAUL.get(), 2,
                        Map.of(ResourceLocation.withDefaultNamespace("density"), 3,
                               ResourceLocation.withDefaultNamespace("unbreaking"), 3)),
                new OneOfItemModifier.Choice(ModItems.WIND_CANNON.get(), 2),
                new OneOfItemModifier.Choice(ModItems.SHADOW_CLOAK.get(), 1),
                new OneOfItemModifier.Choice(ModItems.SUNDER_PIKE.get(), 2,
                        Map.of(ResourceLocation.withDefaultNamespace("sharpness"), 2,
                               ResourceLocation.withDefaultNamespace("unbreaking"), 3)),
                new OneOfItemModifier.Choice(Items.DIAMOND_CHESTPLATE, 2,
                        Map.of(ResourceLocation.withDefaultNamespace("protection"), 1,
                               ResourceLocation.withDefaultNamespace("unbreaking"), 3)));
        // Guaranteed counter kit — the bounty winner walks away ready to defend the lead.
        oneOf("bounty_counter", ModBlocks.BOUNTY_CRATE.get(), 1.0f,
                new OneOfItemModifier.Choice(ModItems.ANCHOR_CHARM.get(), 1, 1,1),
                new OneOfItemModifier.Choice(ModItems.BOLA.get(), 1, 3, 5),
                new OneOfItemModifier.Choice(ModItems.TRUESIGHT_LENS.get(), 1, 1, 1),
                new OneOfItemModifier.Choice(ModItems.BULWARK_LEGGUARDS.get(), 1, 1,1),
                new OneOfItemModifier.Choice(Items.END_CRYSTAL, 1, 1, 2)
                );

        // ------------------------------------------------------------------
        // Mystery Crate Loot — a curatable grab-bag (bought as a "box" at the Tribute
        // Stone). Mix in hostile spawn eggs, rare custom weapons, and oddities freely.
        // ------------------------------------------------------------------
        // GUARANTEED floor (chance 1.0): the crate is NEVER empty. Heavily weighted toward
        // filler/junk so you always get *something* but only rarely something great. The
        // bonus rolls below stack on top of this when their own chances pass.
        // Weight split: ~77% junk, ~19% decent, ~4% good (gapple/diamond).
        oneOf("mystery_guaranteed", ModBlocks.MYSTERY_CRATE.get(), 1.0f,
                new OneOfItemModifier.Choice(Items.BONE, 8, 1, 3),
                new OneOfItemModifier.Choice(Items.ARROW, 8, 4, 12),
                new OneOfItemModifier.Choice(Items.BREAD, 8, 1, 2),
                new OneOfItemModifier.Choice(Items.COAL, 6, 2, 5),
                new OneOfItemModifier.Choice(Items.STICK, 6, 2, 6),
                new OneOfItemModifier.Choice(Items.WHEAT_SEEDS, 5, 2, 4),
                new OneOfItemModifier.Choice(Items.IRON_INGOT, 4, 1, 3),
                new OneOfItemModifier.Choice(Items.ENDER_PEARL, 4, 1, 1),
                new OneOfItemModifier.Choice(Items.DIAMOND, 1, 1, 1));

        // Hostile eggs roll INDEPENDENTLY (was a single one-of pick): a crate can now carry a
        // MIX of egg types, and multiples of each, for more varied ambushes. Chances are tuned
        // so the combined odds of getting at least one egg stay ~0.9, but you'll often get two
        // or three different kinds. Zombies/skeletons are the staples; creepers/spiders rarer
        // (and capped lower) since they're nastier surprises.
        mysteryCrate("mystery_egg_zombie",   Items.ZOMBIE_SPAWN_EGG,   0.50f, 1, 3);
        mysteryCrate("mystery_egg_skeleton", Items.SKELETON_SPAWN_EGG, 0.50f, 1, 3);
        mysteryCrate("mystery_egg_creeper",  Items.CREEPER_SPAWN_EGG,  0.35f, 1, 2);
        mysteryCrate("mystery_egg_spider",   Items.SPIDER_SPAWN_EGG,   0.35f, 1, 2);
        mysteryCrate("mystery_egg_zombie",   Items.PILLAGER_SPAWN_EGG,   0.20f, 1, 3);
        mysteryCrate("mystery_egg_zombie",   Items.GHAST_SPAWN_EGG,   0.10f, 1, 3);
        mysteryCrate("mystery_egg_zombie",   Items.BREEZE_SPAWN_EGG,   0.10f, 1, 3);
        mysteryCrate("mystery_ender_pearls", Items.ENDER_PEARL,        0.40f, 1, 3);
        mysteryCrate("mystery_gapple",       Items.GOLDEN_APPLE,       0.25f, 1, 1);
        mysteryCrate("mystery_crown",       ModItems.CROWN.get(),       0.1f, 1, 1);
        // Combat counters in the grab-bag, so the mystery box can also arm your defense.
        mysteryCrate("mystery_bola",         ModItems.BOLA.get(),         0.30f, 1, 2);
        mysteryCrate("mystery_anchor",       ModItems.ANCHOR_CHARM.get(), 0.15f, 1, 1);
        mysteryCrate("mystery_truesight",    ModItems.TRUESIGHT_LENS.get(), 0.15f, 1, 1);
        mysteryCrate("mystery_bulwark",      ModItems.BULWARK_LEGGUARDS.get(), 0.15f, 1, 1);
        mysteryCrate("mystery_launch_pad",   ModBlocks.LAUNCH_PAD.get().asItem(), 0.20f, 1, 2);

        // Rare weapon — ONE of the group (not all). ~25% chance. Now spans the full custom arsenal.
        oneOf("mystery_rare_weapon", ModBlocks.MYSTERY_CRATE.get(), 0.25f,
                new OneOfItemModifier.Choice(ModItems.SLAYER_SWORD.get(), 3),
                new OneOfItemModifier.Choice(ModItems.SLAYER_BAT.get(), 2),
                new OneOfItemModifier.Choice(ModItems.YOINK_ROD.get(), 2),
                new OneOfItemModifier.Choice(ModItems.GRAPPLE_CROSSBOW.get(), 1),
                new OneOfItemModifier.Choice(ModItems.STORM_BRAND.get(), 1),
                new OneOfItemModifier.Choice(ModItems.KINGS_MAUL.get(), 1),
                new OneOfItemModifier.Choice(ModItems.WIND_CANNON.get(), 1),
                new OneOfItemModifier.Choice(ModItems.SHADOW_CLOAK.get(), 1),
                new OneOfItemModifier.Choice(ModItems.SUNDER_PIKE.get(), 1));

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

    /** Adds an item drop (uniform count) to any crate block's loot when {@code chance} passes. */
    private void crateItem(String name, net.minecraft.world.level.block.Block crate, float chance,
                           net.minecraft.world.item.Item item, int min, int max) {
        this.add(name, new AddItemModifier(new LootItemCondition[] {
                LootItemBlockStatePropertyCondition.hasBlockStateProperties(crate).build(),
                LootItemRandomChanceCondition.randomChance(chance).build() },
                item, min, max));
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
