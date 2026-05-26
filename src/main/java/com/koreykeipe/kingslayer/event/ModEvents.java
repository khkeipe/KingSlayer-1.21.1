package com.koreykeipe.kingslayer.event;

import com.koreykeipe.kingslayer.KingSlayer;
import com.koreykeipe.kingslayer.command.DeathCommand;
import com.koreykeipe.kingslayer.game.CombatTracker;
import com.koreykeipe.kingslayer.game.GameManager;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = KingSlayer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEvents {

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event){
        if(event.getEntity() instanceof ServerPlayer player){
            GameManager.get().onPlayerLogin(player);
            CompoundTag persistent = player.getPersistentData();
            if(!persistent.contains("hasJoinedBefore")){
                //First Time Join
                persistent.putBoolean("hasJoinedBefore", true);
                player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("Welcome to ")
                        .append(Component.literal("King Slayer").withStyle(ChatFormatting.GOLD))
                ));
                Scoreboard scoreboard = player.getServer().getScoreboard();
                PlayerTeam team = scoreboard.getPlayerTeam("aqua_team");
                if(team != null){
                    scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
                }
            }else{
                player.sendSystemMessage(Component.literal("Welcome Back o/"));
            }

        }
    }


    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        if(event.isWasDeath()) {
            if (event.getEntity() instanceof ServerPlayer player) {
                updateDeaths(player);
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event){
        DeathCommand.register(event.getDispatcher());
    }

    public static void updateDeaths(ServerPlayer player){
        final int MAX_DEATHS = 5;
        Scoreboard scoreboard = player.getServer().getScoreboard();
        int deaths = player.getStats().getValue(Stats.CUSTOM, Stats.DEATHS);
        int lives = MAX_DEATHS - deaths;

        if(deaths == 0){
            PlayerTeam team = scoreboard.getPlayerTeam("aqua_team");
            if(team != null){
                scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
                player.sendSystemMessage(Component.literal("You Have ")
                        .append(Component.literal(String.valueOf(lives)).withStyle(ChatFormatting.AQUA))
                        .append(" Lives Remaining").withStyle(ChatFormatting.RESET)
                );
            }
        }
        else if(deaths == 1){
            PlayerTeam team = scoreboard.getPlayerTeam("green_team");
            if(team != null){
                scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
                player.sendSystemMessage(Component.literal("You Have ")
                        .append(Component.literal(String.valueOf(lives)).withStyle(ChatFormatting.DARK_GREEN))
                        .append(" Lives Remaining").withStyle(ChatFormatting.RESET)
                );
            }
        }
        else if(deaths == 2){
            PlayerTeam team = scoreboard.getPlayerTeam("lime_team");
            if(team != null){
                scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
                player.sendSystemMessage(Component.literal("You Have ")
                        .append(Component.literal(String.valueOf(lives)).withStyle(ChatFormatting.GREEN))
                        .append(" Lives Remaining").withStyle(ChatFormatting.RESET)
                );
            }
        }
        else if(deaths == 3){
            PlayerTeam team = scoreboard.getPlayerTeam("yello_team");
            if(team != null){
                scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
                player.sendSystemMessage(Component.literal("You Have ")
                        .append(Component.literal(String.valueOf(lives)).withStyle(ChatFormatting.YELLOW))
                        .append(" Lives Remaining").withStyle(ChatFormatting.RESET)
                );
            }
        }
        else if(deaths == 4){
            PlayerTeam team = scoreboard.getPlayerTeam("red_team");
            if(team != null){
                scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
                player.sendSystemMessage(Component.literal("You Have ")
                        .append(Component.literal(String.valueOf(lives)).withStyle(ChatFormatting.RED))
                        .append(" Life Remaining").withStyle(ChatFormatting.RESET)
                        .append(". . . MAKE IT COUNT!").withStyle(ChatFormatting.DARK_RED)
                );
            }
        }
        else if(deaths >= 5){
            PlayerTeam team = scoreboard.getPlayerTeam("gray_team");
            if(team != null){
                scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
            }
            GameManager.get().onPlayerEliminated(player);
            player.setGameMode(GameType.SPECTATOR);
            player.sendSystemMessage(Component.literal("THANKS FOR PLAYING KING SLAYER"));
        }
        else {
            Stat<ResourceLocation> stat = Stats.CUSTOM.get(Stats.DEATHS);
            player.getStats().setValue(player, stat, 0);
        }
    }
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        GameManager.get().onServerStarted(event.getServer());
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
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

        CombatTracker.KillAttribution attribution = CombatTracker.resolveKill(
            victim.getUUID(), directKillerUUID, directKillerName, source.getMsgId()
        );

        CombatTracker.clearPlayer(victim.getUUID());
        GameManager.get().onPlayerKilled(victim, attribution);
    }
}
