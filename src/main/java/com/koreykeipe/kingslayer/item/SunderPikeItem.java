package com.koreykeipe.kingslayer.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The Sunder Pike — an anti-armor weapon. On top of its melee hit it deals bonus
 * armor-bypassing damage that scales with the target's armor points, so the more armor
 * a foe is wearing, the harder it bites — it melts heavily-plated players while doing
 * nothing extra to the unarmored. Counter: Bulwark Legguards, whose flat damage cut blunts
 * even this bypassing damage (ordinary armor can't).
 */
public class SunderPikeItem extends SwordItem {

    /** Bonus bypass damage per point of the target's armor. */
    private static final double ARMOR_PIERCE_FACTOR = 0.5;

    public SunderPikeItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide && attacker instanceof Player p) {
            int armor = target.getArmorValue();
            if (armor > 0) {
                float bonus = (float) (armor * ARMOR_PIERCE_FACTOR);
                // Clear the melee hit's i-frames so the bypass tick lands, then deal it as
                // player-attributed magic (ignores armor points).
                target.invulnerableTime = 0;
                target.hurt(target.damageSources().indirectMagic(p, p), bonus);
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("⩘ Sunder Pike")
                .withStyle(s -> s.withColor(ChatFormatting.GOLD).withBold(true).withItalic(false));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Pierces armor — bonus damage that grows with the foe's armor.")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("Melts heavy plating; no extra against the unarmored.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("Counter: Bulwark Legguards blunt even piercing hits.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
