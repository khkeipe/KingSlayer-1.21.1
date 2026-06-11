package com.koreykeipe.kingslayer.game;

import com.koreykeipe.kingslayer.KingSlayer;
import com.koreykeipe.kingslayer.block.ModBlocks;
import com.koreykeipe.kingslayer.item.ModItems;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraftforge.client.event.sound.SoundEvent;

import javax.annotation.Nullable;
import java.util.*;

public class GameManager {

    private static final GameManager INSTANCE = new GameManager();

    private MinecraftServer server;
    private final LinkedHashSet<UUID> alivePlayers     = new LinkedHashSet<>();
    private final Set<UUID>           eliminatedPlayers = new HashSet<>();
    private boolean gameActive = false;

    // -------------------------------------------------------------------------
    // Kill tracking
    // -------------------------------------------------------------------------

    /** Total player kills credited to each UUID this session. */
    private final Map<UUID, Integer> killCounts   = new HashMap<>();

    /**
     * Composite threat score driving The Marked election.
     * Player kill = 2 pts. Knight kills add 1/2/3 via {@link #awardThreatScore}.
     */
    private final Map<UUID, Integer> threatScores = new HashMap<>();

    // -------------------------------------------------------------------------
    // Marked / Bounty
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

        // First blood — must check before logKill so the list is still empty
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

        // If The Marked was just killed, pay out the bounty before re-electing
        if (uuid.equals(currentMarkedUUID)) {
            currentMarkedUUID = null;
            handleMarkedKilled(uuid, attribution.killerUUID(), attribution.killerName());
        }

