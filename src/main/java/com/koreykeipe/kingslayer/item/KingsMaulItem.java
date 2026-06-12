package com.koreykeipe.kingslayer.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * The King's Maul — a Mace that, on right-click, ground-slams a big AOE shockwave:
 * everything around the wielder is knocked back (respecting Knockback Resistance, so the
 * Anchor Charm counters it) and takes a little damage. Keeps the vanilla mace's melee/smash.
 */
public class KingsMaulItem extends MaceItem {

    private static final double RADIUS    = 5.0;
    private static final double STRENGTH  = 1.6;   // knockback power (scaled by victim KB-resist)
    private static final float  DAMAGE    = 4.0f;
    private static final int    COOLDOWN  = 160;   // 8 seconds

    public KingsMaulItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.success(stack);
        }
        ServerLevel sl = sp.serverLevel();

        AABB box = sp.getBoundingBox().inflate(RADIUS);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, box,
                t -> t != sp && t.isAlive() && !t.isSpectator())) {
            // knockback() applies (1 - knockbackResistance), so the Anchor Charm shrugs it off.
            e.knockback(STRENGTH, sp.getX() - e.getX(), sp.getZ() - e.getZ());
            e.hurt(sl.damageSources().playerAttack(sp), DAMAGE);
        }

        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, sp.getX(), sp.getY() + 0.2, sp.getZ(), 1, 0, 0, 0, 0);
        for (int i = 0; i < 24; i++) {
            double a = i / 24.0 * Math.PI * 2;
            sl.sendParticles(ParticleTypes.CLOUD,
                    sp.getX() + Math.cos(a) * 1.5, sp.getY() + 0.2, sp.getZ() + Math.sin(a) * 1.5,
                    1, Math.cos(a) * 0.2, 0.05, Math.sin(a) * 0.2, 0.1);
        }
        sl.playSound(null, sp.blockPosition(), SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 1.2f, 0.7f);

        sp.getCooldowns().addCooldown(this, COOLDOWN);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("⚒ King's Maul")
                .withStyle(s -> s.withColor(ChatFormatting.GOLD).withBold(true).withItalic(false));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click: ground-slam shockwave (knockback + damage).")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("Counter: Knockback Resistance (the Anchor Charm).")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
