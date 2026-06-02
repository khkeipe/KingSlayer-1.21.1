package com.koreykeipe.kingslayer.game;

import com.koreykeipe.kingslayer.KingSlayer;
import com.koreykeipe.kingslayer.airdrop.AirdropEntity;
import com.koreykeipe.kingslayer.airdrop.AirdropTier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.*;

public class GameManager {

    private static final GameManager INSTANCE = new GameManager();

    private MinecraftServer server;
    private final LinkedHashSet<UUID> alivePlayers     = new LinkedHashSet<>();
    private final Set<UUID>           eliminatedPlayers = new HashSet<>();
    private boolean gameActive = false;

    // -------------------------------------------------------------------------
    // Kill tracking (task 2)
    // -------------------------------------------------------------------------

    /** Total player kills credited to each UUID this session. */
    private final Map<UUID, Integer> killCounts   = new HashMap<>();

    /**
     * Composite threat score used to elect The Marked.
     * Player kill = 2 pts. Knight kills add 1 / 2 / 3 pts via
     * {@link #awardThreatScore} from KnightSpawnHandler.
     */
    private final Map<UUID, Integer> threatScores = new HashMap<>();

    // -------------------------------------------------------------------------
    // Marked / Bounty (task 3)
    // -------------------------------------------------------------------------

    @Nullable private UUID currentMarkedUUID = null;

    // -------------------------------------------------------------------------

    private GameManager() {}

    public static GameManager get() { return INSTANCE; }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    public void onServerStarted(MinecraftServer server) {
        this.server = server;
        alivePlayers.clear();
        eliminatedPlayers.clear();
        killCounts.clear();
        threatScores.clear();
        currentMarkedUUID = null;
        CombatLog.clear();
        CombatTracker.clear();
        gameActive = true;
        KingSlayer.LOGGER.info("KingSlayer initialized — waiting for players to join.");
    }

    public void onPlayerLogin(ServerPlayer player) {
        if (!gameActive) return;
        UUID uuid = player.getUUID();
        if (eliminatedPlayers.contains(uuid)) return;
        if (alivePlayers.add(uuid)) {
            broadcast("§6KingSlayer §a" + player.getName().getString()
                    + " §ehas entered the arena. §7(" + alivePlayers.size() + " players)");
        }
    }

    // -------------------------------------------------------------------------
    // Kill hook
    // -------------------------------------------------------------------------

    public void onPlayerKilled(ServerPlayer victim, CombatTracker.KillAttribution attribution) {
        if (!gameActive) return;
        UUID uuid = victim.getUUID();
        if (!alivePlayers.contains(uuid) && !eliminatedPlayers.contains(uuid)) return;

        // Task 1 — First blood: check before logKill so the list is still empty
        boolean isFirstBlood = CombatLog.getEntries().isEmpty()
                && attribution.killerName() != null;

        KillEntry entry = new KillEntry(
                victim.getName().getString(),
                attribution.killerName(),
                attribution.cause(),
                attribution.indirect(),
                attribution.assists(),
                System.currentTimeMillis()
        );
        CombatLog.logKill(entry, server);

        if (isFirstBlood) {
            announceFirstBlood(attribution.killerName(), victim.getName().getString());
        }

        // Task 3 — if The Marked was just killed, pay out the bounty before re-electing
        if (uuid.equals(currentMarkedUUID)) {
            currentMarkedUUID = null;
            handleMarkedKilled(attribution.killerUUID(), attribution.killerName());
        }

        // Task 2 — credit the kill and re-evaluate The Marked
        if (attribution.killerUUID() != null) {
            killCounts.merge(attribution.killerUUID(), 1, Integer::sum);
            threatScores.merge(attribution.killerUUID(), 2, Integer::sum); // player kill = 2 pts
            computeMarked();
        }
    }

    // -------------------------------------------------------------------------
    // Elimination / win condition
    // -------------------------------------------------------------------------

    public void onPlayerEliminated(ServerPlayer player) {
        if (!gameActive) return;
        UUID uuid = player.getUUID();
        if (!alivePlayers.remove(uuid)) return;
        eliminatedPlayers.add(uuid);

        // Clear marked status so the next kill re-elects a new target
        if (uuid.equals(currentMarkedUUID)) {
            currentMarkedUUID = null;
        }

        int remaining = alivePlayers.size();
        if (remaining == 1) {
            UUID winnerId = alivePlayers.iterator().next();
            ServerPlayer winner = server.getPlayerList().getPlayer(winnerId);
            String winnerName = winner != null ? winner.getName().getString() : "Unknown";
            broadcast("§6KingSlayer §a§l" + winnerName + " §r§ewins the KingSlayer! §7("
                    + CombatLog.getEntries().size() + " kills total)");
            gameActive = false;
        } else if (remaining == 0) {
            broadcast("§6KingSlayer §cNo survivors! It's a draw!");
            gameActive = false;
        } else {
            broadcast("§7" + remaining + " players remain.");
        }
    }

