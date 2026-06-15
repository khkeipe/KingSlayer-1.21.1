package com.koreykeipe.kingslayer.event;

import com.koreykeipe.kingslayer.KingSlayer;
import com.koreykeipe.kingslayer.damage.ModDamageTypes;
import com.koreykeipe.kingslayer.item.ModItems;
import com.koreykeipe.kingslayer.item.ShadowCloakItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Passive perks for the wearable combat gear (the held abilities became armor-slot trade-offs):
 * <ul>
 *   <li><b>Anchor Greaves</b> (boots) — full knockback immunity while worn.</li>
 *   <li><b>Shadow Cloak</b> (chest) — invisible while sneaking; reveals on strike (StealthHandler)
 *       and stays revealed briefly afterward.</li>
 *   <li><b>Truesight Visor</b> (helmet) — a periodic aura that Glows nearby invisible enemies.</li>
 * </ul>
 */
@EventBusSubscriber(modid = KingSlayer.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ArmorPerkHandler {

    private static final ResourceLocation ANCHOR_KB_ID =
            ResourceLocation.fromNamespaceAndPath(KingSlayer.MOD_ID, "anchor_boots");

    private static final double REVEAL_RADIUS = 16.0;

    /** Flat share of incoming damage the Bulwark Legguards absorb. */
    private static final float BULWARK_DR = 0.20f;

    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        long now = p.level().getGameTime();

        // --- Anchor Greaves: full knockback resistance while the boots are worn. ---
        AttributeInstance kb = p.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kb != null) {
            boolean anchored = p.getItemBySlot(EquipmentSlot.FEET).is(ModItems.ANCHOR_CHARM.get());
            boolean hasMod = kb.getModifier(ANCHOR_KB_ID) != null;
            if (anchored && !hasMod) {
                kb.addTransientModifier(new AttributeModifier(ANCHOR_KB_ID, 1.0, AttributeModifier.Operation.ADD_VALUE));
            } else if (!anchored && hasMod) {
                kb.removeModifier(ANCHOR_KB_ID);
            }
        }

        // --- Shadow Cloak: vanish while sneaking, unless recently revealed by a strike. ---
        boolean cloaked = p.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.SHADOW_CLOAK.get());
        boolean revealed = now < p.getPersistentData().getLong(ShadowCloakItem.REVEAL_KEY);
        if (cloaked && p.isShiftKeyDown() && !revealed) {
            p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 12, 0, false, false, false));
            p.getPersistentData().putLong(ShadowCloakItem.CLOAK_KEY, now + 12);
        } else if (now <= p.getPersistentData().getLong(ShadowCloakItem.CLOAK_KEY)
                && p.hasEffect(MobEffects.INVISIBILITY)) {
            // We granted this invis but the condition no longer holds — drop it now.
            p.removeEffect(MobEffects.INVISIBILITY);
            p.getPersistentData().remove(ShadowCloakItem.CLOAK_KEY);
        }

        // --- Truesight Visor: periodic reveal aura while the helmet is worn. ---
        if (now % 40 == 0 && p.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.TRUESIGHT_LENS.get())) {
            AABB box = p.getBoundingBox().inflate(REVEAL_RADIUS);
            for (LivingEntity t : p.serverLevel().getEntitiesOfClass(LivingEntity.class, box,
                    x -> x != p && x.isAlive() && x.isInvisible())) {
                t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false, true));
            }
        }
    }

    /** Bulwark Legguards: cut all incoming damage by a flat share — even armor-bypassing hits. */
    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        LivingEntity e = event.getEntity();
        if (!e.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.BULWARK_LEGGUARDS.get())) return;

        // Never soften the intentional anti-cheat / fail-safe sources.
        DamageSource src = event.getSource();
        if (src.is(ModDamageTypes.NETHER_GAS)
                || src.is(DamageTypes.FELL_OUT_OF_WORLD)
                || src.is(DamageTypes.GENERIC_KILL)) {
            return;
        }
        event.setAmount(event.getAmount() * (1.0f - BULWARK_DR));
    }
}
