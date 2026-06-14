package com.koreykeipe.kingslayer.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The Shadow Cloak — worn on the chest. While sneaking, the wearer fades from sight; they
 * reappear the moment they stop sneaking or strike an enemy (see
 * {@link com.koreykeipe.kingslayer.event.ArmorPerkHandler} and
 * {@link com.koreykeipe.kingslayer.event.StealthHandler}). The cost is real chestplate
 * protection. Counter: a Truesight Visor reveals cloaked players with Glowing.
 *
 * <p>Note: vanilla invisibility still renders worn armor and held items, so the cloak hides
 * your body but not your other gear — run light for true stealth.</p>
 */
public class ShadowCloakItem extends ArmorItem {

    /** Game-time tick the cloak-granted invisibility expires (so only OUR invis is managed). */
    public static final String CLOAK_KEY = "ks_cloaked_until";
    /** Game-time tick until which a struck/revealed cloak refuses to re-hide. */
    public static final String REVEAL_KEY = "ks_cloak_reveal_until";

    public ShadowCloakItem(Properties properties) {
        super(ArmorMaterials.NETHERITE, ArmorItem.Type.CHESTPLATE, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("☁ Shadow Cloak")
                .withStyle(s -> s.withColor(ChatFormatting.DARK_PURPLE).withBold(true).withItalic(false));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Worn on the chest — sneak to fade from sight.")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("Stop sneaking or strike to reappear.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("Counter: a Truesight Visor reveals you with a glow.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
