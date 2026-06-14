package com.koreykeipe.kingslayer.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The Anchor Greaves — worn as boots. While equipped they grant full knockback immunity
 * (applied by {@link com.koreykeipe.kingslayer.event.ArmorPerkHandler}), making the wearer
 * the keystone counter to every displacement special: launchers, shockwaves, the Yoink Rod
 * and the Wind Cannon. The cost is the boots slot — no Feather Falling, Depth Strider, etc.
 */
public class AnchorCharmItem extends ArmorItem {

    public AnchorCharmItem(Properties properties) {
        super(ModArmorMaterials.SLAYER_PLATE.getHolder().orElseThrow(), ArmorItem.Type.BOOTS, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("⚓ Anchor Greaves")
                .withStyle(s -> s.withColor(ChatFormatting.BLUE).withBold(true).withItalic(false));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Worn as boots — grants full knockback immunity.")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("Counters launchers, shockwaves, the Yoink Rod & Wind Cannon.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("Trade-off: no boot enchants while anchored.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
