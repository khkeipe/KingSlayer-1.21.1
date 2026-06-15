package com.koreykeipe.kingslayer.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The Bulwark Legguards — worn as leggings. While equipped they apply a flat percentage cut
 * to ALL incoming damage (see {@link com.koreykeipe.kingslayer.event.ArmorPerkHandler}),
 * including armor-bypassing hits that ordinary armor can't stop. That makes them the counter
 * to the Sunder Pike — and a solid general defensive. The cost is the leggings slot.
 */
public class BulwarkLegguardsItem extends ArmorItem {

    public BulwarkLegguardsItem(Properties properties) {
        super(ModArmorMaterials.SLAYER_PLATE, ArmorItem.Type.LEGGINGS, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("⛨ Bulwark Legguards")
                .withStyle(s -> s.withColor(ChatFormatting.GRAY).withBold(true).withItalic(false));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Worn as leggings — cuts all incoming damage by a flat share.")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("Blunts even armor-piercing hits the Sunder Pike deals.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("Trade-off: no legging enchants while braced.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
