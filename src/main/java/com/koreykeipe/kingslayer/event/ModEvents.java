package com.koreykeipe.kingslayer.event;

import com.koreykeipe.kingslayer.KingSlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.*;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = KingSlayer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEvents {
    public static ServerScoreboard scoreboard;

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {

        }
    }

    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        final int MAX_DEATHS = 5;
        if(event.isWasDeath()) {
            if(event.getEntity() instanceof ServerPlayer player){
                int deaths = player.getStats().getValue(Stats.CUSTOM,Stats.DEATHS);
                player.sendSystemMessage(Component.literal("You've Lost A Life"));


                if(deaths == 0){
                    player.getScoreboard().addPlayerToTeam("dark_green");
                }
                else if(deaths == 1){

                }
                else if(deaths == 2){

                }
                else if(deaths == 3){

                }
                else if(deaths == 4){

                }else if(deaths >= 5){
                    player.setGameMode(GameType.SPECTATOR);
                    player.sendSystemMessage(Component.literal("THANKS FOR PLAYING KING SLAYER"));
                }
                else {
                    Stat<ResourceLocation> stat = Stats.CUSTOM.get(Stats.DEATHS);
                    player.getStats().setValue(player, stat, 0);
                }
            }
        }
    }
}
