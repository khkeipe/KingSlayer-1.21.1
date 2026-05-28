package com.koreykeipe.kingslayer.airdrop;

import com.koreykeipe.kingslayer.KingSlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.border.WorldBorder;

import java.util.EnumSet;
import java.util.Set;

/**
 * Singleton that manages dynamic world-border scaling for the KingSlayer game.
 *
 * <h3>Expansion (first-time player join)</h3>
 * Each player who joins for the <em>very first time ever</em> adds one
 * {@code initial_blocks_per_player} radius slot to the border.
 * The caller ({@code ModEvents}) is responsible for the first-time check via
 * the {@code hasJoinedBefore} persistent-data flag; this manager only does the
 * resize and has no session-dedup of its own.
 *
 * <ul>
 *   <li>Player #1 joins → border = 1 × blocksPerPlayer × 2 (diameter)</li>
 *   <li>Player #2 joins → border = 2 × blocksPerPlayer × 2</li>
 *   <li>…and so on until all expected players have connected.</li>
 * </ul>
 *
 * <h3>Shrinking (tier trigger)</h3>
 * When an airdrop tier fires for the <em>first time</em>, the border smoothly
 * lerps down by that tier's configured {@code border_shrink_radius} over
 * {@code border_shrink_seconds} seconds. Repeat drops for the same tier do
 * NOT trigger additional shrinks. Shrinks stack: RARE starts from wherever
 * COMMON left off.
 *
 * <h3>Restart safety</h3>
 * Minecraft saves the actual border size in {@code level.dat}, so the physical
 * border survives server restarts. On start-up this manager reads that saved
 * size back as {@code targetDiameter}, meaning tier shrinks continue to stack
 * correctly from the correct baseline after a restart.
 * If the border is still at the Minecraft default (≥ 1 000 000 blocks) it is
 * treated as unconfigured and will be initialised when the first new player joins.
 */
public class BorderManager {

    private static final BorderManager INSTANCE = new BorderManager();
    public static BorderManager get() { return INSTANCE; }

    /** Threshold above which the border is considered "not yet configured by this mod". */
    private static final double UNCONFIGURED_THRESHOLD = 1_000_000.0;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /**
     * Working target diameter. Updated on every player-join expansion and tier shrink.
     * Seeded from the saved border size on server start so restarts don't reset stacking.
     */
    private double targetDiameter = UNCONFIGURED_THRESHOLD;

    /** Tiers whose border shrink has already been applied. Guards against double-shrink. */
    private final Set<AirdropTier> shrunkenTiers = EnumSet.noneOf(AirdropTier.class);

    private BorderManager() {}

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Called from {@code ModEvents.onServerStarted}.
     * Seeds {@code targetDiameter} from the level-saved border so that restart
     * restores the correct baseline. Also clears the tier-shrink guard so that
     * the new session can apply tier shrinks from the pre-marking pass if needed.
     */
    public void onServerStarted(MinecraftServer server) {
        shrunkenTiers.clear();

        double savedSize = server.overworld().getWorldBorder().getSize();
        // If the border is at the Minecraft default it has never been touched by this mod.
        // Set targetDiameter to 0 so the first player-join initialises it correctly.
        targetDiameter = (savedSize >= UNCONFIGURED_THRESHOLD) ? 0.0 : savedSize;

        KingSlayer.LOGGER.info("KingSlayer Border: restored targetDiameter={} from saved border size={}",
                (int) targetDiameter, (int) savedSize);
    }

    // -------------------------------------------------------------------------
    // First-time player join — expand border
    // -------------------------------------------------------------------------

    /**
     * Call this when a player joins for the very first time (i.e. {@code hasJoinedBefore}
     * was absent from their persistent data). Expands the border by one
     * {@code initial_blocks_per_player} radius slot.
     */
    public void onNewPlayerFirstJoin(MinecraftServer server) {
        if (!AirdropConfig.BORDER_ENABLED.get()) return;

        int blocksPerPlayer = AirdropConfig.INITIAL_BLOCKS_PER_PLAYER.get();
        targetDiameter += blocksPerPlayer * 2.0;

        WorldBorder border = server.overworld().getWorldBorder();
        border.setSize(targetDiameter);

        KingSlayer.LOGGER.info("KingSlayer Border: new player joined — diameter → {} ({} radius)",
                (int) targetDiameter, (int)(targetDiameter / 2));
    }

    // -------------------------------------------------------------------------
    // Tier trigger — shrink border
    // -------------------------------------------------------------------------

    /**
     * Called by {@link AirdropManager} when a tier fires for the first time.
     * Initiates a smooth border shrink. Idempotent — calling it again for the
     * same tier (e.g. from a repeat drop or manual re-trigger) is a no-op.
     */
    public void onTierTriggered(MinecraftServer server, AirdropTier tier) {
        if (!AirdropConfig.BORDER_ENABLED.get()) return;
        if (shrunkenTiers.contains(tier)) return; // already shrunk for this tier, ignore
        shrunkenTiers.add(tier);

        AirdropConfig.TierConfig cfg = configFor(tier);
        int shrinkRadius  = cfg.borderShrinkRadius.get();
        int shrinkSeconds = cfg.borderShrinkSeconds.get();

        if (shrinkRadius <= 0) {
            KingSlayer.LOGGER.info("KingSlayer Border: [{}] border_shrink_radius = 0, skipping.",
                    tier.getDisplayName());
            return;
        }

        double minDiameter = AirdropConfig.BORDER_MIN_RADIUS.get() * 2.0;
        double newTarget   = Math.max(targetDiameter - (shrinkRadius * 2.0), minDiameter);

        if (newTarget >= targetDiameter) return; // already at or below floor

        WorldBorder border = server.overworld().getWorldBorder();
        double currentSize = border.getSize(); // real-time diameter, may be mid-lerp
        long   durationMs  = (long) shrinkSeconds * 1000L;

        border.lerpSizeBetween(currentSize, newTarget, durationMs);
        targetDiameter = newTarget;

        KingSlayer.LOGGER.info(
                "KingSlayer Border: [{}] shrinking {} → {} (-{} radius) over {}s",
                tier.getDisplayName(), (int) currentSize, (int) newTarget, shrinkRadius, shrinkSeconds);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private AirdropConfig.TierConfig configFor(AirdropTier tier) {
        return switch (tier) {
            case BROKEN -> AirdropConfig.BROKEN;
            case COMMON -> AirdropConfig.COMMON;
            case RARE   -> AirdropConfig.RARE;
            case EPIC   -> AirdropConfig.EPIC;
        };
    }
}
