package com.koreykeipe.kingslayer.game;

import com.koreykeipe.kingslayer.KingSlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class GameManager {
    private static final GameManager INSTANCE = new GameManager();

    private MinecraftServer server;
    private final LinkedHashSet<UUID> alivePlayers = new LinkedHashSet<>();
    private final Set<UUID> eliminatedPlayers = new HashSet<>();
    private boolean gameActive = false;

    private GameManager() {}

    public static GameManager get() {
        return INSTANCE;
    }

    public void onServerStarted(MinecraftServer server) {
        this.server = server;
        alivePlayers.clear();
        eliminatedPlayers.clear();
        CombatLog.clear();
        CombatTracker.clear();
        gameActive = true;
        KingSlayer.LOGGER.info("[KingSlayer] Game initialized — waiting for players to join.");
    }

    public void onPlayerLogin(ServerPlayer player) {
        if (!gameActive) return;
        UUID uuid = player.getUUID();
        if (eliminatedPlayers.contains(uuid)) return;
        if (alivePlayers.add(uuid)) {
            broadcast("§6[KingSlayer] §a" + player.getName().getString()
                    + " §ehas entered the arena. §7(" + alivePlayers.size() + " players)");
        }
    }

    public void onPlayerDeath(ServerPlayer victim, @Nullable String killerName, String cause, boolean indirect) {
        if (!gameActive) return;
        UUID uuid = victim.getUUID();
        if (!alivePlayers.remove(uuid)) return;
        eliminatedPlayers.add(uuid);

        KillEntry entry = new KillEntry(victim.getName().getString(), killerName, cause, indirect, System.currentTimeMillis());
        CombatLog.logKill(entry, server);

        int remaining = alivePlayers.size();
        if (remaining == 1) {
            UUID winnerId = alivePlayers.iterator().next();
            ServerPlayer winner = server.getPlayerList().getPlayer(winnerId);
            String winnerName = winner != null ? winner.getName().getString() : "Unknown";
            broadcast("§6[KingSlayer] §a§l" + winnerName + " §r§ewins the KingSlayer! §7("
                    + CombatLog.getEntries().size() + " kills total)");
            gameActive = false;
        } else if (remaining == 0) {
            broadcast("§6[KingSlayer] §cNo survivors! It's a draw!");
            gameActive = false;
        } else {
            broadcast("§7" + remaining + " players remain.");
        }
    }

    public void broadcast(String message) {
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(Component.literal(message), false);
        }
    }

    public boolean isGameActive() { return gameActive; }

    public Set<UUID> getAlivePlayers() { return Collections.unmodifiableSet(alivePlayers); }
}
