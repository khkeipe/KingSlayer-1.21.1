package com.koreykeipe.kingslayer.airdrop;

import com.koreykeipe.kingslayer.KingSlayer;
import com.koreykeipe.kingslayer.game.GameManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Singleton that tracks which airdrop tiers have fired and handles both
 * threshold-based first-fires and configurable repeating intervals.
 *
 * <h3>Progress formula</h3>
 * <pre>
 *   progress = sum( min(deaths, 5) for each online player ) / ( playerCount × 5 )
 * </pre>
 *
 * <h3>Interval behaviour</h3>
 * Once a tier is unlocked (threshold crossed), it fires immediately. If
 * {@code repeat_interval_ticks > 0} in the config, it will keep firing every
 * N ticks thereafter for the rest of the session. Set the interval to 0 for
 * one-shot behaviour.
 *
 * <h3>Restart safety</h3>
 * On the first death after a server restart, a silent pre-marking pass marks
 * any tier whose threshold is already exceeded <em>without</em> spawning a drop.
 * The interval timer for pre-marked tiers starts from that moment, so the first
 * repeat happens N ticks after the restart, not instantly.
 */
public class AirdropManager {

    private static final AirdropManager INSTANCE = new AirdropManager();

    /** Tiers that have been unlocked (threshold crossed or manually triggered). */
    private final Set<AirdropTier> triggeredTiers = EnumSet.noneOf(AirdropTier.class);

    /**
     * Server tick ({@link MinecraftServer#getTickCount()}) when each tier last fired a drop.
     * Used to gate the repeat interval.  Pre-marked tiers are recorded here without firing.
     */
    private final Map<AirdropTier, Integer> lastFireTick = new EnumMap<>(AirdropTier.class);

    /** Guards the one-time pre-marking pass on the first death after a restart. */
    private boolean initialized = false;

    /**
     * UUID → expiry server-tick for temporary glowing ArmorStand markers placed over chests.
     * Populated by {@link #trackGlowMarker} and cleaned up each tick.
     */
    private final Map<UUID, Integer> glowMarkers = new HashMap<>();

    /** Half-width of the square spawn area centred on the world origin (blocks). */
    private static final int SPAWN_SPREAD = 200;

    private AirdropManager() {}

    public static AirdropManager get() { return INSTANCE; }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /** Called when the server starts. Resets all state for a fresh game. */
    public void onServerStarted(MinecraftServer server) {
        triggeredTiers.clear();
        lastFireTick.clear();
        glowMarkers.clear();
        initialized = false;
    }

    // -------------------------------------------------------------------------
    // Death hook — called after every player death
    // -------------------------------------------------------------------------

