package com.koreykeipe.kingslayer.game;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CombatLog {
    private static final List<KillEntry> entries = new ArrayList<>();

    public static void logKill(KillEntry entry, MinecraftServer server) {
        entries.add(entry);
        server.getPlayerList().broadcastSystemMessage(Component.literal(formatKill(entry)), false);
    }

    public static List<KillEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public static void clear() {
        entries.clear();
    }

    private static String formatKill(KillEntry entry) {
        String victim = "§c" + entry.victimName() + "§r";
        if (entry.killerName() != null) {
            if (entry.indirect()) {
                return "§6[KingSlayer] " + victim + " §7died to §e" + entry.cause()
                        + " §7(last hit by §a" + entry.killerName() + "§7)";
            } else {
                return "§6[KingSlayer] " + victim + " §7was killed by §a" + entry.killerName();
            }
        } else {
            return "§6[KingSlayer] " + victim + " §7died to §e" + entry.cause();
        }
    }
}