        // Credit the kill and re-evaluate The Marked
        if (attribution.killerUUID() != null) {
            killCounts.merge(attribution.killerUUID(), 1, Integer::sum);
            threatScores.merge(attribution.killerUUID(), 2, Integer::sum);
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

        if (uuid.equals(currentMarkedUUID)) {
            currentMarkedUUID = null;
        }

        int remaining = alivePlayers.size();

        if (remaining == 1) {
            UUID winnerId = alivePlayers.iterator().next();
            ServerPlayer winner = server.getPlayerList().getPlayer(winnerId);
            String winnerName = winner != null ? winner.getName().getString() : "Unknown";
            announceVictory(winner, winnerName);
            gameActive = false;

        } else if (remaining == 0) {
            broadcast("§6KingSlayer §cNo survivors — it's a draw!");
            gameActive = false;

        } else {
            broadcast("§7" + remaining + " players remain.");

            // Milestone title broadcasts
            if (remaining == 5) {
                announceMilestone("FINAL FIVE",      "Only 5 players remain!");
            } else if (remaining == 3) {
                announceMilestone("FINAL THREE",     "Only 3 players remain!");
            } else if (remaining == 2) {
                announceMilestone("FINAL SHOWDOWN",  "Only 2 players remain — fight to the end!");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Threat score — called externally by KnightSpawnHandler
    // -------------------------------------------------------------------------

    /**
     * Awards threat-score points to a player and re-evaluates The Marked.
     * Point guide: Footsoldier = 1, Champion = 2, Guard = 3, player kill = 2 (internal).
     */
    public void awardThreatScore(UUID playerUUID, int points) {
        if (!gameActive) return;
        threatScores.merge(playerUUID, points, Integer::sum);
        computeMarked();
    }

    // -------------------------------------------------------------------------
    // Leaderboard — called every 3 min from ModEvents (task 12)
    // -------------------------------------------------------------------------

    public void broadcastLeaderboard() {
        if (!gameActive || killCounts.isEmpty()) return;

        List<Map.Entry<UUID, Integer>> top = killCounts.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(3)
                .toList();

        broadcast("§6======= KingSlayer Standings =======");
        int rank = 1;
        for (Map.Entry<UUID, Integer> e : top) {
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            String name  = p != null ? p.getName().getString() : "Unknown";
            int    score = threatScores.getOrDefault(e.getKey(), 0);
            broadcast("  §e" + rank++ + ". §a" + name
                    + " §7— " + e.getValue() + " kills  §8(threat: " + score + ")");
        }
        broadcast("  §7" + alivePlayers.size() + " players remain.");
        broadcast("§6=====================================");
    }

    // -------------------------------------------------------------------------
    // Marked / Bounty — internal
    // -------------------------------------------------------------------------

    private void computeMarked() {
        if (server == null) return;

        UUID newMarked = threatScores.entrySet().stream()
                .filter(e -> alivePlayers.contains(e.getKey()))
                .filter(e -> e.getValue() > 5)
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
            boolean isTarget = p.getUUID().equals(newMarked);
            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
            if (isTarget) {
                // The hunted player gets a message aimed squarely at them.
                p.connection.send(new ClientboundSetTitleTextPacket(
                        Component.literal("☠  YOU ARE THE MARKED  ☠")
                                .withStyle(s -> s.withColor(ChatFormatting.DARK_RED).withBold(true))));
                p.connection.send(new ClientboundSetSubtitleTextPacket(
                        Component.literal("Every player can hunt you — stay alive!")
                                .withStyle(s -> s.withColor(ChatFormatting.RED).withItalic(false))));
            } else {
                p.connection.send(new ClientboundSetTitleTextPacket(
                        Component.literal("☠  BOUNTY PLACED  ☠")
                                .withStyle(s -> s.withColor(ChatFormatting.DARK_RED).withBold(true))));
                p.connection.send(new ClientboundSetSubtitleTextPacket(
                        Component.literal(name + " is now THE MARKED")
                                .withStyle(s -> s.withColor(ChatFormatting.RED).withItalic(false))));
            }
        }

        // Everyone hears the hunt is on...
        broadcast("§c☠ §e" + name + " §cis THE MARKED §7(threat: "
                + score + ") §c— hunt them down for a Bounty Crate!");

        // ...and The Marked gets a direct, personal warning in chat.
        markedPlayer.sendSystemMessage(Component.literal(
                "§4☠ §cA bounty has been placed on YOU! §7Every player can now hunt you for a Bounty Crate. §cStay alive!"));
    }

    private void handleMarkedKilled(UUID markedUUID, @Nullable UUID killerUUID, @Nullable String killerName) {
        // No reward for suicides or environmental deaths — the bounty must be *earned*
        // by another player. Self-kills (void, lava, /kill, fall, nether gas) leave it unclaimed.
        if (killerUUID == null || killerUUID.equals(markedUUID)) {
            broadcast("§7☠ §eThe Marked has fallen by their own hand — the bounty goes unclaimed.");
            return;
        }
        if (killerName == null) return; // killed by a non-player with no credit

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

        if (killerUUID != null) {
            ServerPlayer killer = server.getPlayerList().getPlayer(killerUUID);
            if (killer != null) {
                // Hand the Bounty Crate straight to the earner so the reward can't be
                // stolen — they place it and break it to claim the loot, wherever they choose.
                ItemStack crate = new ItemStack(ModBlocks.BOUNTY_CRATE.get());
                if (!killer.getInventory().add(crate)) {
                    killer.drop(crate, false); // inventory full — drop at their feet
                }
                killer.sendSystemMessage(Component.literal(
                        "§6⚔ §eYou received a §6§lBounty Crate§r§e — place it and break it to claim your reward!"));
            }
        }
    }

    // -------------------------------------------------------------------------
    // First blood
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
                + " §7drew first blood on §c" + victimName + "§7!");
    }

    // -------------------------------------------------------------------------
    // Milestone broadcasts (task 10)
    // -------------------------------------------------------------------------

