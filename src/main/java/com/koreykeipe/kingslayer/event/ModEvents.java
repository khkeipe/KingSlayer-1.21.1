package com.koreykeipe.kingslayer.event;

import com.koreykeipe.kingslayer.KingSlayer;
import com.koreykeipe.kingslayer.airdrop.AirdropManager;
import com.koreykeipe.kingslayer.airdrop.BorderManager;
import com.koreykeipe.kingslayer.command.AirdropCommand;
import com.koreykeipe.kingslayer.command.DeathCommand;
import com.koreykeipe.kingslayer.game.CombatTracker;
import com.koreykeipe.kingslayer.event.KnightSpawnHandler;
import com.koreykeipe.kingslayer.game.GameManager;
import com.koreykeipe.kingslayer.game.TrapTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.UUID;

@EventBusSubscriber(modid = KingSlayer.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ModEvents {

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event){
        if(event.getEntity() instanceof ServerPlayer player){
            GameManager.get().onPlayerLogin(player);

            // First-EVER join expands the border once. Tracked by UUID in the persistent game
            // data (not player NBT, which is wiped on death) so a relog-after-death no longer
            // re-grows the border.
            if (GameManager.get().markBorderContributor(player.getUUID())) {
                net.minecraft.server.MinecraftServer srv = player.getServer();
                if (srv != null) {
                    BorderManager.get().onNewPlayerFirstJoin(srv);
                }
                player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("Welcome to ")
                        .append(Component.literal("King Slayer").withStyle(ChatFormatting.GOLD))
                ));
            } else {
                player.sendSystemMessage(Component.literal("Welcome Back o/"));
            }

            // Always reflect the player's CURRENT life count on their name colour on every login,
            // so a relog never appears to reset them to full lives.
            updateDeaths(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        // Reassign the life-count team AFTER respawn (not in Clone): at Clone time the
        // respawning player's connection isn't ready yet, so the team packet reaches other
        // clients but not their own — which is why their own name stayed teal to themselves.
        if (event.getEntity() instanceof ServerPlayer player) {
            updateDeaths(player);
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event){
        DeathCommand.register(event.getDispatcher());
        AirdropCommand.register(event.getDispatcher());
        com.koreykeipe.kingslayer.command.KingCommand.register(event.getDispatcher());
        com.koreykeipe.kingslayer.command.SimCommand.register(event.getDispatcher());
    }

    public static void updateDeaths(ServerPlayer player){
        // Lives are driven by the persistent event death counter (incremented in onLivingDeath),
        // so they survive restarts. This hook just reflects the count on respawn/login.
        int max = GameManager.get().maxLives();
        int deaths = GameManager.get().getDeaths(player.getUUID());
        int lives = max - deaths;

        // Name colour is owned by GameManager (it also applies the ☠ marked override).
        GameManager.get().assignNameTagTeam(player);

        if (lives <= 0) { // eliminated (the win-condition check already ran in recordDeath)
            player.setGameMode(GameType.SPECTATOR);
            player.sendSystemMessage(Component.literal("THANKS FOR PLAYING KING SLAYER"));
            return;
        }

        // Lives-aware message + colour, so any configured life total reads sensibly.
        ChatFormatting color = lives == 1 ? ChatFormatting.RED
                : (lives <= Math.max(1, max / 2) ? ChatFormatting.YELLOW
                : (lives < max ? ChatFormatting.GREEN : ChatFormatting.AQUA));
        net.minecraft.network.chat.MutableComponent msg = Component.literal("You Have ")
                .append(Component.literal(String.valueOf(lives)).withStyle(color))
                .append(Component.literal(lives == 1 ? " Life Remaining" : " Lives Remaining")
                        .withStyle(ChatFormatting.RESET));
        if (lives == 1) {
            msg.append(Component.literal(". . . MAKE IT COUNT!").withStyle(ChatFormatting.DARK_RED));
        }
        player.sendSystemMessage(msg);
    }
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        GameManager.get().onServerStarted(event.getServer());
        AirdropManager.get().onServerStarted(event.getServer());
        BorderManager.get().onServerStarted(event.getServer());
        com.koreykeipe.kingslayer.exchange.TributeStoneShrine.ensureAtSpawn(event.getServer());

        // First-time world setup: default keepInventory ON so deaths don't reset progress.
        // Runs once per world (tracked in saved data), leaving later operator changes intact.
        net.minecraft.server.level.ServerLevel overworld = event.getServer().overworld();
        com.koreykeipe.kingslayer.game.KsWorldData ksData = com.koreykeipe.kingslayer.game.KsWorldData.get(overworld);
        if (!ksData.firstStartDone) {
            event.getServer().getGameRules()
                    .getRule(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY)
                    .set(true, event.getServer());
            ksData.firstStartDone = true;
            ksData.setDirty();
            KingSlayer.LOGGER.info("KingSlayer: first world start — keepInventory defaulted to true.");
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        AirdropManager.get().onServerTick(event.getServer());
        com.koreykeipe.kingslayer.exchange.TributeExchange.tick(event.getServer());

        // Lapse the bounty contract if it ran out unclaimed (checked once a second).
        if (event.getServer().getTickCount() % 20 == 0) {
            GameManager.get().tickBounty();
        }

        // Standings broadcast on the configured interval. broadcastLeaderboard() suppresses
        // itself when nothing has changed, so this is an upper bound on frequency.
        int leaderboardInterval = com.koreykeipe.kingslayer.airdrop.AirdropConfig.LEADERBOARD_INTERVAL.get();
        if (leaderboardInterval > 0 && event.getServer().getTickCount() % leaderboardInterval == 0) {
            GameManager.get().broadcastLeaderboard();
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        if (!GameManager.get().isGameActive()) return;
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof ServerPlayer attackerPlayer) {
            CombatTracker.recordDamage(
                victim.getUUID(),
                attackerPlayer.getUUID(),
                attackerPlayer.getName().getString(),
                event.getAmount()
            );
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        if (!GameManager.get().isGameActive()) return;

        DamageSource source = event.getSource();
        Entity directKiller = source.getEntity();

        UUID directKillerUUID = directKiller instanceof ServerPlayer sp ? sp.getUUID() : null;
        String directKillerName = directKiller instanceof ServerPlayer sp ? sp.getName().getString() : null;

        // If a Knight mob landed the killing blow, use its display name as the cause
        // so the kill feed reads "died to King's Champion" rather than "died to mob".
        String cause = source.getMsgId();
        if (directKiller instanceof net.minecraft.world.entity.Mob mob
                && KnightSpawnHandler.isKnight(mob)) {
            cause = switch (KnightSpawnHandler.getKnightTier(mob)) {
                case "FOOTSOLDIER" -> "King's Footsoldier";
                case "CHAMPION"    -> "King's Champion";
                case "GUARD"       -> "King's Guard";
                default            -> "a King's Knight";
            };
        }

        // Vanilla's own combat-tracker credit as a last-resort fallback: catches indirect kills
        // (knocked into lava/void, doomed to fall by X, explosions) our windows/tags missed.
        net.minecraft.world.entity.LivingEntity vanillaCredit = victim.getKillCredit();
        UUID vanillaCreditUUID = vanillaCredit instanceof ServerPlayer vp ? vp.getUUID() : null;
        String vanillaCreditName = vanillaCredit instanceof ServerPlayer vp ? vp.getName().getString() : null;

        CombatTracker.KillAttribution attribution = CombatTracker.resolveKill(
            victim.getUUID(), directKillerUUID, directKillerName, cause,
            vanillaCreditUUID, vanillaCreditName
        );

        CombatTracker.clearPlayer(victim.getUUID());
        GameManager.get().onPlayerKilled(victim, attribution);

        // Tally this death into the persistent event counter BEFORE progress is read, so the
        // airdrop tiers and King auto-summon see the up-to-date roster (this also runs the
        // elimination / win-condition check when the victim hits their last life).
        GameManager.get().recordDeath(victim);

        // Check whether this death has crossed an airdrop threshold
        net.minecraft.server.MinecraftServer server = victim.getServer();
        if (server != null) {
            AirdropManager.get().onPlayerDeath(server);
        }
    }

    // -------------------------------------------------------------------------
    // Trap attribution
    // -------------------------------------------------------------------------

    /** Record who placed each pressure plate so trap kills can be attributed. */
    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getPlacedBlock().getBlock() instanceof PressurePlateBlock)) return;
        TrapTracker.recordPressurePlate(event.getPos(), player.getUUID(), player.getName().getString());
    }

    /** Remove the record when a plate is broken so stale entries don't accumulate. */
    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (!(event.getState().getBlock() instanceof PressurePlateBlock)) return;
        TrapTracker.removePressurePlate(event.getPos());
    }

    /**
     * When TNT is primed, search nearby blocks for a powered tracked pressure plate.
     * If found, tag this TNT entity so the explosion can attribute the kill.
     * The search radius covers most practical trap designs; extend SEARCH_RADIUS for
     * longer redstone wire runs between the plate and the TNT.
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof PrimedTnt tnt)) return;

        final int SEARCH_RADIUS = 4;
        BlockPos tntPos = tnt.blockPosition();

        outer:
        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                    BlockPos checkPos = tntPos.offset(dx, dy, dz);
                    TrapTracker.PlacerInfo placer = TrapTracker.getPressurePlatePlacer(checkPos);
                    if (placer == null) continue;

                    BlockState state = event.getLevel().getBlockState(checkPos);
                    if (state.getBlock() instanceof PressurePlateBlock
                            && state.getValue(PressurePlateBlock.POWERED)) {
                        TrapTracker.attributeTntToPlate(tnt.getId(), placer);
                        break outer;
                    }
                }
            }
        }
    }

    /**
     * Before explosion damage is applied, register CombatTracker attribution for every
     * player in the blast so that resolveKill() will credit the trap owner even if the
     * player dies to fire, fall, or another indirect cause seconds later.
     */
    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (event.getLevel().isClientSide()) return;
        Entity cause = event.getExplosion().getDirectSourceEntity();

        // Pressure-plate → TNT trap kills.
        if (cause instanceof PrimedTnt tnt) {
            TrapTracker.PlacerInfo placer = TrapTracker.getTntPlacer(tnt.getId());
            if (placer == null) return;
            for (Entity entity : event.getAffectedEntities()) {
                if (entity instanceof ServerPlayer victim) {
                    CombatTracker.registerAttribution(
                        victim.getUUID(), placer.uuid(), placer.name(),
                        "pressure_plate_trap", 10);
                }
            }
            TrapTracker.removeTnt(tnt.getId());
            return;
        }

        // Wind-charge knockback: deals no damage, so a player launched off a ledge / into
        // lava would otherwise be unattributed. Credit the thrower if they die soon after.
        if (cause instanceof net.minecraft.world.entity.projectile.windcharge.AbstractWindCharge wind
                && wind.getOwner() instanceof ServerPlayer thrower) {
            for (Entity entity : event.getAffectedEntities()) {
                if (entity instanceof ServerPlayer victim && victim != thrower) {
                    CombatTracker.registerAttribution(
                        victim.getUUID(), thrower.getUUID(), thrower.getName().getString(),
                        "wind_blast", 7);
                }
            }
        }
    }
}
