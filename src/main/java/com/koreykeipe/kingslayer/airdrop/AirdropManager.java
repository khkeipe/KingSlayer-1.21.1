package com.koreykeipe.kingslayer.airdrop;

import com.koreykeipe.kingslayer.KingSlayer;
import com.koreykeipe.kingslayer.game.GameManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
     * Chest block position → server tick at which the smoke column should stop.
     * Populated by {@link #trackChest} on landing; entries expire naturally each tick.
     * No in-world entity is needed — AirdropManager already ticks every server tick.
     */
    private final Map<BlockPos, Integer> activeChests = new HashMap<>();

    private AirdropManager() {}

    public static AirdropManager get() { return INSTANCE; }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /** Called when the server starts. Resets all state for a fresh game. */
    public void onServerStarted(MinecraftServer server) {
        triggeredTiers.clear();
        lastFireTick.clear();
        activeChests.clear();
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
                BorderManager.get().onTierTriggered(server, tier);
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

        // Emit a rising smoke column above each active chest (every 8 ticks ≈ 2.5 puffs/second)
        if (!activeChests.isEmpty() && currentTick % 8 == 0) {
            ServerLevel overworld = server.overworld();
            for (Map.Entry<BlockPos, Integer> entry : activeChests.entrySet()) {
                if (currentTick >= entry.getValue()) continue; // expired — skip, removed below
                BlockPos pos = entry.getKey();
                // Two offset puffs per interval for a natural-looking column
                for (int i = 0; i < 2; i++) {
                    double ox = (overworld.random.nextFloat() - 0.5f) * 0.25;
                    double oz = (overworld.random.nextFloat() - 0.5f) * 0.25;
                    // count=0 mode: xDist/yDist/zDist are treated as the velocity vector
                    overworld.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                            pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                            0, ox * 0.05, 0.12, oz * 0.05, 1.0);
                }
            }
        }

        // Remove expired chest entries (no entity to clean up — the map IS the timer)
        if (!activeChests.isEmpty()) {
            activeChests.entrySet().removeIf(entry -> currentTick >= entry.getValue());
        }
    }

    // -------------------------------------------------------------------------
    // Manual trigger (operator command)
    // -------------------------------------------------------------------------

    /**
     * Registers a chest position for smoke-particle emission for {@link AirdropConfig#GLOW_DURATION}
     * ticks. Called by {@link AirdropEntity} immediately after the chest is placed on landing.
     * No in-world entity is needed — the expiry is tracked entirely in this map.
     */
    public void trackChest(BlockPos chestPos, MinecraftServer server) {
        activeChests.put(chestPos, server.getTickCount() + AirdropConfig.GLOW_DURATION.get());
    }

    /**
     * Bypasses threshold checks and spawns a drop immediately.
     * Also unlocks the repeat interval for the tier (useful for testing repeats).
     */
    public void triggerManual(MinecraftServer server, AirdropTier tier) {
        triggeredTiers.add(tier); // enable the repeat timer even if threshold wasn't met
        spawnAirdrop(server, tier, "manual");
        BorderManager.get().onTierTriggered(server, tier);
    }

    // -------------------------------------------------------------------------
    // Core spawn logic
    // -------------------------------------------------------------------------

    /**
     * Picks a random surface location inside the current world border, announces
     * the drop via screen title + sound (first-fires only), and spawns the entity.
     *
     * @param label  Non-null on the first fire of a tier (e.g. {@code "35% progress"}
     *               or {@code "manual"}); {@code null} for silent repeat drops.
     */
    private void spawnAirdrop(MinecraftServer server, AirdropTier tier, String label) {
        ServerLevel overworld = server.overworld();

        // Pick a random surface position inside the current world border (8-block inset
        // keeps drops away from the wall even while the border is shrinking).
        WorldBorder border = overworld.getWorldBorder();
        final int BORDER_INSET = 8;
        int minX = (int) Math.ceil(border.getMinX())  + BORDER_INSET;
        int maxX = (int) Math.floor(border.getMaxX()) - BORDER_INSET;
        int minZ = (int) Math.ceil(border.getMinZ())  + BORDER_INSET;
        int maxZ = (int) Math.floor(border.getMaxZ()) - BORDER_INSET;
        // Guard against a border that is too small to give a valid range
        if (minX > maxX) { minX = maxX = (int) border.getCenterX(); }
        if (minZ > maxZ) { minZ = maxZ = (int) border.getCenterZ(); }

        int x        = overworld.random.nextIntBetweenInclusive(minX, maxX);
        int z        = overworld.random.nextIntBetweenInclusive(minZ, maxZ);
        int surfaceY = overworld.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        int spawnY   = surfaceY + AirdropConfig.SPAWN_HEIGHT.get();

        // Record fire time before spawning so the interval is measured from this moment
        lastFireTick.put(tier, server.getTickCount());

        // Announce via screen title + sound (first-fire only; repeats are silent)
        if (label != null) {
            announceIncoming(server, tier);
        }

        // Spawn entity
        AirdropEntity entity = new AirdropEntity(tier, overworld, x + 0.5, spawnY, z + 0.5);
        overworld.addFreshEntity(entity);

        KingSlayer.LOGGER.info("KingSlayer Airdrop: spawned {} at ({}, {}, {})",
                tier.getDisplayName(), x, spawnY, z);
    }

    /**
     * Sends a screen title, subtitle, and notification sound to every online player.
     * The sound is sent directly as a packet at each player's location so it is
     * guaranteed audible regardless of world-border size or player spread.
     */
    private void announceIncoming(MinecraftServer server, AirdropTier tier) {
        ChatFormatting tierColor = switch (tier) {
            case BROKEN -> ChatFormatting.GRAY;
            case COMMON -> ChatFormatting.GREEN;
            case RARE   -> ChatFormatting.BLUE;
            case EPIC   -> ChatFormatting.DARK_PURPLE;
        };

        Component title = Component.literal("✦  AIRDROP  ✦")
                .withStyle(style -> style.withColor(ChatFormatting.GOLD).withBold(true));
        Component subtitle = Component.literal(tier.getDisplayName() + " Airdrop is incoming!")
                .withStyle(style -> style.withColor(tierColor).withBold(false).withItalic(false));

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // Timing: 0.5 s fade-in | 3.5 s hold | 1 s fade-out
            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
            player.connection.send(new ClientboundSetTitleTextPacket(title));
            player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
            // Challenge-complete ding — sent directly so every player hears it
            server.overworld().getLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RAID_HORN, SoundSource.AMBIENT, 1.5f, 1.6f);
        }

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
            case BROKEN -> AirdropConfig.BROKEN;
            case COMMON -> AirdropConfig.COMMON;
            case RARE   -> AirdropConfig.RARE;
            case EPIC   -> AirdropConfig.EPIC;
        };
    }
}
