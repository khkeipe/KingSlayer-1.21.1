package com.koreykeipe.kingslayer.worldgen;

import com.koreykeipe.kingslayer.KingSlayer;
import com.koreykeipe.kingslayer.block.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.*;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;

import java.util.Optional;

/**
 * One configured feature per crate tier, plus a couple of decorative no-crate piles.
 * The feature describes <em>what</em> to place; {@link ModPlacedFeatures} decides
 * <em>where</em> and {@link ModBiomeModifiers} decides which biomes receive it.
 *
 * <p>Broken uses {@link Feature#BLOCK_PILE} (can scatter several crates). Common/Rare/Epic
 * use {@link CrateDebrisFeature}: a guaranteed crate wrapped in a tight, rarity-themed
 * mound, with a small chance to hide a higher-tier crate in the rubble.</p>
 */
public class ModConfiguredFeatures {

    public static final ResourceKey<ConfiguredFeature<?,?>> BROKEN_CRATE_KEY = registerKey("broken_crate");
    public static final ResourceKey<ConfiguredFeature<?,?>> COMMON_CRATE_KEY = registerKey("common_crate");
    public static final ResourceKey<ConfiguredFeature<?,?>> RARE_CRATE_KEY   = registerKey("rare_crate");
    public static final ResourceKey<ConfiguredFeature<?,?>> EPIC_CRATE_KEY   = registerKey("epic_crate");

    // Decorative (no-crate) piles — King's-realm atmosphere: camps, battles, graves, ruins.
    public static final ResourceKey<ConfiguredFeature<?,?>> DECOR_CAMP_KEY   = registerKey("decor_camp");
    public static final ResourceKey<ConfiguredFeature<?,?>> DECOR_BATTLE_KEY = registerKey("decor_battle");
    public static final ResourceKey<ConfiguredFeature<?,?>> DECOR_GRAVE_KEY  = registerKey("decor_grave");
    public static final ResourceKey<ConfiguredFeature<?,?>> DECOR_RUINS_KEY  = registerKey("decor_ruins");

    // Custom NBT structures (Route A). Wired and waiting — places nothing until the
    // matching .nbt exists at data/kcs_kingslayer/structure/<name>.nbt.
    public static final ResourceKey<ConfiguredFeature<?,?>> KNIGHT_TENT_KEY  = registerKey("knight_tent");
    public static final ResourceKey<ConfiguredFeature<?,?>> BIG_TENT_KEY     = registerKey("big_tent");
    public static final ResourceKey<ConfiguredFeature<?,?>> OUTPOST_KEY      = registerKey("outpost_01");
    public static final ResourceKey<ConfiguredFeature<?,?>> CRYPT_KEY        = registerKey("crypt");
    public static final ResourceKey<ConfiguredFeature<?,?>> GRAVEYARD_KEY    = registerKey("graveyard");
    public static final ResourceKey<ConfiguredFeature<?,?>> SHIP_KEY         = registerKey("ship");

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {

        // BROKEN — a "crashed supply" wreckage pile (BLOCK_PILE). Can scatter several
        // broken crates; dressed with fallen (side-laid) logs and an occasional intact
        // Common crate buried in the rubble.
        register(context, BROKEN_CRATE_KEY, Feature.BLOCK_PILE,
                new BlockPileConfiguration(
                        new WeightedStateProvider(
                                SimpleWeightedRandomList.<BlockState>builder()
                                        .add(ModBlocks.BROKEN_CRATE.get().defaultBlockState(), 3)
                                        .add(Blocks.GRAVEL.defaultBlockState(), 2)
                                        .add(Blocks.COARSE_DIRT.defaultBlockState(), 2)
                                        .add(sideLog(Blocks.OAK_LOG, Direction.Axis.X), 2)
                                        .add(sideLog(Blocks.OAK_LOG, Direction.Axis.Z), 1)
                                        .add(ModBlocks.COMMON_CRATE.get().defaultBlockState(), 1)
                        )));

        // Common — tight humble rubble (radius 1); ~6% chance to hide a Rare crate.
        register(context, COMMON_CRATE_KEY, ModFeatures.CRATE_DEBRIS.get(),
                new CrateDebrisConfiguration(
                        ModBlocks.COMMON_CRATE.get().defaultBlockState(),
                        new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
                                .add(Blocks.COBBLESTONE.defaultBlockState(), 4)
                                .add(Blocks.STONE.defaultBlockState(), 3)
                                .add(Blocks.DIRT.defaultBlockState(), 2)
                                .add(Blocks.GRAVEL.defaultBlockState(), 2)
                                .add(sideLog(Blocks.OAK_LOG, Direction.Axis.X), 1)
                                .add(sideLog(Blocks.OAK_LOG, Direction.Axis.Z), 1)
                                .add(Blocks.COAL_ORE.defaultBlockState(), 1)),
                        1, 0.8f,
                        Optional.of(BlockStateProvider.simple(ModBlocks.RARE_CRATE.get().defaultBlockState())), 0.06f));

