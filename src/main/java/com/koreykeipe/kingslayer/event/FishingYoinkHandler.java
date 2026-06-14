package com.koreykeipe.kingslayer.event;

import com.koreykeipe.kingslayer.KingSlayer;
import com.koreykeipe.kingslayer.game.CombatTracker;
import com.koreykeipe.kingslayer.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Fishing-hook combat: when any fishing hook latches onto a player, the owner is
 * registered for kill attribution (so a yoink into lava/void credits them). If the owner
 * wields the {@link ModItems#YOINK_ROD}, the victim is also violently yanked toward them.
 */
@Mod.EventBusSubscriber(modid = KingSlayer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FishingYoinkHandler {

    /** Max reel-in speed (blocks/tick) of the Yoink Rod — a reverse grapple toward the caster. */
    private static final double YANK_MAX_PULL = 2.5;

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof FishingHook hook)) return;
        if (!(event.getRayTraceResult() instanceof EntityHitResult ehr)) return;
        if (!(ehr.getEntity() instanceof ServerPlayer victim)) return;            // PvP only, server-side
        if (!(hook.getOwner() instanceof ServerPlayer owner) || owner == victim) return;

        // Indirect attribution — any rod that hooks a player credits the owner if the
        // victim dies soon after (e.g. reeled into lava or off a ledge).
        CombatTracker.registerAttribution(victim.getUUID(), owner.getUUID(),
                owner.getName().getString(), "yoinked", 7);

        // Super-yoink: only the custom Yoink Rod yanks the target toward the caster.
        boolean yoinkRod = owner.getMainHandItem().is(ModItems.YOINK_ROD.get())
                || owner.getOffhandItem().is(ModItems.YOINK_ROD.get());
        if (yoinkRod) {
            // Reverse grapple: reel the target toward the caster with a distance-scaled pull
            // (clamped), so a long-range hook drags them all the way in. Scaled by
            // (1 - knockback resistance) so the Anchor Greaves shrug off the reel.
            double resist = victim.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
            if (resist < 1.0) {
                Vec3 pull = owner.position().subtract(victim.position());
                double speed = Math.min(YANK_MAX_PULL, 0.25 * pull.length() + 0.6) * (1.0 - resist);
                Vec3 vel = pull.normalize().scale(speed);
                victim.setDeltaMovement(vel.x, vel.y + 0.3 * (1.0 - resist), vel.z);
                victim.hurtMarked = true; // sync the velocity to the victim's client
                owner.level().playSound(null, victim.blockPosition(),
                        SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 1.2f, 0.6f);
            }
        }
    }
}
