package com.koreykeipe.kingslayer.event;

import com.koreykeipe.kingslayer.KingSlayer;
import com.koreykeipe.kingslayer.item.ShadowCloakItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Stealth balance: striking an enemy while cloaked (by the Shadow Cloak) drops the cloak,
 * so the cloak is for repositioning and ambush setup — not a free invisible kill loop.
 * Only cloak-granted invisibility is broken; vanilla potion invisibility is left alone.
 */
@EventBusSubscriber(modid = KingSlayer.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class StealthHandler {

    /** Ticks a struck cloak stays revealed before it can re-hide. */
    private static final int REVEAL_TICKS = 40;

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        long now = sp.level().getGameTime();
        if (now <= sp.getPersistentData().getLong(ShadowCloakItem.CLOAK_KEY) && sp.hasEffect(MobEffects.INVISIBILITY)) {
            sp.removeEffect(MobEffects.INVISIBILITY);
            sp.getPersistentData().remove(ShadowCloakItem.CLOAK_KEY);
            sp.getPersistentData().putLong(ShadowCloakItem.REVEAL_KEY, now + REVEAL_TICKS);
            sp.displayClientMessage(Component.literal("☁ Your cloak falls as you strike!")
                    .withStyle(ChatFormatting.DARK_PURPLE), true);
        }
    }
}
