package com.koreykeipe.kingslayer.airdrop;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/**
 * Server-side config for the KingSlayer airdrop system.
 * Loaded from {@code config/kcs_kingslayer-server.toml}.
 *
 * <p>Loot entries use the format: {@code "namespace:item_id chance min max"}<br>
 * Example: {@code "minecraft:golden_apple 0.8 1 2"}</p>
 */
public class AirdropConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // -------------------------------------------------------------------------
    // Global settings
    // -------------------------------------------------------------------------

    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.IntValue SPAWN_HEIGHT;
    /**
     * Terminal fall velocity in blocks/tick. Lower = slower, more cinematic drop.
     * 0.5 ≈ 5s from 60 blocks, 1.5 ≈ 3s from 60 blocks, 3.0 ≈ 2s from 60 blocks.
     */
    public static final ForgeConfigSpec.DoubleValue FALL_SPEED;
    /** How many ticks the chest glows after landing. 20 ticks = 1 second. */
    public static final ForgeConfigSpec.IntValue GLOW_DURATION;

    // -------------------------------------------------------------------------
    // Per-tier settings
    // -------------------------------------------------------------------------

    public static final TierConfig COMMON;
    public static final TierConfig RARE;
    public static final TierConfig EPIC;
    public static final TierConfig LEGENDARY;

    static {
        BUILDER.comment("=== KingSlayer Airdrop System ===").push("global");
        ENABLED = BUILDER
                .comment("Set to false to disable all airdrops.")
                .define("enabled", true);
        SPAWN_HEIGHT = BUILDER
                .comment("How many blocks above the surface the airdrop spawns before falling.")
                .defineInRange("spawn_height", 60, 10, 256);
        FALL_SPEED = BUILDER
                .comment("Terminal fall velocity in blocks/tick (acceleration is fixed at 0.04 b/t²). "
                        + "0.5 = slow/cinematic (~5s from 60 blocks), 1.5 = default (~3s), 3.0 = fast (~2s).")
                .defineInRange("fall_speed", 1.5, 0.1, 5.0);
        GLOW_DURATION = BUILDER
                .comment("How many ticks the chest glows after landing. 200 = 10 seconds.")
                .defineInRange("glow_duration_ticks", 200, 20, 6000);
        BUILDER.pop();

        // repeat_interval_ticks defaults (0 = fire once and stop):
        //   6000  =  5 min  |  9000  = 7.5 min  |  12000 = 10 min  |  18000 = 15 min
        COMMON = new TierConfig(BUILDER, "common", 0.15, 6000,
                List.of(
                        "minecraft:golden_apple 0.9 1 2",
                        "minecraft:bow 0.6 1 1",
                        "minecraft:arrow 1.0 8 24",
                        "minecraft:cooked_beef 0.8 3 6",
                        "minecraft:iron_ingot 0.7 2 4"
                ));

        RARE = new TierConfig(BUILDER, "rare", 0.35, 9000,
                List.of(
                        "minecraft:enchanted_golden_apple 0.2 1 1",
                        "minecraft:diamond_sword 0.5 1 1",
                        "minecraft:arrow 1.0 16 32",
                        "minecraft:golden_apple 0.8 2 4",
                        "minecraft:iron_chestplate 0.6 1 1"
                ));

        EPIC = new TierConfig(BUILDER, "epic", 0.60, 12000,
                List.of(
                        "minecraft:enchanted_golden_apple 0.5 1 2",
                        "minecraft:diamond_chestplate 0.6 1 1",
                        "minecraft:diamond_sword 0.8 1 1",
                        "minecraft:totem_of_undying 0.3 1 1",
                        "minecraft:arrow 1.0 24 48"
                ));

        LEGENDARY = new TierConfig(BUILDER, "legendary", 0.85, 18000,
                List.of(
                        "minecraft:enchanted_golden_apple 1.0 2 3",
                        "minecraft:netherite_sword 0.8 1 1",
                        "minecraft:netherite_chestplate 0.7 1 1",
                        "minecraft:totem_of_undying 0.7 1 2",
                        "minecraft:arrow 1.0 32 64"
                ));
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // -------------------------------------------------------------------------
    // Inner class — per-tier config block
    // -------------------------------------------------------------------------

    public static class TierConfig {

        /**
         * Fraction (0.0–1.0) of total possible player deaths that must be reached to unlock this tier.
         * Formula: {@code sum(min(deaths, 5) per online player) / (playerCount × 5)}.
         */
        public final ForgeConfigSpec.DoubleValue thresholdPercent;

        /**
         * How many ticks between repeat drops once this tier is unlocked.
         * Set to 0 to fire only once (on threshold cross) and never repeat.
         * 6000 = 5 min, 12000 = 10 min, 24000 = 20 min.
         */
        public final ForgeConfigSpec.IntValue repeatIntervalTicks;

        /**
         * Loot entries. Each string: {@code "namespace:item chance min max"}.
         * All four fields are required. Invalid entries are silently skipped.
         */
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> loot;

        TierConfig(ForgeConfigSpec.Builder builder, String name, double defaultThreshold,
                   int defaultRepeatInterval, List<String> defaultLoot) {
            builder.push("tiers").push(name);

            thresholdPercent = builder
                    .comment("Death-progress fraction (0.0–1.0) required to trigger this drop tier.")
                    .defineInRange("death_threshold_percent", defaultThreshold, 0.0, 1.0);

            repeatIntervalTicks = builder
                    .comment("Ticks between repeat drops after this tier unlocks. 0 = fire once only. "
                            + "(20 ticks = 1 second, 6000 = 5 min, 12000 = 10 min)")
                    .defineInRange("repeat_interval_ticks", defaultRepeatInterval, 0, Integer.MAX_VALUE);

            loot = builder
                    .comment("Loot list. Format: \"namespace:item_id chance min max\"  (chance = 0.0–1.0).")
                    .defineListAllowEmpty("loot", defaultLoot, obj -> {
                        if (!(obj instanceof String s)) return false;
                        String[] p = s.trim().split("\\s+");
                        if (p.length != 4) return false;
                        try {
                            Double.parseDouble(p[1]);
                            Integer.parseInt(p[2]);
                            Integer.parseInt(p[3]);
                            return true;
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    });

            builder.pop().pop();
        }
    }
}
