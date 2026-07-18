package com.koreykeipe.kingslayer.game;

import com.koreykeipe.kingslayer.KingSlayer;
import com.koreykeipe.kingslayer.airdrop.AirdropConfig;
import com.koreykeipe.kingslayer.airdrop.BorderManager;
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
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.neoforge.client.event.sound.SoundEvent;

import javax.annotation.Nullable;
import java.util.*;

public class GameManager {

    private static final GameManager INSTANCE = new GameManager();

    private MinecraftServer server;
    private boolean gameActive = false;

    /** Persistent event state (roster, per-player deaths, finale flags) — survives restarts. */
    private KsWorldData state;

    /** Lives each player starts with (config-driven); elimination latches at this death count. */
    private static int maxDeaths() { return AirdropConfig.LIVES.get(); }

    // -------------------------------------------------------------------------
    // Kill tracking
    // -------------------------------------------------------------------------

    /** Total player kills credited to each UUID this session. */
    private final Map<UUID, Integer> killCounts   = new HashMap<>();

    /**
     * Composite threat score driving The Marked election.
     * Player kill = 2 pts. Assist = 1 pt. Knight kills add 1/2/3 via {@link #awardThreatScore}.
     */
    private final Map<UUID, Integer> threatScores = new HashMap<>();

    /** Fingerprint of the last standings posted, so an unchanged board isn't reprinted. */
    private String lastLeaderboardSignature = null;

    /** Kill assists credited to each UUID this session (for stats / leaderboard). */
    private final Map<UUID, Integer> assistCounts = new HashMap<>();

    // -------------------------------------------------------------------------
    // Marked / Bounty
    // -------------------------------------------------------------------------

    @Nullable private UUID currentMarkedUUID = null;

    /** Game-tick the current bounty contract lapses if it hasn't been claimed. */
    private long markedExpiresTick = 0L;
    /** Game-tick before which no NEW bounty may be elected — a short breather after one ENDS. */
    private long nextMarkedAllowedTick = 0L;
    /** Per-player tick until which a player who just held the bounty (killed or expired) can't be re-marked. */
    private final Map<UUID, Long> markedCooldownUntil = new HashMap<>();

    // Bounty pacing is config-driven (config/kcs_kingslayer-server.toml → [bounty]); minutes → ticks.
    private static long contractTicks()          { return AirdropConfig.BOUNTY_CONTRACT_MINUTES.get() * 1200L; }
    private static long selectionCooldownTicks() { return AirdropConfig.BOUNTY_SELECTION_COOLDOWN_MINUTES.get() * 1200L; }
    private static long reselectPlayerTicks()    { return AirdropConfig.BOUNTY_RESELECT_PLAYER_MINUTES.get() * 1200L; }

    // -------------------------------------------------------------------------

    private GameManager() {}

    public static GameManager get() { return INSTANCE; }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    public void onServerStarted(MinecraftServer server) {
        this.server = server;
        // Load the persistent event roster/deaths. Continuous tournament: the game is over
        // only if a victor was already declared — otherwise it resumes right where it left off.
        this.state = KsWorldData.get(server.overworld());

        // Per-session scoreboards (kills/threat/assists/Marked/kill-feed) reset each session;
        // the win-critical roster + death counts live in `state` and persist.
        killCounts.clear();
        threatScores.clear();
        lastLeaderboardSignature = null;
        assistCounts.clear();
        currentMarkedUUID = null;
        markedCooldownUntil.clear();
        nextMarkedAllowedTick = 0L;
        markedExpiresTick = 0L;
        CombatLog.clear();
        CombatTracker.clear();

        gameActive = !state.concluded;
        KingSlayer.LOGGER.info("KingSlayer {} — {} participants, {} still alive.",
                state.concluded ? "event already concluded" : "event resumed/started",
                state.participants.size(), aliveCount());
    }

    public void onPlayerLogin(ServerPlayer player) {
        if (state == null) return;
        UUID uuid = player.getUUID();
        state.names.put(uuid, player.getName().getString());

        // Eliminated players rejoin as spectators no matter when they reconnect.
        if (isEliminated(uuid)) {
            player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            state.setDirty();
            return;
        }
        if (!gameActive) { state.setDirty(); return; }

        if (state.participants.add(uuid)) {
            broadcast("§6KingSlayer §a" + player.getName().getString()
                    + " §ehas entered the arena. §7(" + aliveCount() + " players)");
        }
        state.setDirty();
    }

    // -------------------------------------------------------------------------
    // Roster helpers — "alive" = a participant who hasn't hit maxDeaths(), online or not.
    // -------------------------------------------------------------------------

