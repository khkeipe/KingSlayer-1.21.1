package com.koreykeipe.kingslayer.event;

import com.koreykeipe.kingslayer.KingSlayer;
import com.koreykeipe.kingslayer.game.CombatTracker;
import com.koreykeipe.kingslayer.game.GameManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KingSlayer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEvents {

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        GameManager.get().onServerStarted(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GameManager.get().onPlayerLogin(player);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        if (!GameManager.get().isGameActive()) return;
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof ServerPlayer attackerPlayer) {
            CombatTracker.recordAttack(victim.getUUID(), attackerPlayer.getUUID(), attackerPlayer.getName().getString());
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        if (!GameManager.get().isGameActive()) return;

        DamageSource source = event.getSource();
        Entity directKiller = source.getEntity();
        String killerName = null;
        String cause = source.getMsgId();
        boolean indirect = false;

        if (directKiller instanceof ServerPlayer killerPlayer) {
            killerName = killerPlayer.getName().getString();
        } else {
            CombatTracker.AttackRecord recent = CombatTracker.getRecentAttacker(victim.getUUID());
            if (recent != null) {
                killerName = recent.attackerName();
                indirect = true;
            }
        }

        CombatTracker.clearPlayer(victim.getUUID());
        GameManager.get().onPlayerDeath(victim, killerName, cause, indirect);
    }

    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        // reserved for future respawn / spectator handling
    }
}
