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

    /** Horizontal pull strength of the Yoink Rod. */
    private static final double YANK_POWER = 2.0;

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
            // Scale by (1 - knockback resistance) so the Anchor Charm shrugs off the yank.
            double resist = victim.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
            double power = YANK_POWER * (1.0 - resist);
            if (power > 0.05) {
                Vec3 dir = owner.position().subtract(victim.position()).normalize();
                victim.setDeltaMovement(dir.x * power, 0.4 * (1.0 - resist) + 0.05, dir.z * power);
                victim.hurtMarked = true; // sync the velocity to the victim's client
                owner.level().playSound(null, victim.blockPosition(),
                        SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 1.2f, 0.6f);
            }
        }
    }
}
