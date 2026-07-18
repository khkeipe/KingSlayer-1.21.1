package com.koreykeipe.kingslayer.airdrop;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side config for the KingSlayer airdrop system.
 * Loaded from {@code config/kcs_kingslayer-server.toml}.
 *
 * <p>Loot is defined in JSON loot tables at
 * {@code data/kcs_kingslayer/loot_tables/airdrops/<tier>.json}.
 * Override any tier's table at runtime with a datapack — no recompile needed.</p>
 */
public class AirdropConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // -------------------------------------------------------------------------
    // Global settings
    // -------------------------------------------------------------------------

    public static final ModConfigSpec.BooleanValue ENABLED;
    /** Lives each player starts with; elimination latches when their death count reaches this. */
    public static final ModConfigSpec.IntValue LIVES;
    /**
     * Total remaining lives across the WHOLE roster at which The King is auto-summoned as the
     * grand finale — fires once when remaining lives drop to this value, regardless of player
     * count. An absolute lives count (not a %) guarantees a consistent buffer at any player
     * count: 3 means he rises with more than 2 lives still in play. The auto-summon also
     * requires more than one player still alive; a Boss Key can bring him earlier. 0 = disable.
     */
    public static final ModConfigSpec.IntValue KING_AUTO_SUMMON_LIVES;
    public static final ModConfigSpec.IntValue SPAWN_HEIGHT;
    /**
     * Terminal fall velocity in blocks/tick. Lower = slower, more cinematic drop.
     * 0.5 ≈ 5s from 60 blocks, 1.5 ≈ 3s from 60 blocks, 3.0 ≈ 2s from 60 blocks.
     */
    public static final ModConfigSpec.DoubleValue FALL_SPEED;
    /** How many ticks the chest glows after landing. 20 ticks = 1 second. */
    public static final ModConfigSpec.IntValue GLOW_DURATION;
    /** Ticks between automatic standings broadcasts (skipped when standings are unchanged). */
    public static final ModConfigSpec.IntValue LEADERBOARD_INTERVAL;
    /**
     * Airdrops spawn within this many blocks of a randomly chosen active player (clamped to the
     * world border). 0 = fall back to a fully random position inside the border (old behaviour).
     */
    public static final ModConfigSpec.IntValue DROP_RADIUS;

    // -------------------------------------------------------------------------
    // World-border settings
    // -------------------------------------------------------------------------

    /** Whether automated world-border scaling is active. */
    public static final ModConfigSpec.BooleanValue BORDER_ENABLED;
    /**
     * Border radius (blocks) contributed by each player who joins before the game
     * becomes active.  Starting diameter = 2 × playerCount × this value.
     * Example: 10 players × 100 = 1 000-block radius (2 000-block diameter).
     */
    public static final ModConfigSpec.IntValue INITIAL_BLOCKS_PER_PLAYER;
    /** Hard floor on border radius. The border will never shrink below 2× this (diameter). */
    public static final ModConfigSpec.IntValue BORDER_MIN_RADIUS;

    // -------------------------------------------------------------------------
    // Knight settings
    // -------------------------------------------------------------------------

    /** Movement-speed base attribute for each knight tier. */
    public static final ModConfigSpec.DoubleValue FOOTSOLDIER_SPEED;
    public static final ModConfigSpec.DoubleValue CHAMPION_SPEED;
    public static final ModConfigSpec.DoubleValue GUARD_SPEED;
    /** Max health per knight tier. */
    public static final ModConfigSpec.DoubleValue FOOTSOLDIER_HP;
    public static final ModConfigSpec.DoubleValue CHAMPION_HP;
    public static final ModConfigSpec.DoubleValue GUARD_HP;
    /** Base attack-damage overrides for the champion/guard (footsoldier keeps its iron sword). */
    public static final ModConfigSpec.DoubleValue CHAMPION_ATTACK;
    public static final ModConfigSpec.DoubleValue GUARD_ATTACK;
    /** Ambient knight spawn pacing near players. */
    public static final ModConfigSpec.DoubleValue KNIGHT_SPAWN_CHANCE;
    public static final ModConfigSpec.IntValue    KNIGHT_SPAWN_INTERVAL_TICKS;
    public static final ModConfigSpec.IntValue    KNIGHT_MAX_NEAR_PLAYER;

    // -------------------------------------------------------------------------
    // Bounty / The Marked pacing
    // -------------------------------------------------------------------------

    /** How long (minutes) a bounty stays active before it expires unclaimed. */
    public static final ModConfigSpec.IntValue BOUNTY_CONTRACT_MINUTES;
    /** Minutes after a bounty is ASSIGNED before another may be selected (spacing). */
    public static final ModConfigSpec.IntValue BOUNTY_SELECTION_COOLDOWN_MINUTES;
    /** Minutes a former bounty holder is excluded from becoming the bounty again. */
    public static final ModConfigSpec.IntValue BOUNTY_RESELECT_PLAYER_MINUTES;
    /** Threat score a player must exceed to be eligible to become The Marked. */
    public static final ModConfigSpec.IntValue BOUNTY_THREAT_THRESHOLD;
    /** Threat points awarded per player kill / assist. */
    public static final ModConfigSpec.IntValue THREAT_PER_KILL;
    public static final ModConfigSpec.IntValue THREAT_PER_ASSIST;
    /** Threat points awarded per knight kill, by tier. */
    public static final ModConfigSpec.IntValue THREAT_PER_FOOTSOLDIER;
    public static final ModConfigSpec.IntValue THREAT_PER_CHAMPION;
    public static final ModConfigSpec.IntValue THREAT_PER_GUARD;

    // -------------------------------------------------------------------------
    // Combat-item power
    // -------------------------------------------------------------------------

    public static final ModConfigSpec.DoubleValue LAUNCH_PAD_VERTICAL;
    public static final ModConfigSpec.DoubleValue LAUNCH_PAD_FORWARD;
    public static final ModConfigSpec.DoubleValue YOINK_MAX_PULL;
    public static final ModConfigSpec.IntValue    BOLA_ROOT_TICKS;
    public static final ModConfigSpec.IntValue    BOLA_COOLDOWN_TICKS;
    public static final ModConfigSpec.DoubleValue BOLA_RANGE;
    public static final ModConfigSpec.DoubleValue BULWARK_DAMAGE_REDUCTION;
    /** Volume of the Storm Brand's local thunder replacement (vanilla's is global). */
    public static final ModConfigSpec.DoubleValue STORM_BRAND_THUNDER_VOLUME;

    /** Minimum ticks between The King's telegraphed lightning storms. */
    public static final ModConfigSpec.IntValue KING_LIGHTNING_COOLDOWN;
    /** Minimum ticks between The King's hex casts. */
    public static final ModConfigSpec.IntValue KING_HEX_COOLDOWN;

    // -------------------------------------------------------------------------
    // Custom-structure content markers
    // -------------------------------------------------------------------------

    /** Chance (0-1) each {@code kcs:crate} data marker in a structure becomes a crate. */
    public static final ModConfigSpec.DoubleValue STRUCTURE_CRATE_CHANCE;
    /** Horizontal radius a structure spawner places mobs within (vanilla default 4). */
    public static final ModConfigSpec.IntValue SPAWNER_SPAWN_RANGE;
    /** How many of its mob may be nearby before a spawner pauses (vanilla default 6). */
    public static final ModConfigSpec.IntValue SPAWNER_MAX_NEARBY;
    /** How close a player must be for a spawner to activate (vanilla default 16). */
    public static final ModConfigSpec.IntValue SPAWNER_PLAYER_RANGE;

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
        LIVES = BUILDER
                .comment("Lives each player gets before elimination (the core tournament knob). "
                        + "Drives name colours, death progress, airdrop-tier pacing and the win condition.")
                .defineInRange("lives_per_player", 5, 1, 50);
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
                .defineInRange("fall_speed", 0.3, 0.1, 5.0);
        GLOW_DURATION = BUILDER
                .comment("How many ticks the smoke signal rises after the crate lands. "
                        + "20 ticks = 1 second, 3000 = 150 seconds.")
                .defineInRange("glow_duration_ticks", 4000, 20, 12000);
        DROP_RADIUS = BUILDER
                .comment("Airdrops spawn within this many blocks of a randomly chosen active player "
                        + "(clamped to the world border). 0 = fully random position inside the border.")
                .defineInRange("drop_radius", 90, 0, 4000);

        LEADERBOARD_INTERVAL = BUILDER
                .comment("How many ticks between standings broadcasts. 20 ticks = 1 second, "
                        + "36000 = 30 minutes. Standings are skipped entirely when nothing has "
                        + "changed since the last post, so this is a ceiling, not a guarantee. "
                        + "0 = never broadcast standings automatically.")
                .defineInRange("leaderboard_interval_ticks", 36000, 0, 72000);

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

        BUILDER.comment("Knight movement speed (base MOVEMENT_SPEED attribute). "
                + "Vanilla references: zombie 0.23, vindicator 0.35, wither skeleton 0.25.").push("knights");
        FOOTSOLDIER_SPEED = BUILDER
                .comment("Footsoldier (zombie) movement speed.")
                .defineInRange("footsoldier_speed", 0.20, 0.05, 1.0);
        CHAMPION_SPEED = BUILDER
                .comment("Champion (vindicator) movement speed. Lowered from vanilla 0.35 so they don't run players down.")
                .defineInRange("champion_speed", 0.20, 0.05, 1.0);
        GUARD_SPEED = BUILDER
                .comment("Guard (wither skeleton) movement speed.")
                .defineInRange("guard_speed", 0.20, 0.05, 1.0);
        FOOTSOLDIER_HP = BUILDER.comment("Footsoldier max health.").defineInRange("footsoldier_hp", 50.0, 1.0, 1024.0);
        CHAMPION_HP    = BUILDER.comment("Champion max health.").defineInRange("champion_hp", 50.0, 1.0, 1024.0);
        GUARD_HP       = BUILDER.comment("Guard max health.").defineInRange("guard_hp", 60.0, 1.0, 1024.0);
        CHAMPION_ATTACK = BUILDER.comment("Champion base attack damage (its axe adds more).")
                .defineInRange("champion_attack", 1.0, 0.0, 100.0);
        GUARD_ATTACK    = BUILDER.comment("Guard base attack damage (its sword + wither effect add more).")
                .defineInRange("guard_attack", 2.0, 0.0, 100.0);
        KNIGHT_SPAWN_CHANCE = BUILDER
                .comment("Chance (0-1) a knight spawns on each per-player attempt.")
                .defineInRange("spawn_chance", 0.25, 0.0, 1.0);
        KNIGHT_SPAWN_INTERVAL_TICKS = BUILDER
                .comment("Server ticks between ambient knight spawn attempts per player. 1000 = 50s.")
                .defineInRange("spawn_interval_ticks", 1000, 20, 24000);
        KNIGHT_MAX_NEAR_PLAYER = BUILDER
                .comment("Max knights near a player before further ambient spawns are skipped.")
                .defineInRange("max_knights_near_player", 3, 0, 50);
        BUILDER.pop();

        BUILDER.comment("THE MARKED / bounty pacing. A bounty is a rare, time-boxed hunt so no one "
                + "gets ganged up on and eliminated early.").push("bounty");
        BOUNTY_CONTRACT_MINUTES = BUILDER
                .comment("Minutes a bounty stays active before it expires unclaimed — the target is "
                        + "only hunted this long, then they're safe.")
                .defineInRange("contract_minutes", 5, 1, 120);
        BOUNTY_SELECTION_COOLDOWN_MINUTES = BUILDER
                .comment("Minutes after a bounty is ASSIGNED before another can be selected. Keeps "
                        + "bounties spaced out. 0 = a new bounty can be picked as soon as the last ends.")
                .defineInRange("selection_cooldown_minutes", 60, 0, 600);
        BOUNTY_RESELECT_PLAYER_MINUTES = BUILDER
                .comment("Minutes a player who just held the bounty is excluded from being picked "
                        + "again — prevents the same player being targeted back-to-back.")
                .defineInRange("reselect_same_player_minutes", 120, 0, 1200);
        BOUNTY_THREAT_THRESHOLD = BUILDER
                .comment("Threat score a player must EXCEED to be eligible for a bounty.")
                .defineInRange("threat_threshold", 5, 0, 1000);
        THREAT_PER_KILL = BUILDER.comment("Threat gained per player kill.").defineInRange("threat_per_kill", 2, 0, 100);
        THREAT_PER_ASSIST = BUILDER.comment("Threat gained per kill assist.").defineInRange("threat_per_assist", 1, 0, 100);
        THREAT_PER_FOOTSOLDIER = BUILDER.comment("Threat gained for slaying a Footsoldier.").defineInRange("threat_per_footsoldier", 1, 0, 100);
        THREAT_PER_CHAMPION = BUILDER.comment("Threat gained for slaying a Champion.").defineInRange("threat_per_champion", 2, 0, 100);
        THREAT_PER_GUARD = BUILDER.comment("Threat gained for slaying a Guard.").defineInRange("threat_per_guard", 3, 0, 100);
        BUILDER.pop();

        BUILDER.comment("Combat-item power. Tune the custom arsenal without recompiling.").push("combat");
        LAUNCH_PAD_VERTICAL = BUILDER.comment("Launch Pad vertical pop (blocks/tick velocity).")
                .defineInRange("launch_pad_vertical", 1.6, 0.1, 10.0);
        LAUNCH_PAD_FORWARD = BUILDER.comment("Launch Pad horizontal kick in the direction of travel/facing. "
                        + "Horizontal velocity bleeds off to ~0.91x per tick in air, so distance scales "
                        + "less than linearly with this — expect a big number to feel smaller than it "
                        + "reads. Pair with a LOWER launch_pad_vertical for a flatter, longer arc.")
                .defineInRange("launch_pad_forward", 2.8, 0.0, 10.0);
        YOINK_MAX_PULL = BUILDER.comment("Yoink Rod max reel-in speed (blocks/tick).")
                .defineInRange("yoink_max_pull", 4.0, 0.1, 10.0);
        BOLA_ROOT_TICKS = BUILDER.comment("How long a Bola roots its target. 80 = 4s.")
                .defineInRange("bola_root_ticks", 80, 1, 600);
        BOLA_COOLDOWN_TICKS = BUILDER.comment("Bola use cooldown. 100 = 5s.")
                .defineInRange("bola_cooldown_ticks", 100, 0, 600);
        BOLA_RANGE = BUILDER.comment("Bola throw/lock range in blocks.")
                .defineInRange("bola_range", 24.0, 1.0, 64.0);
        BULWARK_DAMAGE_REDUCTION = BUILDER.comment("Fraction (0-1) of incoming damage the Bulwark Legguards absorb.")
                .defineInRange("bulwark_damage_reduction", 0.20, 0.0, 0.9);
        STORM_BRAND_THUNDER_VOLUME = BUILDER
                .comment("Volume of the Storm Brand's thunder. Audible range is roughly volume x 16 "
                        + "blocks, so 4.0 = ~64 blocks. Vanilla lightning uses 10000 (server-wide), "
                        + "which is why the Storm Brand replaces it with a local sound on the PLAYERS "
                        + "channel instead of WEATHER. 0 = silent.")
                .defineInRange("storm_brand_thunder_volume", 4.0, 0.0, 100.0);
        BUILDER.pop();

        BUILDER.comment("The King's ranged-attack pacing. Each cooldown is a MINIMUM; a random "
                + "spread is added on top so his rhythm isn't metronomic.").push("king");
        KING_LIGHTNING_COOLDOWN = BUILDER
                .comment("Minimum ticks between telegraphed lightning storms (a random 0-140 is added). "
                        + "240 = 12-19s. Lower = more frequent.")
                .defineInRange("lightning_cooldown_ticks", 240, 20, 2400);
        KING_HEX_COOLDOWN = BUILDER
                .comment("Minimum ticks between hex casts (a random 0-120 is added). 140 = 7-13s.")
                .defineInRange("hex_cooldown_ticks", 140, 20, 2400);
        BUILDER.pop();

        BUILDER.comment("Content placed by data markers inside custom structures.").push("structures");
        STRUCTURE_CRATE_CHANCE = BUILDER
                .comment("Chance (0-1) each 'kcs:crate' data marker becomes a crate (else cleared to air). "
                        + "Rolled per marker, so crate placement varies between generations.")
                .defineInRange("crate_marker_chance", 0.5, 0.0, 1.0);
        SPAWNER_SPAWN_RANGE = BUILDER
                .comment("Horizontal radius (blocks) a structure spawner places mobs within — vanilla is 4. "
                        + "Raise this so mobs can appear OUTSIDE a cramped structure instead of stalling.")
                .defineInRange("spawner_spawn_range", 8, 1, 32);
        SPAWNER_MAX_NEARBY = BUILDER
                .comment("How many of its own mob may already be nearby before a spawner pauses (vanilla 6).")
                .defineInRange("spawner_max_nearby_entities", 6, 1, 64);
        SPAWNER_PLAYER_RANGE = BUILDER
                .comment("How close a player must be for a structure spawner to activate (vanilla 16).")
                .defineInRange("spawner_required_player_range", 16, 1, 128);
        BUILDER.pop();

        // repeat_interval_ticks defaults (0 = fire once and stop):
        //   6000  =  5 min  |  9000  = 7.5 min  |  12000 = 10 min  |  18000 = 15 min
        // Threshold pacing (deaths needed = threshold x players x lives):
        //   broken 0.04 -> fires on the first death or two, so the action starts immediately
        //   common 0.25 / rare 0.50 / epic 0.70 -> evenly spaced rungs
        // Epic at 0.70 leaves a real gap before the King (who rises at 3 lives left), so the
        // epic loot actually gets used: gap = 1.5 x players - 3 deaths.
        BROKEN = new TierConfig(BUILDER, "broken", 0.04, 22000, 75, 12000);

        COMMON = new TierConfig(BUILDER, "common", 0.25, 21000, 100, 15000);

        RARE = new TierConfig(BUILDER, "rare", 0.50, 19000, 125, 18000);

        EPIC = new TierConfig(BUILDER, "epic", 0.70, 18000, 150, 24000);
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    // -------------------------------------------------------------------------
    // Inner class — per-tier config block
    // -------------------------------------------------------------------------

    public static class TierConfig {

        /**
         * Fraction (0.0–1.0) of total possible player deaths that must be reached to unlock this tier.
         * Formula: {@code sum(min(deaths, 5) per online player) / (playerCount × 5)}.
         */
        public final ModConfigSpec.DoubleValue thresholdPercent;

        /**
         * How many ticks between repeat drops once this tier is unlocked.
         * Set to 0 to fire only once (on threshold cross) and never repeat.
         * 6000 = 5 min, 12000 = 10 min, 24000 = 20 min.
         */
        public final ModConfigSpec.IntValue repeatIntervalTicks;

        /**
         * Radius (blocks) by which to shrink the world border when this tier first triggers.
         * Set to 0 to skip the border shrink for this tier entirely.
         */
        public final ModConfigSpec.IntValue borderShrinkRadius;

        /**
         * How many seconds the border-shrink transition takes.
         * The border lerps smoothly from its current size to the new target over this duration.
         */
        public final ModConfigSpec.IntValue borderShrinkSeconds;

        TierConfig(ModConfigSpec.Builder builder, String name, double defaultThreshold,
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
