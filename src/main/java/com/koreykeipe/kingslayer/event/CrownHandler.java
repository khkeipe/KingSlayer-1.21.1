package com.koreykeipe.kingslayer.event;

import com.koreykeipe.kingslayer.KingSlayer;
import com.koreykeipe.kingslayer.damage.ModDamageTypes;
import com.koreykeipe.kingslayer.game.GameManager;
import com.koreykeipe.kingslayer.item.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Drives the dual-purpose Crown:
 * <ul>
 *   <li><b>Totem (passive):</b> a Crown anywhere in the inventory is consumed to cancel a
 *       lethal hit, leaving the bearer at 3 hearts with Regen/Absorption/Fire-Res.</li>
 *   <li><b>Extra hearts (active):</b> {@link #grantHeart} adds a permanent max-HP bonus,
 *       persisted per player and re-applied on login/respawn.</li>
 * </ul>
 * The totem deliberately does <em>not</em> save from the void or from nether gas (the
 * uncounterable anti-cheat).
 */
@EventBusSubscriber(modid = KingSlayer.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class CrownHandler {

    private static final int CAP = 5; // max crown hearts (+10 max health)
    private static final String TAG = "ks_crown_hearts";
    private static final ResourceLocation MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(KingSlayer.MOD_ID, "crown_hearts");

    // ------------------------------------------------------------------
    // Extra hearts (right-click consume)
    // ------------------------------------------------------------------

    public static int getHearts(Player p) { return p.getPersistentData().getInt(TAG); }
    public static void setHearts(Player p, int n) { p.getPersistentData().putInt(TAG, n); }

    /** Re-applies the max-HP bonus from the player's stored crown-heart count. */
    public static void applyHearts(ServerPlayer p) {
        AttributeInstance attr = p.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return;
        attr.removeModifier(MODIFIER_ID);
        int hearts = getHearts(p);
        if (hearts > 0) {
            attr.addTransientModifier(new AttributeModifier(MODIFIER_ID, hearts * 2.0,
                    AttributeModifier.Operation.ADD_VALUE));
        }
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    /** Grants +1 permanent heart (up to {@link #CAP}). Returns false if already capped. */
    public static boolean grantHeart(ServerPlayer p) {
        int hearts = getHearts(p);
        if (hearts >= CAP) return false;
        setHearts(p, hearts + 1);
        applyHearts(p);
        p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 2f)); // fill the new heart
        p.displayClientMessage(Component.literal(
                "§6♛ §a+1 Heart §7(" + (hearts + 1) + "/" + CAP + ")"), true);
        return true;
    }

    // ------------------------------------------------------------------
    // Totem (passive, while held in inventory)
    // ------------------------------------------------------------------

    // HIGHEST priority so the Crown cancels the death BEFORE the game's death-counter
    // (ModEvents.onLivingDeath, normal priority) can run. Once cancelled, that handler is
    // skipped — so a Crown save never registers as a death, elimination, or airdrop progress.
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Never save from the void (would loop) or nether gas (uncounterable anti-cheat).
        DamageSource src = event.getSource();
        if (src.is(DamageTypes.FELL_OUT_OF_WORLD) || src.is(ModDamageTypes.NETHER_GAS)) return;

        if (!consumeOneCrown(player)) return; // no Crown — let the death happen

        event.setCanceled(true);
        player.setHealth(6.0f);
        player.removeAllEffects();
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
        player.level().broadcastEntityEvent(player, (byte) 35); // totem "pop" animation
        player.serverLevel().playSound(null, player.blockPosition(),
                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
        GameManager.get().broadcast("§6♛ §e" + player.getName().getString()
                + "'s §6Crown §eshatters to spare their life!");
    }

    private static boolean consumeOneCrown(ServerPlayer p) {
        for (ItemStack s : p.getInventory().offhand) {
            if (s.is(ModItems.CROWN.get())) { s.shrink(1); return true; }
        }
        for (ItemStack s : p.getInventory().items) {
            if (s.is(ModItems.CROWN.get())) { s.shrink(1); return true; }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Persistence — re-apply the heart bonus across login / respawn
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) applyHearts(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) applyHearts(p);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return; // forge data isn't auto-copied on death — carry it over
        int hearts = event.getOriginal().getPersistentData().getInt(TAG);
        event.getEntity().getPersistentData().putInt(TAG, hearts);
    }
}