    // -------------------------------------------------------------------------
    // Threat score — called externally by KnightSpawnHandler
    // -------------------------------------------------------------------------

    /**
     * Awards threat-score points to a player and re-evaluates The Marked.
     * Called from {@code KnightSpawnHandler} when a knight is killed.
     *
     * <p>Point guide: Footsoldier = 1 pt, Champion = 2 pts, Guard = 3 pts.
     * Player kills award 2 pts internally via {@link #onPlayerKilled}.</p>
     */
    public void awardThreatScore(UUID playerUUID, int points) {
        if (!gameActive) return;
        threatScores.merge(playerUUID, points, Integer::sum);
        computeMarked();
    }

    // -------------------------------------------------------------------------
    // Marked / Bounty — internal
    // -------------------------------------------------------------------------

    /**
     * Finds the alive player with the highest threat score. If they differ from
     * the current marked player, announces the shift to everyone.
     */
    private void computeMarked() {
        if (server == null) return;

        UUID newMarked = threatScores.entrySet().stream()
                .filter(e -> alivePlayers.contains(e.getKey()))
                .filter(e -> e.getValue() > 0)
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        if (newMarked == null || newMarked.equals(currentMarkedUUID)) return;

        currentMarkedUUID = newMarked;
        ServerPlayer markedPlayer = server.getPlayerList().getPlayer(newMarked);
        if (markedPlayer == null) return;

        String name  = markedPlayer.getName().getString();
        int    score = threatScores.getOrDefault(newMarked, 0);

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
            p.connection.send(new ClientboundSetTitleTextPacket(
                    Component.literal("☠  BOUNTY PLACED  ☠")
                            .withStyle(s -> s.withColor(ChatFormatting.DARK_RED).withBold(true))));
            p.connection.send(new ClientboundSetSubtitleTextPacket(
                    Component.literal(name + " is now THE MARKED")
                            .withStyle(s -> s.withColor(ChatFormatting.RED).withItalic(false))));
        }
        broadcast("§c☠ §e" + name + " §cis THE MARKED §7(threat score: "
                + score + ")§c — kill them for a bonus airdrop!");
    }

    /** Fires when The Marked is killed — announces and drops a bonus BROKEN airdrop on the killer. */
    private void handleMarkedKilled(@Nullable UUID killerUUID, @Nullable String killerName) {
        if (killerName == null) return;

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
            p.connection.send(new ClientboundSetTitleTextPacket(
                    Component.literal("⚔  BOUNTY CLAIMED  ⚔")
                            .withStyle(s -> s.withColor(ChatFormatting.GOLD).withBold(true))));
            p.connection.send(new ClientboundSetSubtitleTextPacket(
                    Component.literal(killerName + " has claimed the bounty!")
                            .withStyle(s -> s.withColor(ChatFormatting.YELLOW).withItalic(false))));
        }
        broadcast("§6⚔ §a" + killerName + " §ehas slain THE MARKED and claimed the bounty!");

        // Spawn a bonus BROKEN airdrop above the killer's current position
        if (killerUUID != null) {
            ServerPlayer killer = server.getPlayerList().getPlayer(killerUUID);
            if (killer != null) {
                ServerLevel level = killer.serverLevel();
                AirdropEntity bonus = new AirdropEntity(
                        AirdropTier.BROKEN, level,
                        killer.getX(), killer.getY() + 80, killer.getZ());
                level.addFreshEntity(bonus);
            }
        }
    }

    // -------------------------------------------------------------------------
    // First blood — internal (task 1)
    // -------------------------------------------------------------------------

    private void announceFirstBlood(String killerName, String victimName) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(5, 50, 15));
            p.connection.send(new ClientboundSetTitleTextPacket(
                    Component.literal("FIRST BLOOD")
                            .withStyle(s -> s.withColor(ChatFormatting.DARK_RED).withBold(true))));
            p.connection.send(new ClientboundSetSubtitleTextPacket(
                    Component.literal(killerName + " drew first blood on " + victimName)
                            .withStyle(s -> s.withColor(ChatFormatting.RED).withItalic(false))));
        }
        broadcast("§4§lFIRST BLOOD! §r§c" + killerName
                + " §7has drawn first blood on §c" + victimName + "§7!");
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public void broadcast(String message) {
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(Component.literal(message), false);
        }
    }

    public boolean isGameActive()    { return gameActive; }
    public MinecraftServer getServer() { return server; }

    public Set<UUID>           getAlivePlayers() { return Collections.unmodifiableSet(alivePlayers); }
    public Map<UUID, Integer>  getKillCounts()   { return Collections.unmodifiableMap(killCounts); }
    public Map<UUID, Integer>  getThreatScores() { return Collections.unmodifiableMap(threatScores); }
}
