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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The Wind Cannon — fires a blast of air in a forward cone, hurling everyone caught back
 * (and a little up) with light damage. Built on {@link LivingEntity#knockback}, so the
 * Anchor Charm's knockback resistance shrugs it off entirely — that's its counter.
 */
public class WindCannonItem extends Item {

    private static final double RANGE     = 6.0;
    private static final double CONE_DOT  = 0.45;  // ~63° half-cone
    private static final float  STRENGTH  = 1.8f;
    private static final float  DAMAGE    = 3.0f;
    private static final int    COOLDOWN  = 80;    // 4 seconds

    public WindCannonItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.success(stack);
        }
        ServerLevel sl = sp.serverLevel();
        Vec3 origin = sp.getEyePosition();
        Vec3 look = sp.getViewVector(1.0f);

        AABB box = sp.getBoundingBox().expandTowards(look.scale(RANGE)).inflate(2.0);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, box,
                t -> t != sp && t.isAlive() && !t.isSpectator())) {
            Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(origin);
            if (to.length() > RANGE) continue;
            if (look.dot(to.normalize()) < CONE_DOT) continue; // outside the cone

            e.knockback(STRENGTH, sp.getX() - e.getX(), sp.getZ() - e.getZ());
            double resist = e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
            if (resist < 1.0) {
                e.setDeltaMovement(e.getDeltaMovement().add(0, 0.42 * (1.0 - resist), 0));
                e.hurtMarked = true;
            }
            e.hurt(sl.damageSources().playerAttack(sp), DAMAGE);
        }

        // Visual gust + report.
        Vec3 muzzle = origin.add(look.scale(1.2));
        sl.sendParticles(ParticleTypes.CLOUD, muzzle.x, muzzle.y, muzzle.z, 25, 0.4, 0.4, 0.4, 0.15);
        sl.sendParticles(ParticleTypes.GUST, muzzle.x, muzzle.y, muzzle.z, 1, 0, 0, 0, 0);
        sl.playSound(null, sp.blockPosition(), SoundEvents.WIND_CHARGE_THROW, SoundSource.PLAYERS, 1.2f, 0.8f);

        sp.getCooldowns().addCooldown(this, COOLDOWN);
        stack.hurtAndBreak(1, sp, EquipmentSlot.MAINHAND);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("✺ Wind Cannon")
                .withStyle(s -> s.withColor(ChatFormatting.WHITE).withBold(true).withItalic(false));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click: blast a cone of wind to launch enemies back.")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("Counter: the Anchor Charm's knockback resistance ignores it.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
