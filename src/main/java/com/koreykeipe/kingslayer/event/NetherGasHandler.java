package com.koreykeipe.kingslayer.event;

import com.koreykeipe.kingslayer.KingSlayer;
import com.koreykeipe.kingslayer.damage.ModDamageTypes;
import com.koreykeipe.kingslayer.game.GameManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Applies slow, unavoidable nether-gas damage to every player inside the Nether
 * while a game is active. Used as an anti-cheat / region lock — there is no
 * immunity or counter.
 *
 * <h3>How the effect is presented</h3>
 * <ul>
 *   <li><b>Visual</b> — vanilla {@link MobEffects#POISON} is refreshed each pulse,
 *       giving the green screen tint, poison particles, and the wobbling health
 *       bar. No custom GUI rendering required.</li>
 *   <li><b>Lethal damage</b> — poison cannot kill (it floors at ½ heart), so the
 *       custom {@code nether_gas} damage source delivers the actual kill. Because
 *       poison can never land the final blow, the death message stays
 *       {@code death.attack.nether_gas}.</li>
 *   <li><b>Warning</b> — an action-bar message is shown to the affected player
 *       each pulse (action bar refreshes in place, so it never spams the chat log).</li>
 * </ul>
 *
 * <h3>Tuning constants</h3>
 * <ul>
 *   <li>{@link #DAMAGE_INTERVAL_TICKS} — how often a pulse fires (default 60 = 3 s)</li>
 *   <li>{@link #DAMAGE_AMOUNT}         — half-hearts of custom damage per pulse</li>
 *   <li>{@link #POISON_DURATION_TICKS} — poison length per pulse (kept above the
 *       interval so the tint stays continuous)</li>
 *   <li>{@link #POISON_AMPLIFIER}      — poison level (0 = Poison I)</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = KingSlayer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NetherGasHandler {

    /** Ticks between pulses. 60 = every 3 seconds. */
    private static final int DAMAGE_INTERVAL_TICKS = 60;

    /** Custom (lethal) damage dealt per pulse in half-hearts. 1.0f = ½ heart. */
    private static final float DAMAGE_AMOUNT = 1.0f;

    /** Poison duration applied each pulse. Slightly above the interval keeps the tint seamless. */
    private static final int POISON_DURATION_TICKS = 80;

    /** Poison level. 0 = Poison I, 1 = Poison II, etc. */
    private static final int POISON_AMPLIFIER = 0;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // Only act at the end of the tick and only for server-side players
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        // Respect game state and player mode
        if (!GameManager.get().isGameActive()) return;
        if (player.isCreative() || player.isSpectator()) return;

        // Only in the Nether
        if (!player.level().dimension().equals(Level.NETHER)) return;

        // Pulse on the interval
        if (player.tickCount % DAMAGE_INTERVAL_TICKS != 0) return;

        // 1 — Vanilla poison for the green-screen visual + particles (cannot kill on its own)
        player.addEffect(new MobEffectInstance(
                MobEffects.POISON, POISON_DURATION_TICKS, POISON_AMPLIFIER,
                false,  // not ambient
                true,   // show particles
                true)); // show HUD icon

        // 2 — Custom lethal damage; this is what actually finishes the player and
        //     owns the "nether_gas" death message.
        Holder<DamageType> holder = player.serverLevel()
                .registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(ModDamageTypes.NETHER_GAS);
        player.hurt(new DamageSource(holder), DAMAGE_AMOUNT);

        // 3 — Action-bar warning (the `true` flag routes it to the action bar, not chat)
        player.displayClientMessage(
                Component.literal("☠ The nether gas is poisoning you! Get out!")
                        .withStyle(ChatFormatting.DARK_GREEN),
                true);
    }
}
