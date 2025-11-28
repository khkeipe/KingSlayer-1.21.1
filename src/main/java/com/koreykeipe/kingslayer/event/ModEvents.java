package com.koreykeipe.kingslayer.event;

import com.koreykeipe.kingslayer.KingSlayer;
import com.koreykeipe.kingslayer.command.DeathCommand;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.*;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = KingSlayer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEvents {

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event){
        if(event.getEntity() instanceof Player player){
            CompoundTag persistent = player.getPersistentData();
            if(!persistent.contains("hasJoinedBefore")){
                //First Time Join
                persistent.putBoolean("hasJoinedBefore", true);
                player.sendSystemMessage(Component.literal("Welcome to King Slayer!"));

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
                player.sendSystemMessage(Component.literal("You've Lost A Life, You Have " + lives + " Remaining"));

                if(deaths == 0){
                    PlayerTeam team = scoreboard.getPlayerTeam("aqua_team");
                    if(team != null){
                        scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
                    }
                }
                else if(deaths == 1){
                    PlayerTeam team = scoreboard.getPlayerTeam("green_team");
                    if(team != null){
                        scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
                    }
                }
                else if(deaths == 2){
                    PlayerTeam team = scoreboard.getPlayerTeam("lime_team");
                    if(team != null){
                        scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
                    }
                }
                else if(deaths == 3){
                    PlayerTeam team = scoreboard.getPlayerTeam("yello_team");
                    if(team != null){
                        scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
                    }
                }
                else if(deaths == 4){
                    PlayerTeam team = scoreboard.getPlayerTeam("red_team");
                    if(team != null){
                        scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
                    }
                }
                else if(deaths >= 5){
                    PlayerTeam team = scoreboard.getPlayerTeam("gray_team");
                    if(team != null){
                        scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
                    }
                    player.setGameMode(GameType.SPECTATOR);
                    player.sendSystemMessage(Component.literal("THANKS FOR PLAYING KING SLAYER"));
                }
                else {
                    Stat<ResourceLocation> stat = Stats.CUSTOM.get(Stats.DEATHS);
                    player.getStats().setValue(player, stat, 0);
                }
        }
}
