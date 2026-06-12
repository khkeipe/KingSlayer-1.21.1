package com.koreykeipe.kingslayer.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The Yoink Rod — a combat fishing rod. It casts and reels like a normal rod, but when
 * its hook latches onto a player, that player is violently yanked toward the caster
 * (handled in {@code FishingYoinkHandler}). A displacement/control weapon rather than a
 * damage one — perfect for dragging foes into lava, off ledges, or into your team.
 *
 * <p>No vanilla enchant adds combat power to rods, so the yank is intrinsic; Unbreaking /
 * Mending are the useful enchants (enabled via the durability item tag).</p>
 */
public class YoinkRodItem extends FishingRodItem {

    public YoinkRodItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("⚓ Yoink Rod")
                .withStyle(style -> style.withColor(ChatFormatting.AQUA).withBold(true).withItalic(false));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Hook a player to YANK them to you.")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("Drag foes into lava, off ledges, or into your team.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
