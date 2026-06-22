package com.koreykeipe.kingslayer.item;

import com.koreykeipe.kingslayer.event.MovementCombatHandler;
import com.koreykeipe.kingslayer.game.CombatTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The Bola — a thrown net that Roots whoever it catches: they're frozen in place for a few
 * seconds, unable to walk, jump, grapple or be launched. It's the keystone counter to every
 * mobility special (Grapple Crossbow, and future Launch Pad / Spring Boat). One is consumed
 * per throw. Hooking a player also credits the thrower if the victim dies while netted.
 */
public class BolaItem extends Item {

    // Range / root duration / cooldown are config-driven ([combat] in the server config).
    private static double range()     { return com.koreykeipe.kingslayer.airdrop.AirdropConfig.BOLA_RANGE.get(); }
    private static int    rootTicks() { return com.koreykeipe.kingslayer.airdrop.AirdropConfig.BOLA_ROOT_TICKS.get(); }
    private static int    cooldown()  { return com.koreykeipe.kingslayer.airdrop.AirdropConfig.BOLA_COOLDOWN_TICKS.get(); }

    public BolaItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level,
                                                  net.minecraft.world.entity.player.Player player,
                                                  InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.success(stack);
        }
        ServerLevel sl = sp.serverLevel();

        Vec3 eye = sp.getEyePosition();
        Vec3 look = sp.getViewVector(1.0f);
        Vec3 end = eye.add(look.scale(range()));

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(sl, sp, eye, end,
                sp.getBoundingBox().expandTowards(look.scale(range())).inflate(1.0),
                e -> e instanceof LivingEntity && e != sp && !e.isSpectator());

        if (hit == null || !(hit.getEntity() instanceof LivingEntity victim)) {
            sp.displayClientMessage(Component.literal("✖ The net found no target.")
                    .withStyle(ChatFormatting.GRAY), true);
            sl.playSound(null, sp.blockPosition(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.9f, 0.8f);
            sp.getCooldowns().addCooldown(this, cooldown() / 2);
            return InteractionResultHolder.fail(stack);
        }

        MovementCombatHandler.root(victim, rootTicks());
        if (victim instanceof ServerPlayer netted) {
            CombatTracker.registerAttribution(netted.getUUID(), sp.getUUID(),
                    sp.getName().getString(), "netted", 7);
            netted.displayClientMessage(Component.literal("⛓ You are Rooted!")
                    .withStyle(ChatFormatting.RED), true);
        }

        sl.playSound(null, sp.blockPosition(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 1.0f, 1.0f);
        sl.playSound(null, victim.blockPosition(), SoundEvents.LEASH_KNOT_PLACE, SoundSource.PLAYERS, 1.2f, 0.8f);
        sp.getCooldowns().addCooldown(this, cooldown());
        if (!sp.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("⛓ Bola")
                .withStyle(s -> s.withColor(ChatFormatting.WHITE).withBold(true).withItalic(false));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click: net a target to Root them (4s).")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("Rooted enemies can't run, jump or use mobility gear.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("The counter to grapples, launch pads & spring boats.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
