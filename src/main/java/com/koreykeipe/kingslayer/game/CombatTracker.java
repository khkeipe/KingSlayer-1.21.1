package com.koreykeipe.kingslayer.game;

import javax.annotation.Nullable;
import java.util.*;

public class CombatTracker {
    // Direct hit within this window earns kill credit
    private static final long KILL_CREDIT_WINDOW_MS    = 10_000;
    // Look back this far when no recent hit exists (traps, environmental finishes)
    private static final long EXTENDED_CREDIT_WINDOW_MS = 120_000;
    // Contributions within this window count toward assist credit
    private static final long ASSIST_WINDOW_MS          = 60_000;
    // Attribution tags (traps, raid summons, etc.) stay valid this long
    private static final long ATTRIBUTION_TAG_WINDOW_MS = 300_000;

    private static final Map<UUID, List<DamageContribution>> damageHistory   = new HashMap<>();
    private static final Map<UUID, List<AttributionTag>>     attributionTags = new HashMap<>();

    // -------------------------------------------------------------------------
    // Data types
    // -------------------------------------------------------------------------

    /** A single damage hit from one player against a victim. */
    public record DamageContribution(
        UUID   attackerUUID,
        String attackerName,
        float  damage,
        long   timestamp
    ) {}

    /**
     * An indirect attribution tag registered by any game system.
     * Use this for traps, raid summons, knockback setups, or any future mechanic
     * where a player created the conditions that led to a kill.
     *
     * Priority guide:
     *   1-4  — informational / low-confidence (e.g. stray arrow from far away)
     *   5    — default (normal assist-level contribution)
     *   10   — intentional setup (trap placed deliberately, raid summoned)
     *   15+  — system override (reserved for mechanics that should always claim credit)
     */
    public record AttributionTag(
        UUID   playerUUID,
        String playerName,
        String reason,
        int    priority,
        long   timestamp
    ) {}

    /** Full attribution resolved at the moment of death. */
    public record KillAttribution(
        @Nullable String         killerName,
        @Nullable UUID           killerUUID,
        String                   cause,
        boolean                  indirect,
        List<AssistEntry>        assists
    ) {}

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Record damage dealt by one player against another. Call from LivingHurtEvent. */
    public static void recordDamage(UUID victimUUID, UUID attackerUUID, String attackerName, float damage) {
        damageHistory.computeIfAbsent(victimUUID, k -> new ArrayList<>())
            .add(new DamageContribution(attackerUUID, attackerName, damage, System.currentTimeMillis()));
    }

    /**
     * Register an indirect attribution for a player against a victim.
     * Call this from any game system that creates a threat — traps, raid summons,
     * environmental hazards the player deliberately set up, etc.
     *
     * @param reason   Human-readable label shown in the kill feed ("trap", "raid_summon", etc.)
     * @param priority See AttributionTag priority guide above
     */
    public static void registerAttribution(UUID victimUUID, UUID playerUUID, String playerName,
                                           String reason, int priority) {
        attributionTags.computeIfAbsent(victimUUID, k -> new ArrayList<>())
            .add(new AttributionTag(playerUUID, playerName, reason, priority, System.currentTimeMillis()));
    }

    /** Convenience overload — default priority 5. */
    public static void registerAttribution(UUID victimUUID, UUID playerUUID, String playerName, String reason) {
        registerAttribution(victimUUID, playerUUID, playerName, reason, 5);
    }

