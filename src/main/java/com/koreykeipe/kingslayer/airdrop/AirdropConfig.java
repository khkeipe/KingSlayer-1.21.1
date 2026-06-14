package com.koreykeipe.kingslayer.airdrop;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Server-side config for the KingSlayer airdrop system.
 * Loaded from {@code config/kcs_kingslayer-server.toml}.
 *
 * <p>Loot is defined in JSON loot tables at
 * {@code data/kcs_kingslayer/loot_tables/airdrops/<tier>.json}.
 * Override any tier's table at runtime with a datapack — no recompile needed.</p>
 */
public class AirdropConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // -------------------------------------------------------------------------
    // Global settings
    // -------------------------------------------------------------------------

    public static final ForgeConfigSpec.BooleanValue ENABLED;
    /**
     * Total remaining lives across the WHOLE roster at which The King is auto-summoned as the
     * grand finale — fires once when remaining lives drop to this value, regardless of player
     * count. An absolute lives count (not a %) guarantees a consistent buffer at any player
     * count: 3 means he rises with more than 2 lives still in play. The auto-summon also
     * requires more than one player still alive; a Boss Key can bring him earlier. 0 = disable.
     */
    public static final ForgeConfigSpec.IntValue KING_AUTO_SUMMON_LIVES;
    public static final ForgeConfigSpec.IntValue SPAWN_HEIGHT;
    /**
     * Terminal fall velocity in blocks/tick. Lower = slower, more cinematic drop.
     * 0.5 ≈ 5s from 60 blocks, 1.5 ≈ 3s from 60 blocks, 3.0 ≈ 2s from 60 blocks.
     */
    public static final ForgeConfigSpec.DoubleValue FALL_SPEED;
    /** How many ticks the chest glows after landing. 20 ticks = 1 second. */
    public static final ForgeConfigSpec.IntValue GLOW_DURATION;

    // -------------------------------------------------------------------------
    // World-border settings
    // -------------------------------------------------------------------------

    /** Whether automated world-border scaling is active. */
    public static final ForgeConfigSpec.BooleanValue BORDER_ENABLED;
    /**
     * Border radius (blocks) contributed by each player who joins before the game
     * becomes active.  Starting diameter = 2 × playerCount × this value.
     * Example: 10 players × 100 = 1 000-block radius (2 000-block diameter).
     */
    public static final ForgeConfigSpec.IntValue INITIAL_BLOCKS_PER_PLAYER;
    /** Hard floor on border radius. The border will never shrink below 2× this (diameter). */
    public static final ForgeConfigSpec.IntValue BORDER_MIN_RADIUS;

    // -------------------------------------------------------------------------
    // Per-tier settings
    // -------------------------------------------------------------------------

    public static final TierConfig BROKEN;
    public static final TierConfig COMMON;
    public static final TierConfig RARE;
    public static final TierConfig EPIC;

    static {
        BUILDER.comment("=== KingSlayer Airdrop System ===").push("global");
        ENABLED = BUILDER
                .comment("Set to false to disable all airdrops.")
                .define("enabled", true);
        KING_AUTO_SUMMON_LIVES = BUILDER
                .comment("Total remaining lives across the whole roster at which The King auto-summons as the "
                        + "finale (fires once, regardless of player count). 3 = he rises with more than 2 lives "
                        + "still in play. Also requires more than one player alive. 0 = disable auto-summon.")
                .defineInRange("king_auto_summon_lives", 3, 0, 1000);
        SPAWN_HEIGHT = BUILDER
                .comment("How many blocks above the surface the airdrop spawns before falling.")
                .defineInRange("spawn_height", 150, 50, 256);
        FALL_SPEED = BUILDER
                .comment("Terminal fall velocity in blocks/tick (acceleration is fixed at 0.04 b/t²). "
                        + "0.5 = slow/cinematic (~5s from 60 blocks), 1.5 = default (~3s), 3.0 = fast (~2s).")
                .defineInRange("fall_speed", 0.5, 0.1, 5.0);
        GLOW_DURATION = BUILDER
                .comment("How many ticks the chest glows after landing. 200 = 10 seconds.")
                .defineInRange("glow_duration_ticks", 1500, 20, 6000);

        BORDER_ENABLED = BUILDER
                .comment("Set to false to leave the world border unmanaged by the airdrop system.")
                .define("border_enabled", true);
        INITIAL_BLOCKS_PER_PLAYER = BUILDER
                .comment("Border radius (blocks) contributed by each player who joins before the game starts. "
                        + "Starting diameter = 2 × playerCount × this value. "
                        + "Example: 10 players × 100 = 1 000-block radius (2 000-block diameter).")
                .defineInRange("initial_blocks_per_player", 100, 10, 5000);
        BORDER_MIN_RADIUS = BUILDER
                .comment("Minimum border radius (blocks). The border will never be shrunk below this.")
                .defineInRange("border_min_radius", 100, 10, 5000);
        BUILDER.pop();

        // repeat_interval_ticks defaults (0 = fire once and stop):
        //   6000  =  5 min  |  9000  = 7.5 min  |  12000 = 10 min  |  18000 = 15 min
        BROKEN = new TierConfig(BUILDER, "broken", 0.15, 6000, 75, 12000);

        COMMON = new TierConfig(BUILDER, "common", 0.35, 9000, 100, 15000);

        RARE = new TierConfig(BUILDER, "rare", 0.60, 12000, 125, 18000);

        EPIC = new TierConfig(BUILDER, "epic", 0.85, 18000, 150, 24000);
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
         * Radius (blocks) by which to shrink the world border when this tier first triggers.
         * Set to 0 to skip the border shrink for this tier entirely.
         */
        public final ForgeConfigSpec.IntValue borderShrinkRadius;

        /**
         * How many seconds the border-shrink transition takes.
         * The border lerps smoothly from its current size to the new target over this duration.
         */
        public final ForgeConfigSpec.IntValue borderShrinkSeconds;

        TierConfig(ForgeConfigSpec.Builder builder, String name, double defaultThreshold,
                   int defaultRepeatInterval, int defaultShrinkRadius, int defaultShrinkSeconds) {
            builder.push("tiers").push(name);

            thresholdPercent = builder
                    .comment("Death-progress fraction (0.0–1.0) required to trigger this drop tier.")
                    .defineInRange("death_threshold_percent", defaultThreshold, 0.0, 1.0);

            repeatIntervalTicks = builder
                    .comment("Ticks between repeat drops after this tier unlocks. 0 = fire once only. "
                            + "(20 ticks = 1 second, 6000 = 5 min, 12000 = 10 min)")
                    .defineInRange("repeat_interval_ticks", defaultRepeatInterval, 0, Integer.MAX_VALUE);

            borderShrinkRadius = builder
                    .comment("Radius (blocks) to shrink the world border when this tier first triggers. "
                            + "0 = no border shrink for this tier.")
                    .defineInRange("border_shrink_radius", defaultShrinkRadius, 0, 10000);

            borderShrinkSeconds = builder
                    .comment("Duration of the border shrink transition in seconds.")
                    .defineInRange("border_shrink_seconds", defaultShrinkSeconds, 1, 3600);

            builder.pop().pop();
        }
    }
}