    public void onPlayerDeath(MinecraftServer server) {
        if (!AirdropConfig.ENABLED.get()) return;
        if (!GameManager.get().isGameActive()) return;

        double progress = calculateProgress(server);

        // One-time pre-marking pass: silently mark already-exceeded tiers without dropping.
        // Interval timers start from this tick so repeats don't fire immediately on restart.
        if (!initialized) {
            initialized = true;
            int currentTick = server.getTickCount();
            for (AirdropTier tier : AirdropTier.values()) {
                if (progress >= configFor(tier).thresholdPercent.get()) {
                    triggeredTiers.add(tier);
                    lastFireTick.put(tier, currentTick);
                    KingSlayer.LOGGER.info(
                            "KingSlayer Airdrop: pre-marking {} (progress {}).",
                            tier.getDisplayName(), String.format("%.1f%%", progress * 100));
                }
            }
            return;
        }

        // Fire drops for any newly-crossed thresholds
        for (AirdropTier tier : AirdropTier.values()) {
            if (triggeredTiers.contains(tier)) continue;
            if (progress >= configFor(tier).thresholdPercent.get()) {
                triggeredTiers.add(tier);
                spawnAirdrop(server, tier, String.format("%.0f%% progress", progress * 100));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Tick hook — called every server tick to drive repeating intervals
    // -------------------------------------------------------------------------

    public void onServerTick(MinecraftServer server) {
        if (!AirdropConfig.ENABLED.get()) return;
        if (!GameManager.get().isGameActive()) return;
        if (triggeredTiers.isEmpty()) return; // cheap early exit before any tier is unlocked

        int currentTick = server.getTickCount();

        for (AirdropTier tier : AirdropTier.values()) {
            if (!triggeredTiers.contains(tier)) continue;

            int intervalTicks = configFor(tier).repeatIntervalTicks.get();
            if (intervalTicks <= 0) continue; // one-shot mode — no repeat

            int lastFire = lastFireTick.getOrDefault(tier, currentTick - intervalTicks - 1);
            if (currentTick - lastFire >= intervalTicks) {
                spawnAirdrop(server, tier, null); // null label = repeat drop, no tag in chat
            }
        }

        // Expire glowing chest markers
        if (!glowMarkers.isEmpty()) {
            Iterator<Map.Entry<UUID, Integer>> it = glowMarkers.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Integer> entry = it.next();
                if (currentTick >= entry.getValue()) {
                    it.remove();
                    for (ServerLevel level : server.getAllLevels()) {
                        Entity entity = level.getEntity(entry.getKey());
                        if (entity != null) {
                            entity.discard();
                            break;
                        }
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Manual trigger (operator command)
    // -------------------------------------------------------------------------

    /**
     * Registers a glowing ArmorStand marker to be removed after {@code GLOW_DURATION} ticks.
     * Called by {@link AirdropEntity} immediately after the marker is spawned.
     */
    public void trackGlowMarker(UUID entityId, MinecraftServer server) {
        glowMarkers.put(entityId, server.getTickCount() + AirdropConfig.GLOW_DURATION.get());
    }

    /**
     * Bypasses threshold checks and spawns a drop immediately.
     * Also unlocks the repeat interval for the tier (useful for testing repeats).
     */
    public void triggerManual(MinecraftServer server, AirdropTier tier) {
        triggeredTiers.add(tier); // enable the repeat timer even if threshold wasn't met
        spawnAirdrop(server, tier, "manual");
    }

    // -------------------------------------------------------------------------
    // Core spawn logic
    // -------------------------------------------------------------------------

    /**
     * Picks a random surface location, broadcasts the incoming warning, and
     * spawns an {@link AirdropEntity}.
     *
     * @param label  Text shown in parentheses in the chat announcement, e.g.
     *               {@code "35% progress"} or {@code "manual"}.
     *               Pass {@code null} for silent repeat drops (no tag shown).
     */
    private void spawnAirdrop(MinecraftServer server, AirdropTier tier, @Nullable String label) {
        ServerLevel overworld = server.overworld();

        int x        = overworld.random.nextIntBetweenInclusive(-SPAWN_SPREAD, SPAWN_SPREAD);
        int z        = overworld.random.nextIntBetweenInclusive(-SPAWN_SPREAD, SPAWN_SPREAD);
        int surfaceY = overworld.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        int spawnY   = surfaceY + AirdropConfig.SPAWN_HEIGHT.get();

        // Record fire time before spawning so the interval is measured from this moment
        lastFireTick.put(tier, server.getTickCount());

        // Broadcast warning
        String tag = label != null ? " §7(" + label + ")" : "";
        GameManager.get().broadcast("§6§l☆ " + tier.coloredName() + " §r§6§lis incoming!" + tag);

        // Spawn entity
        AirdropEntity entity = new AirdropEntity(tier, overworld, x + 0.5, spawnY, z + 0.5);
        overworld.addFreshEntity(entity);

        KingSlayer.LOGGER.info("KingSlayer Airdrop: spawned {} at ({}, {}, {})",
                tier.getDisplayName(), x, spawnY, z);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private double calculateProgress(MinecraftServer server) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return 0.0;

        int totalDeaths = 0;
        for (ServerPlayer player : players) {
            int deaths = player.getStats().getValue(Stats.CUSTOM, Stats.DEATHS);
            totalDeaths += Math.min(deaths, 5);
        }
        return (double) totalDeaths / ((double) players.size() * 5.0);
    }

    private AirdropConfig.TierConfig configFor(AirdropTier tier) {
        return switch (tier) {
            case COMMON    -> AirdropConfig.COMMON;
            case RARE      -> AirdropConfig.RARE;
            case EPIC      -> AirdropConfig.EPIC;
            case LEGENDARY -> AirdropConfig.LEGENDARY;
        };
    }
}
