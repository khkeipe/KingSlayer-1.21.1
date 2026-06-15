package com.koreykeipe.kingslayer.event;

import com.koreykeipe.kingslayer.KingSlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Shared movement-combat state for the displacement arsenal:
 * <ul>
 *   <li><b>Root</b> (Bola/Net) — freezes a target in place: horizontal movement is zeroed and
 *       upward velocity is clamped, so a rooted player can't walk, jump, grapple or be launched.
 *       This is the counter to all mobility specials.</li>
 *   <li><b>Grapple no-fall</b> — a freshly grappled player is spared the landing damage for a short
 *       window, so the Grapple Crossbow is a mobility tool and not a suicide button.</li>
 * </ul>
 */
@EventBusSubscriber(modid = KingSlayer.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class MovementCombatHandler {

    private static final String ROOT_KEY    = "ks_rooted_until";
    private static final String NOFALL_KEY  = "ks_grapple_nofall";

    /** Root a target for {@code ticks}: tick-freeze plus a heavy Slowness for visible feedback. */
    public static void root(LivingEntity victim, int ticks) {
        victim.getPersistentData().putLong(ROOT_KEY, victim.level().getGameTime() + ticks);
        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 6, false, true, true));
        victim.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, ticks, 2, false, true, true));
    }

    public static boolean isRooted(LivingEntity entity) {
        return entity.level().getGameTime() <= entity.getPersistentData().getLong(ROOT_KEY);
    }

    /** Grant a fall-damage grace window (in ticks) — used after a grapple swing or a launch-pad bounce. */
    public static void grantFallImmunity(LivingEntity entity, int ticks) {
        entity.getPersistentData().putLong(NOFALL_KEY, entity.level().getGameTime() + ticks);
    }

    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (entity.level().isClientSide || !isRooted(entity)) return;

        Vec3 m = entity.getDeltaMovement();
        if (m.x != 0 || m.z != 0 || m.y > 0) {
            entity.setDeltaMovement(0, Math.min(m.y, 0), 0); // pin horizontally, allow falling
            entity.hurtMarked = true;                          // sync the freeze to the client
        }
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (entity.level().getGameTime() <= entity.getPersistentData().getLong(NOFALL_KEY)) {
            entity.getPersistentData().remove(NOFALL_KEY);
            event.setCanceled(true); // no landing damage from the grapple
        }
    }
}