    private void announceMilestone(String titleText, String subtitleText) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
            p.connection.send(new ClientboundSetTitleTextPacket(
                    Component.literal(titleText)
                            .withStyle(s -> s.withColor(ChatFormatting.RED).withBold(true))));
            p.connection.send(new ClientboundSetSubtitleTextPacket(
                    Component.literal(subtitleText)
                            .withStyle(s -> s.withColor(ChatFormatting.YELLOW).withItalic(false))));
        }
    }

    // -------------------------------------------------------------------------
    // Slay the King — alternate victory path
    // -------------------------------------------------------------------------

    /** Called by {@link com.koreykeipe.kingslayer.entity.TheKing} when the boss dies. */
    public void onKingSlain(@Nullable ServerPlayer slayer) {
        String name = slayer != null ? slayer.getName().getString() : "an unknown challenger";
        broadcast("§4§l☠ THE KING HAS FALLEN ☠");
        broadcast("§6⚔ §e" + name + " §6has slain The King and seized the throne!");

        if (!gameActive) return; // killed outside an active match — just the announcement

        if (slayer != null) {
            threatScores.merge(slayer.getUUID(), 10, Integer::sum);
            announceVictory(slayer, slayer.getName().getString());
        } else {
            announceVictory(null, name);
        }
        gameActive = false;
    }

    // -------------------------------------------------------------------------
    // Victory ceremony (task 11)
    // -------------------------------------------------------------------------

    private void announceVictory(@Nullable ServerPlayer winner, String winnerName) {
        // Title screen to all players
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(20, 120, 40));
            p.connection.send(new ClientboundSetTitleTextPacket(
                    Component.literal("WINNER")
                            .withStyle(s -> s.withColor(ChatFormatting.GOLD).withBold(true))));
            p.connection.send(new ClientboundSetSubtitleTextPacket(
                    Component.literal(winnerName + " is the KingSlayer!")
                            .withStyle(s -> s.withColor(ChatFormatting.YELLOW).withItalic(false))));
            // Raid horn at every player's location so everyone hears it
            p.connection.send(new ClientboundSoundPacket(
                    SoundEvents.RAID_HORN,
                    SoundSource.AMBIENT,
                    p.getX(), p.getY(), p.getZ(),
                    2.0f, 1.0f, p.getRandom().nextLong()));
        }

        // Fireworks at winner's location and Crown item drop
        if (winner != null) {
            spawnVictoryFireworks(winner.serverLevel(), winner);
            winner.drop(new ItemStack(ModItems.CROWN.get()), false);
        }

        // Final stats block in chat
        broadcastFinalStats(winnerName);
    }

    private static void spawnVictoryFireworks(ServerLevel level, ServerPlayer winner) {
        FireworkExplosion explosion = new FireworkExplosion(
                FireworkExplosion.Shape.LARGE_BALL,
                IntArrayList.of(0xFFD700, 0xFFFFFF), // gold + white
                new IntArrayList(),
                true,   // trail
                true    // twinkle
        );
        ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
        rocket.set(DataComponents.FIREWORKS, new Fireworks(1, List.of(explosion)));

        for (int i = 0; i < 12; i++) {
            double ox = (level.random.nextDouble() - 0.5) * 3.0;
            double oz = (level.random.nextDouble() - 0.5) * 3.0;
            FireworkRocketEntity fw = new FireworkRocketEntity(
                    level,
                    winner.getX() + ox,
                    winner.getY(),
                    winner.getZ() + oz,
                    rocket.copy());
            fw.setDeltaMovement(ox * 0.06,
                    0.4 + level.random.nextDouble() * 0.5,
                    oz * 0.06);
            level.addFreshEntity(fw);
        }
    }

    private void broadcastFinalStats(String winnerName) {
        int totalKills = CombatLog.getEntries().size();

        // Top killer by kill count
        String topKiller = killCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> {
                    ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
                    String name = p != null ? p.getName().getString() : "Unknown";
                    return name + " (" + e.getValue() + ")";
                })
                .orElse("—");

        // Top assist player — count appearances across all kill entries
        Map<String, Integer> assistTotals = new HashMap<>();
        for (KillEntry ke : CombatLog.getEntries()) {
            for (AssistEntry ae : ke.assists()) {
                assistTotals.merge(ae.playerName(), 1, Integer::sum);
            }
        }
        String topAssist = assistTotals.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey() + " (" + e.getValue() + ")")
                .orElse("—");

        broadcast("§6======= KingSlayer Final Stats =======");
        broadcast("  §6§lWinner:       §r§a" + winnerName);
        broadcast("  §6Most Kills:   §e" + topKiller);
        broadcast("  §6Most Assists: §e" + topAssist);
        broadcast("  §6Total Kills:  §e" + totalKills);
        broadcast("§6=====================================");
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public void broadcast(String message) {
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(Component.literal(message), false);
        }
    }

    public boolean isGameActive()      { return gameActive; }
    public MinecraftServer getServer() { return server; }

    public Set<UUID>          getAlivePlayers() { return Collections.unmodifiableSet(alivePlayers); }
    public Map<UUID, Integer> getKillCounts()   { return Collections.unmodifiableMap(killCounts); }
    public Map<UUID, Integer> getThreatScores() { return Collections.unmodifiableMap(threatScores); }
}
