package com.koreykeipe.kingslayer.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The Truesight Visor — worn as a helmet. While equipped it pulses a passive revealing aura
 * (see {@link com.koreykeipe.kingslayer.event.ArmorPerkHandler}): nearby invisible enemies are
 * periodically marked with Glowing, lit up through walls. The counter to the Shadow Cloak.
 * The cost is the helmet slot — no Respiration or Aqua Affinity while watching for ghosts.
 */
public class TruesightLensItem extends ArmorItem {

    public TruesightLensItem(Properties properties) {
        super(ModArmorMaterials.SLAYER_PLATE, ArmorItem.Type.HELMET, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("◉ Truesight Visor")
                .withStyle(s -> s.withColor(ChatFormatting.AQUA).withBold(true).withItalic(false));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Worn as a helmet — reveals nearby invisible enemies.")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("They glow through walls while you wear it.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("The counter to the Shadow Cloak.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
