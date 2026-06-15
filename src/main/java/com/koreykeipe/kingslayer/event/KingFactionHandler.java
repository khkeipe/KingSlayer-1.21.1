package com.koreykeipe.kingslayer.event;

import com.koreykeipe.kingslayer.KingSlayer;
import com.koreykeipe.kingslayer.entity.TheKing;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Keeps The King and his summoned knights on the same side: they will not target one another,
 * and any damage between faction members is cancelled. Players (and everyone else) are
 * unaffected — the King and his knights still hunt them normally.
 */
@EventBusSubscriber(modid = KingSlayer.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class KingFactionHandler {

    /** The King himself or any of his summoned knights. */
    private static boolean isKingFaction(Entity e) {
        if (e instanceof TheKing) return true;
        return e instanceof Mob m && KnightSpawnHandler.isKnight(m);
    }

    /** Don't let a faction member acquire a fellow faction member as its attack target. */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity newTarget = event.getNewAboutToBeSetTarget();
        if (newTarget != null && isKingFaction(event.getEntity()) && isKingFaction(newTarget)) {
            event.setCanceled(true);
        }
    }

    /** Cancel any damage dealt between faction members (melee, shockwaves, stray hits). */
    @SubscribeEvent
    public static void onAttack(LivingIncomingDamageEvent event) {
        if (!isKingFaction(event.getEntity())) return;
        Entity attacker = event.getSource().getEntity();
        if (attacker != null && isKingFaction(attacker)) {
            event.setCanceled(true);
        }
    }
}