        // Rare — tight ore-flecked rubble (radius 1); ~5% chance to hide an Epic crate.
        register(context, RARE_CRATE_KEY, ModFeatures.CRATE_DEBRIS.get(),
                new CrateDebrisConfiguration(
                        ModBlocks.RARE_CRATE.get().defaultBlockState(),
                        new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
                                .add(Blocks.STONE.defaultBlockState(), 3)
                                .add(Blocks.ANDESITE.defaultBlockState(), 2)
                                .add(Blocks.COBBLESTONE.defaultBlockState(), 2)
                                .add(Blocks.IRON_ORE.defaultBlockState(), 2)
                                .add(Blocks.COPPER_ORE.defaultBlockState(), 2)
                                .add(Blocks.RAW_COPPER_BLOCK.defaultBlockState(), 1)),
                        1, 0.8f,
                        Optional.of(BlockStateProvider.simple(ModBlocks.EPIC_CRATE.get().defaultBlockState())), 0.05f));

        // Epic — a glittering wreck with raw ore blocks (radius 2); no higher tier to bonus into.
        register(context, EPIC_CRATE_KEY, ModFeatures.CRATE_DEBRIS.get(),
                new CrateDebrisConfiguration(
                        ModBlocks.EPIC_CRATE.get().defaultBlockState(),
                        new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
                                .add(Blocks.DEEPSLATE.defaultBlockState(), 3)
                                .add(Blocks.BLACKSTONE.defaultBlockState(), 2)
                                .add(Blocks.GOLD_ORE.defaultBlockState(), 2)
                                .add(Blocks.IRON_ORE.defaultBlockState(), 2)
                                .add(Blocks.RAW_IRON_BLOCK.defaultBlockState(), 1)
                                .add(Blocks.RAW_GOLD_BLOCK.defaultBlockState(), 1)),
                        2, 0.75f,
                        Optional.empty(), 0f));

        // DECORATIVE — no loot, pure King's-realm atmosphere so the world feels like a
        // war-torn kingdom as you run around: knight camps, battle aftermaths, graves, ruins.

        // Knight Camp — a burnt-out (unlit) campfire so it never reads as an airdrop smoke
        // signal, ringed by supplies, fallen logs, and lanterns for warm night-time glow.
        register(context, DECOR_CAMP_KEY, ModFeatures.CRATE_DEBRIS.get(),
                new CrateDebrisConfiguration(
                        Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false),
                        new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
                                .add(sideLog(Blocks.OAK_LOG, Direction.Axis.X), 2)
                                .add(sideLog(Blocks.OAK_LOG, Direction.Axis.Z), 2)
                                .add(Blocks.HAY_BLOCK.defaultBlockState(), 2)
                                .add(Blocks.LANTERN.defaultBlockState(), 2)
                                .add(Blocks.BARREL.defaultBlockState(), 1)
                                .add(Blocks.CRAFTING_TABLE.defaultBlockState(), 1)
                                .add(Blocks.COBBLESTONE.defaultBlockState(), 2)),
                        2, 0.5f, Optional.empty(), 0f));

        // Battle Site — broken fortifications and bones marking where knights fell.
        register(context, DECOR_BATTLE_KEY, Feature.BLOCK_PILE,
                new BlockPileConfiguration(new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
                        .add(Blocks.COBBLESTONE.defaultBlockState(), 3)
                        .add(Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 2)
                        .add(Blocks.COBBLESTONE_WALL.defaultBlockState(), 2)
                        .add(Blocks.IRON_BARS.defaultBlockState(), 1)
                        .add(Blocks.BONE_BLOCK.defaultBlockState(), 1)
                        .add(Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), 1)
                        .add(Blocks.COARSE_DIRT.defaultBlockState(), 2))));

        // Grave — a cobblestone headstone over a coarse-dirt mound, lit by an eerie soul torch.
        register(context, DECOR_GRAVE_KEY, ModFeatures.CRATE_DEBRIS.get(),
                new CrateDebrisConfiguration(
                        Blocks.COBBLESTONE_WALL.defaultBlockState(),
                        new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
                                .add(Blocks.COARSE_DIRT.defaultBlockState(), 4)
                                .add(Blocks.PODZOL.defaultBlockState(), 2)
                                .add(Blocks.DIRT.defaultBlockState(), 2)
                                .add(Blocks.STONE_BRICKS.defaultBlockState(), 1)
                                .add(Blocks.SOUL_TORCH.defaultBlockState(), 1)),
                        1, 0.7f, Optional.empty(), 0f));

        // Ruined Keep — crumbled stonework from an old fortress, half-swallowed by moss.
        register(context, DECOR_RUINS_KEY, Feature.BLOCK_PILE,
                new BlockPileConfiguration(new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
                        .add(Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 3)
                        .add(Blocks.COBBLESTONE.defaultBlockState(), 2)
                        .add(Blocks.STONE_BRICKS.defaultBlockState(), 2)
                        .add(Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), 2)
                        .add(Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), 1)
                        .add(Blocks.COBBLESTONE_WALL.defaultBlockState(), 1)
                        .add(Blocks.LANTERN.defaultBlockState(), 1)
                        .add(sideLog(Blocks.OAK_LOG, Direction.Axis.Z), 1))));

        // CUSTOM NBT STRUCTURES (Route A) — stamps a saved template. Builds nothing until
        // you drop knight_tent.nbt into data/kcs_kingslayer/structure/. integrity 1.0 = intact.
        // Surface tents/outpost — levelled flush, and now slope-gated (max_slope 4) so they
        // skip cliffs instead of leaving a flat pad floating off a hillside.
        register(context, KNIGHT_TENT_KEY, ModFeatures.TEMPLATE.get(),
                new TemplateConfiguration(
                        ResourceLocation.fromNamespaceAndPath(KingSlayer.MOD_ID, "knight_tent"),
                        1.0f, true, 0, 4));

        register(context, BIG_TENT_KEY, ModFeatures.TEMPLATE.get(),
                new TemplateConfiguration(
                        ResourceLocation.fromNamespaceAndPath(KingSlayer.MOD_ID, "big_tent"),
                        1.0f, true, 0, 4));

        // Outpost — sunk 1 block so its bottom layer (which holds a spawner) sits underground
        // rather than resting on the surface. Still levelled so the tower seats flush on slopes.
        register(context, OUTPOST_KEY, ModFeatures.TEMPLATE.get(),
                new TemplateConfiguration(
                        ResourceLocation.fromNamespaceAndPath(KingSlayer.MOD_ID, "outpost_01"),
                        1.0f, true, 1, 4));

        // Crypt — a buried tomb. level=false keeps the hill above it; bury_depth sinks the
        // bulk so only the top couple of layers (the ruined entrance lip) pierce the surface.
        // integrity 1.0 for now so the raw shell is fully visible while testing the shape;
        // drop to ~0.85 later to weather the exposed entrance.
        // biome_palette=true → its grass/dirt becomes sand/sandstone in deserts (red in badlands).
        register(context, CRYPT_KEY, ModFeatures.TEMPLATE.get(),
                new TemplateConfiguration(
                        ResourceLocation.fromNamespaceAndPath(KingSlayer.MOD_ID, "crypt"),
                        1.0f, false, 6, 4, true));

        // Graveyard — 9x4x9, sunk HALF underground (bury_depth 2 of 4 layers) so the graves sit
        // below ground and the headstones/markers pierce the surface. level=false keeps the
        // surrounding ground intact; max_slope 4 keeps it off cliffs like the crypt.
        register(context, GRAVEYARD_KEY, ModFeatures.TEMPLATE.get(),
                new TemplateConfiguration(
                        ResourceLocation.fromNamespaceAndPath(KingSlayer.MOD_ID, "graveyard"),
                        1.0f, false, 2, 4, true));

        // Ship (10x18x23) — floats on the sea. Its placed feature lands on the WATER surface
        // (not the seabed), then bury_depth 2 sinks the hull two blocks below the waterline so
        // the rest rides above. level=false so we never carve/fill the ocean floor;
        // max_water=-1 waives the dry-land rule and require_water=true keeps it off shorelines.
        register(context, SHIP_KEY, ModFeatures.TEMPLATE.get(),
                new TemplateConfiguration(
                        ResourceLocation.fromNamespaceAndPath(KingSlayer.MOD_ID, "ship"),
                        1.0f, false, 2, 0, true, -1, true));
    }

    /** A log laid on its side along the given horizontal axis (X or Z) instead of upright. */
    private static BlockState sideLog(Block log, Direction.Axis axis) {
        return log.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(KingSlayer.MOD_ID, name));
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(BootstrapContext<ConfiguredFeature<?, ?>> context,
                                                                                          ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }
}
