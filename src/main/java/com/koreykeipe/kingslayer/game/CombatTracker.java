package com.koreykeipe.kingslayer.game;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatTracker {
    // Player must die within this window of the last hit for the attacker to get kill credit
    private static final long KILL_CREDIT_WINDOW_MS = 10_000;

    private static final Map<UUID, AttackRecord> lastAttackers = new HashMap<>();

    public record AttackRecord(UUID attackerUUID, String attackerName, long timestamp) {}

    public static void recordAttack(UUID victimUUID, UUID attackerUUID, String attackerName) {
        lastAttackers.put(victimUUID, new AttackRecord(attackerUUID, attackerName, System.currentTimeMillis()));
    }

    @Nullable
    public static AttackRecord getRecentAttacker(UUID victimUUID) {
        AttackRecord record = lastAttackers.get(victimUUID);
        if (record == null) return null;
        if (System.currentTimeMillis() - record.timestamp() > KILL_CREDIT_WINDOW_MS) {
            lastAttackers.remove(victimUUID);
            return null;
        }
        return record;
    }

    public static void clearPlayer(UUID victimUUID) {
        lastAttackers.remove(victimUUID);
    }

    public static void clear() {
        lastAttackers.clear();
    }
}
