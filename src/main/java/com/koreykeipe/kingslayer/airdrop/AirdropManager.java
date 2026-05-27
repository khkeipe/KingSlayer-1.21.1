package com.koreykeipe.kingslayer.airdrop;

import com.koreykeipe.kingslayer.KingSlayer;
import com.koreykeipe.kingslayer.game.GameManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Singleton that tracks which airdrop tiers have fired and
 * schedules new drops when the death-progress threshold is crossed.
 *
 * <h3>Progress formula</h3>
 * <pre>
 *   progress = sum( min(deaths, 5) for each online player ) / ( playerCount × 5 )
 * </pre>
 * Eliminated players remain online as spectators with deaths ≥ 5, so they
 * correctly contribute their full 5/5 to the numerator.
 *
 * <h3>Restart safety</h3>
 * On the first death after a server restart, {@code onPlayerDeath} performs a
 * one-time pre-marking pass: any tier whose threshold is already exceeded is
 * quietly marked as triggered <em>without</em> spawning a new drop.  Subsequent
 * deaths then only fire drops for newly-crossed thresholds.
 */
public class AirdropManager {

    private static final AirdropManager INSTANCE = new AirdropManager();

    private final Set<AirdropTier> triggeredTiers = EnumSet.noneOf(AirdropTier.class);
    private boolean initialized = false;

    /** Half-width of the square spawn area centred on the world origin (blocks). */
    private static final int SPAWN_SPREAD = 200;

    private AirdropManager() {}

    public static AirdropManager get() { return INSTANCE; }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Called when the server starts. Clears state so a fresh game begins cleanly.
     * Actual pre-marking of already-exceeded tiers is deferred until the first death
     * (at which point players are online and death counts are readable).
     */
    public void onServerStarted(MinecraftServer server) {
        triggeredTiers.clear();
        initialized = false;
    }

    // -------------------------------------------------------------------------
    // Death hook
    // -------------------------------------------------------------------------

    /**
     * Called after every player death. Recalculates progress and fires any
     * newly-crossed tier drops.
     */
    public void onPlayerDeath(MinecraftServer server) {
        if (!AirdropConfig.ENABLED.get()) return;
        if (!GameManager.get().isGameActive()) return;

        double progress = calculateProgress(server);

        // One-time pre-marking pass on the first death after a (re)start
        if (!initialized) {
            initialized = true;
            for (AirdropTier tier : AirdropTier.values()) {
                if (progress >= configFor(tier).thresholdPercent.get()) {
                    triggeredTiers.add(tier);
                    KingSlayer.LOGGER.info(
                            "KingSlayer Airdrop: pre-marking {} as already triggered (progress {}).",
                            tier.getDisplayName(), String.format("%.1f%%", progress * 100));
                }
            }
            return; // Don't fire drops on the pre-marking pass
        }

        // Check for newly-crossed thresholds
        for (AirdropTier tier : AirdropTier.values()) {
            if (triggeredTiers.contains(tier)) continue;
            if (progress >= configFor(tier).thresholdPercent.get()) {
                triggeredTiers.add(tier);
                spawnAirdrop(server, tier, progress);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the current death-progress fraction for all online players.
     * Each player's contribution is capped at 5 (their maximum lives).
     */
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

    /**
     * Manually spawns an airdrop of the given tier, bypassing threshold and triggered-tier checks.
     * Intended for operator testing via the /airdrop command.
     */
    public void triggerManual(MinecraftServer server, AirdropTier tier) {
        spawnAirdrop(server, tier, -1.0);
    }

    private void spawnAirdrop(MinecraftServer server, AirdropTier tier, double progress) {
        ServerLevel overworld = server.overworld();

        // Random spawn point near the world centre
        int x = overworld.random.nextIntBetweenInclusive(-SPAWN_SPREAD, SPAWN_SPREAD);
        int z = overworld.random.nextIntBetweenInclusive(-SPAWN_SPREAD, SPAWN_SPREAD);
        int surfaceY = overworld.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        int spawnY   = surfaceY + AirdropConfig.SPAWN_HEIGHT.get();

        // Incoming warning broadcast (progress < 0 means manually triggered)
        String progressTag = progress >= 0 ? " §7(" + String.format("%.0f%%", progress * 100) + " progress)" : " §7(manual)";
        GameManager.get().broadcast("§6§l☆ " + tier.coloredName() + " §r§6§lis incoming!" + progressTag);

        // Spawn the falling entity
        AirdropEntity entity = new AirdropEntity(tier, overworld, x + 0.5, spawnY, z + 0.5);
        overworld.addFreshEntity(entity);

        KingSlayer.LOGGER.info("KingSlayer Airdrop: spawned {} at ({}, {}, {})",
                tier.getDisplayName(), x, spawnY, z);
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
