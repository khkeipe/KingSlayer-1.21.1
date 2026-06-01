package com.koreykeipe.kingslayer.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A custom melee weapon that demonstrates the three ways to give an item a
 * distinct identity entirely in Java — no JSON or lang-file required:
 *
 * <ol>
 *   <li>{@link #getName}           — styled gold + bold display name in every tooltip / toast</li>
 *   <li>{@link #appendHoverText}   — flavor lore lines below the name</li>
 *   <li>{@link #isFoil}            — unconditional enchanted purple glint</li>
 * </ol>
 *
 * Functional enchantments (Knockback II, Unbreaking III) are applied at drop time
 * via {@code AddItemModifier} in {@code ModGlobalLootModifierProvider}, not here.
 * That keeps the item usable from {@code /give} without breaking the enchantment
 * display — vanilla enchantment lines still render below the flavor text because
 * {@code super.appendHoverText()} is always called last.
 */
public class SlayerBatItem extends SwordItem {

    public SlayerBatItem(Tier tier, Item.Properties properties) {
        super(tier, properties);
    }

    // ------------------------------------------------------------------
    // Display name
    // ------------------------------------------------------------------

    /**
     * Overrides the name shown everywhere: inventory slots, pickup toasts,
     * the tooltip header, and death messages.
     *
     * Tip: returning a Component here completely ignores the lang file entry
     * for this item. If you want the lang file to control the base text but
     * still apply formatting, call {@code super.getName(stack)} and then
     * re-wrap it with {@code .copy().withStyle(...)}.
     */
    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("⚾ Slayer Bat")
                .withStyle(style -> style
                        .withColor(ChatFormatting.GOLD)
                        .withBold(true)
                        .withItalic(false));
    }

    // ------------------------------------------------------------------
    // Tooltip lore
    // ------------------------------------------------------------------

    /**
     * Adds lines below the item name in the tooltip.
     * Always call {@code super} last — it appends the actual enchantment lines,
     * so they appear after your flavor text rather than before it.
     *
     * Add as many {@code tooltip.add(...)} calls here as you like.
     * Common formatting conventions:
     *   • Flavor / lore     → GRAY + ITALIC
     *   • Stat descriptions → DARK_GRAY (no italic)
     *   • Warnings          → RED
     *   • Unlock hints      → DARK_AQUA + ITALIC
     */
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Swing for the fences.")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    // ------------------------------------------------------------------
    // Enchanted glint
    // ------------------------------------------------------------------

    /**
     * Returning {@code true} always gives the item the enchanted purple-shimmer
     * effect regardless of whether any enchantments are on the stack.
     *
     * Remove this override if you only want the glint when the item is
     * actually enchanted (the default SwordItem behaviour).
     */
    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