    public int getDeaths(UUID uuid)        { return state == null ? 0 : state.deaths.getOrDefault(uuid, 0); }
    public boolean isEliminated(UUID uuid) { return getDeaths(uuid) >= maxDeaths(); }
    public boolean isAlive(UUID uuid)      { return state != null && state.participants.contains(uuid) && !isEliminated(uuid); }

    /** Max lives a player starts with (used by the setlives command bounds). */
    public int maxLives() { return maxDeaths(); }

    // -------------------------------------------------------------------------
    // Test simulation — inject fake roster members so player-count features can be
    // exercised solo (/kssim). Simulated players are deterministic offline participants.
    // -------------------------------------------------------------------------

    /** Deterministic offline UUID for simulated player #i (1-based). */
    private static UUID simUuid(int i) {
        return UUID.nameUUIDFromBytes(("kingslayer-sim-" + i).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public int simulatedCount() { return state == null ? 0 : state.simulatedCount; }

    /**
     * Adjusts the number of simulated roster members to exactly {@code target}. Added sims join
     * as alive (0-death) participants and expand the border like real first-joins; removed sims
     * are stripped from the roster. Returns the new simulated count.
     */
    public int setSimulatedPlayerCount(MinecraftServer server, int target) {
        if (state == null) return 0;
        target = Math.max(0, target);
        int old = state.simulatedCount;
        for (int i = old + 1; i <= target; i++) {        // grow
            UUID id = simUuid(i);
            state.participants.add(id);
            state.names.put(id, "SimPlayer" + i);
            state.deaths.putIfAbsent(id, 0);
            if (markBorderContributor(id)) BorderManager.get().onNewPlayerFirstJoin(server);
        }
        for (int i = target + 1; i <= old; i++) {        // shrink
            UUID id = simUuid(i);
            state.participants.remove(id);
            state.deaths.remove(id);
            state.names.remove(id);
            state.borderContributors.remove(id);
        }
        state.simulatedCount = target;
        state.setDirty();
        return target;
    }

    /** Spreads {@code total} deaths across the simulated players (round-robin, each capped at maxDeaths()). */
    public int setSimulatedDeaths(int total) {
        if (state == null || state.simulatedCount == 0) return 0;
        int n = state.simulatedCount;
        for (int i = 1; i <= n; i++) state.deaths.put(simUuid(i), 0); // reset sim deaths first
        int target = Math.max(0, Math.min(total, n * maxDeaths()));
        int applied = 0;
        while (applied < target) {
            state.deaths.merge(simUuid((applied % n) + 1), 1, Integer::sum);
            applied++;
        }
        state.setDirty();
        return applied;
    }

    /** Removes all simulated players from the roster (does not shrink the world border). */
    public void clearSimulatedPlayers(MinecraftServer server) {
        setSimulatedPlayerCount(server, 0);
    }

    /**
     * Puts a player on the correct name-colour team: the dedicated {@code marked_team}
     * (☠ skull + dark-red name) while they're THE MARKED, otherwise their lives-count colour.
     * Single source of truth so respawn/relog/marking all agree on the nametag.
     */
    public void assignNameTagTeam(ServerPlayer player) {
        if (server == null) return;
        Scoreboard sb = server.getScoreboard();
        String teamName;
        if (player.getUUID().equals(currentMarkedUUID)) {
            teamName = "marked_team";
        } else {
            // Map deaths onto the colour ramp scaled to the configured life total, so any
            // lives count works: full = aqua, last life = red, eliminated = gray.
            int max = maxDeaths();
            int deaths = getDeaths(player.getUUID());
            if (deaths >= max) {
                teamName = "gray_team";          // eliminated
            } else if (deaths <= 0) {
                teamName = "aqua_team";          // full lives
            } else if (deaths >= max - 1) {
                teamName = "red_team";           // last life
            } else {
                double frac = (double) deaths / (double) (max - 1); // 0..1 toward elimination
                teamName = frac < 0.34 ? "green_team" : (frac < 0.67 ? "lime_team" : "yello_team");
            }
        }
        PlayerTeam team = sb.getPlayerTeam(teamName);
        if (team != null) sb.addPlayerToTeam(player.getScoreboardName(), team);
    }

    /**
     * Records that a player has expanded the world border on their first-ever join. Tracked by
     * UUID in persistent save data (not player NBT, which is wiped on death), so the border
     * doesn't re-grow when a player relogs after dying. Returns true only the FIRST time.
     */
    public boolean markBorderContributor(UUID uuid) {
        if (state == null) return false;
        boolean isNew = state.borderContributors.add(uuid);
        if (isNew) state.setDirty();
        return isNew;
    }

    /**
     * Admin override: set a player's remaining lives directly (the real life counter that the
     * death-progress + tier pacing read). Updates the persisted death count and restores or
     * removes spectator mode accordingly. The caller refreshes the name-colour team afterwards.
     */
    public void setLives(ServerPlayer player, int lives) {
        if (state == null) return;
        UUID uuid = player.getUUID();
        int deaths = Math.max(0, Math.min(maxDeaths(), maxDeaths() - lives));
        state.participants.add(uuid);
        state.names.put(uuid, player.getName().getString());
        state.deaths.put(uuid, deaths);
        state.setDirty();
        if (deaths >= maxDeaths()) {
            player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
        } else if (player.isSpectator()) {
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        }
    }

    public int aliveCount() {
        if (state == null) return 0;
        int n = 0;
        for (UUID p : state.participants) if (!isEliminated(p)) n++;
        return n;
    }

    /** Total lives left across the WHOLE roster (each participant starts with maxDeaths()). */
    public int remainingLives() {
        if (state == null) return 0;
        int total = 0;
        for (UUID p : state.participants) total += Math.max(0, maxDeaths() - getDeaths(p));
        return total;
    }

    /** Normalised event death progress (0–1) over the FULL roster — player-count independent. */
    public double eventDeathProgress() {
        if (state == null || state.participants.isEmpty()) return 0.0;
        int total = 0;
        for (UUID p : state.participants) total += Math.min(getDeaths(p), maxDeaths());
        return (double) total / ((double) state.participants.size() * maxDeaths());
    }

    public boolean isKingSummoned()        { return state != null && state.kingSummoned; }
    public void    markKingSummoned()      { if (state != null) { state.kingSummoned = true; state.setDirty(); } }

    /**
     * Records one death for a player (the single source of truth for lives). Returns the new
     * count. When it crosses {@link #maxDeaths()} the player is eliminated and the win condition
     * is re-checked. Called from the death hook before airdrop progress is read.
     */
    public int recordDeath(ServerPlayer player) {
        if (state == null) return 0;
        UUID uuid = player.getUUID();
        state.participants.add(uuid);
        state.names.put(uuid, player.getName().getString());
        int count = state.deaths.merge(uuid, 1, Integer::sum);
        state.setDirty();
        if (count == maxDeaths()) {   // exact crossing — eliminate once
            onPlayerEliminated(player);
        }
        return count;
    }

    // -------------------------------------------------------------------------
    // Kill hook
    // -------------------------------------------------------------------------

    public void onPlayerKilled(ServerPlayer victim, CombatTracker.KillAttribution attribution) {
        if (!gameActive) return;
        UUID uuid = victim.getUUID();
        if (state == null || !state.participants.contains(uuid)) return;

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

        // If The Marked was just killed, pay out the bounty before re-electing. Start a cooldown
        // (both global and on the fallen player) so a fresh bounty doesn't drop instantly.
        if (uuid.equals(currentMarkedUUID)) {
            currentMarkedUUID = null;
            long now = server.overworld().getGameTime();
            markedExpiresTick = 0L;
            markedCooldownUntil.put(uuid, now + reselectPlayerTicks()); // slain target can't be re-marked back-to-back
            // nextMarkedAllowedTick stays as set at assignment — the 1-hour spacing runs from assignment.
            handleMarkedKilled(uuid, attribution.killerUUID(), attribution.killerName());
        }

        // Credit the kill and re-evaluate The Marked
        if (attribution.killerUUID() != null) {
            killCounts.merge(attribution.killerUUID(), 1, Integer::sum);
            threatScores.merge(attribution.killerUUID(), AirdropConfig.THREAT_PER_KILL.get(), Integer::sum);
        }

        // Reward assists — partial threat to everyone who contributed, so the fight
        // isn't all about landing the final blow.
        for (AssistEntry assist : attribution.assists()) {
            UUID au = assist.playerUUID();
            if (au == null || au.equals(uuid) || au.equals(attribution.killerUUID())) continue;
            assistCounts.merge(au, 1, Integer::sum);
            threatScores.merge(au, AirdropConfig.THREAT_PER_ASSIST.get(), Integer::sum);
            ServerPlayer assister = server.getPlayerList().getPlayer(au);
            if (assister != null) {
                assister.displayClientMessage(Component.literal(
                        "§7+1 threat §8(assist on " + victim.getName().getString() + ")"), true);
            }
        }

        computeMarked();
    }

    // -------------------------------------------------------------------------
    // Elimination / win condition
    // -------------------------------------------------------------------------

    /**
     * Called once when a player crosses {@link #maxDeaths()} (from {@link #recordDeath}).
     * Elimination itself is derived from the persisted death count; this handles the
     * win-condition check, milestones, and victory/draw — over the FULL roster, so players
     * who are merely offline still count as alive.
     */
    public void onPlayerEliminated(ServerPlayer player) {
        if (!gameActive || state == null) return;
        if (player.getUUID().equals(currentMarkedUUID)) {
            currentMarkedUUID = null;
        }

        int remaining = aliveCount();

        if (remaining <= 1) {
            UUID winnerId = state.participants.stream().filter(p -> !isEliminated(p)).findFirst().orElse(null);
            if (remaining == 1 && winnerId != null) {
                ServerPlayer winner = server.getPlayerList().getPlayer(winnerId); // may be offline
                String winnerName = winner != null ? winner.getName().getString()
                        : state.names.getOrDefault(winnerId, "Unknown");
                announceVictory(winner, winnerName);
            } else {
                broadcast("§6KingSlayer §cNo survivors — it's a draw!");
            }
            state.concluded = true;
            state.setDirty();
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

        // Skip the post entirely when nothing has moved since last time. Quiet stretches
        // (everyone looting, nobody fighting) used to reprint an identical board on every
        // interval, which is what made the standings feel like spam.
        String signature = top.stream()
                .map(e -> e.getKey() + ":" + e.getValue()
                        + ":" + assistCounts.getOrDefault(e.getKey(), 0)
                        + ":" + threatScores.getOrDefault(e.getKey(), 0))
                .collect(java.util.stream.Collectors.joining("|"))
                + "#" + aliveCount();
        if (signature.equals(lastLeaderboardSignature)) return;
        lastLeaderboardSignature = signature;

        broadcast("§6======= KingSlayer Standings =======");
        int rank = 1;
        for (Map.Entry<UUID, Integer> e : top) {
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            String name  = p != null ? p.getName().getString() : "Unknown";
            int    score   = threatScores.getOrDefault(e.getKey(), 0);
            int    assists = assistCounts.getOrDefault(e.getKey(), 0);
            broadcast("  §e" + rank++ + ". §a" + name
                    + " §7— " + e.getValue() + " kills · " + assists + " assists  §8(threat: " + score + ")");
        }
        broadcast("  §7" + aliveCount() + " players remain.");
        broadcast("§6=====================================");
    }

    // -------------------------------------------------------------------------
    // Marked / Bounty — internal
    // -------------------------------------------------------------------------

    /**
     * Lapses the bounty contract if it ran its full duration unclaimed: clears the Marked,
     * restores their name colour, gives them a re-mark reprieve, and opens a short breather
     * before the next bounty. Called periodically from the server tick.
     */
    public void tickBounty() {
        if (server == null || currentMarkedUUID == null) return;
        long now = server.overworld().getGameTime();
        if (now < markedExpiresTick) return;

        UUID expired = currentMarkedUUID;
        currentMarkedUUID = null;
        markedExpiresTick = 0L;
        markedCooldownUntil.put(expired, now + reselectPlayerTicks());
        // nextMarkedAllowedTick stays as set at assignment — spacing is measured from assignment.

        ServerPlayer p = server.getPlayerList().getPlayer(expired);
        if (p != null) assignNameTagTeam(p);
        String name = (state != null && state.names.containsKey(expired)) ? state.names.get(expired) : "The Marked";
        broadcast("§7☠ §eThe bounty on §c" + name + " §ehas expired — they outlasted the contract!");
    }

    private void computeMarked() {
        if (server == null) return;

        // A bounty is locked once assigned — it never transfers; it only ends by kill or expiry.
        if (currentMarkedUUID != null) return;

        long now = server.overworld().getGameTime();
        // Selection cooldown measured from the LAST assignment — keeps bounties rare/spaced.
        if (now < nextMarkedAllowedTick) return;

        UUID newMarked = threatScores.entrySet().stream()
                .filter(e -> isAlive(e.getKey()))
                .filter(e -> e.getValue() > AirdropConfig.BOUNTY_THREAT_THRESHOLD.get())
                .filter(e -> now >= markedCooldownUntil.getOrDefault(e.getKey(), 0L)) // not the last target (back-to-back)
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        if (newMarked == null) return;

        currentMarkedUUID = newMarked;
        markedExpiresTick     = now + contractTicks();          // target is hunted only this long
        nextMarkedAllowedTick = now + selectionCooldownTicks(); // no other bounty until this elapses

        ServerPlayer markedPlayer = server.getPlayerList().getPlayer(newMarked);
        if (markedPlayer == null) return;
        assignNameTagTeam(markedPlayer); // ☠ skull + dark-red name while marked

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
        if (state != null) { state.concluded = true; state.setDirty(); }
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

    public Set<UUID> getAlivePlayers() {
        Set<UUID> alive = new HashSet<>();
        if (state != null) {
            for (UUID p : state.participants) if (!isEliminated(p)) alive.add(p);
        }
        return Collections.unmodifiableSet(alive);
    }
    public Map<UUID, Integer> getKillCounts()   { return Collections.unmodifiableMap(killCounts); }
    public Map<UUID, Integer> getThreatScores() { return Collections.unmodifiableMap(threatScores); }
}