    /**
     * Resolve full kill attribution at the moment of death.
     *
     * Kill credit priority (when no direct player dealt the killing blow):
     *   1. Most recent damage hit within KILL_CREDIT_WINDOW (10s)
     *   2. Highest-priority AttributionTag within ATTRIBUTION_TAG_WINDOW (5 min)
     *   3. Most recent damage hit within EXTENDED_CREDIT_WINDOW (2 min)
     *   4. No player credit — pure environmental death
     *
     * @param directKillerUUID  UUID of the player whose attack dealt the killing blow, or null
     * @param directKillerName  Name of that player, or null
     * @param cause             getMsgId() from the DamageSource (e.g. "fall", "player", "lava")
     */
    public static KillAttribution resolveKill(UUID victimUUID,
                                              @Nullable UUID directKillerUUID,
                                              @Nullable String directKillerName,
                                              String cause) {
        long now = System.currentTimeMillis();
        List<DamageContribution> history = damageHistory.getOrDefault(victimUUID, Collections.emptyList());
        List<AttributionTag>     tags    = attributionTags.getOrDefault(victimUUID, Collections.emptyList());

        UUID   killerUUID = directKillerUUID;
        String killerName = directKillerName;
        boolean indirect  = (directKillerUUID == null);

        if (killerUUID == null) {
            // 1. Recent direct hit
            Optional<DamageContribution> recentHit = history.stream()
                .filter(d -> now - d.timestamp() <= KILL_CREDIT_WINDOW_MS)
                .max(Comparator.comparingLong(DamageContribution::timestamp));

            // 2. Best attribution tag (highest priority, then most recent)
            Optional<AttributionTag> bestTag = tags.stream()
                .filter(t -> now - t.timestamp() <= ATTRIBUTION_TAG_WINDOW_MS)
                .max(Comparator.comparingInt(AttributionTag::priority)
                    .thenComparingLong(AttributionTag::timestamp));

            // 3. Extended-window hit (2 min lookback)
            Optional<DamageContribution> extendedHit = history.stream()
                .filter(d -> now - d.timestamp() <= EXTENDED_CREDIT_WINDOW_MS)
                .max(Comparator.comparingLong(DamageContribution::timestamp));

            if (recentHit.isPresent()) {
                // Someone actively chasing them down gets credit over an older trap/tag
                killerUUID = recentHit.get().attackerUUID();
                killerName = recentHit.get().attackerName();
            } else if (bestTag.isPresent()) {
                killerUUID = bestTag.get().playerUUID();
                killerName = bestTag.get().playerName();
                cause      = bestTag.get().reason();
            } else if (extendedHit.isPresent()) {
                killerUUID = extendedHit.get().attackerUUID();
                killerName = extendedHit.get().attackerName();
            }
        }

        List<AssistEntry> assists = buildAssists(killerUUID, history, tags, now);
        return new KillAttribution(killerName, killerUUID, cause, indirect, assists);
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    public static void clearPlayer(UUID victimUUID) {
        damageHistory.remove(victimUUID);
        attributionTags.remove(victimUUID);
    }

    public static void clear() {
        damageHistory.clear();
        attributionTags.clear();
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private static List<AssistEntry> buildAssists(@Nullable UUID killerUUID,
                                                   List<DamageContribution> history,
                                                   List<AttributionTag> tags,
                                                   long now) {
        // Aggregate damage per attacker within assist window
        Map<UUID, Float>  damageByPlayer = new LinkedHashMap<>();
        Map<UUID, String> nameByPlayer   = new HashMap<>();
        float totalDamage = 0f;

        for (DamageContribution d : history) {
            if (now - d.timestamp() > ASSIST_WINDOW_MS) continue;
            damageByPlayer.merge(d.attackerUUID(), d.damage(), Float::sum);
            nameByPlayer.putIfAbsent(d.attackerUUID(), d.attackerName());
            totalDamage += d.damage();
        }

        List<AssistEntry> assists  = new ArrayList<>();
        Set<UUID>         recorded = new HashSet<>();
        final float total = totalDamage;

        // Damage-based assists, sorted by contribution descending, excluding killer
        damageByPlayer.entrySet().stream()
            .filter(e -> !e.getKey().equals(killerUUID))
            .sorted((a, b) -> Float.compare(b.getValue(), a.getValue()))
            .forEach(e -> {
                int pct = total > 0 ? Math.round(e.getValue() / total * 100) : 0;
                assists.add(new AssistEntry(nameByPlayer.get(e.getKey()), pct + "% damage"));
                recorded.add(e.getKey());
            });

        // Tag-based assists — players not already credited via damage
        tags.stream()
            .filter(t -> now - t.timestamp() <= ATTRIBUTION_TAG_WINDOW_MS)
            .filter(t -> !t.playerUUID().equals(killerUUID))
            .filter(t -> !recorded.contains(t.playerUUID()))
            .forEach(t -> {
                assists.add(new AssistEntry(t.playerName(), t.reason()));
                recorded.add(t.playerUUID());
            });

        return assists;
    }
}
