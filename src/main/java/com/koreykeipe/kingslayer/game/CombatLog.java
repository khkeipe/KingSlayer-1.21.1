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
        StringBuilder sb = new StringBuilder("§6KingSlayer §r");

        if (entry.killerName() != null) {
            if (entry.indirect()) {
                sb.append(victim).append(" §7died to §e").append(entry.cause())
                  .append(" §7(last hit by §a").append(entry.killerName()).append("§7)");
            } else {
                sb.append(victim).append(" §7was killed by §a").append(entry.killerName());
            }
        } else {
            sb.append(victim).append(" §7died to §e").append(entry.cause());
        }

        List<AssistEntry> assists = entry.assists();
        if (!assists.isEmpty()) {
            sb.append(" §8[");
            for (int i = 0; i < assists.size(); i++) {
                if (i > 0) sb.append("§8, ");
                AssistEntry a = assists.get(i);
                sb.append("§7").append(a.playerName()).append(" §8(").append(a.contribution()).append("§8)");
            }
            sb.append("§8]");
        }

        return sb.toString();
    }
}
