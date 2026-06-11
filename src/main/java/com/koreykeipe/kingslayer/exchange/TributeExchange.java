package com.koreykeipe.kingslayer.exchange;

import com.koreykeipe.kingslayer.item.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * The no-GUI trade table behind the Tribute Stone. Players browse offers by
 * right-clicking the stone empty-handed (cycles + previews in the action bar) and
 * buy the selected offer by right-clicking with Crown Fragments in hand. All trades
 * are paid in {@link ModItems#CROWN_FRAGMENT}, the renewable currency.
 *
 * <p>Per-player selection is held in a static map keyed by UUID — fine because the
 * Tribute Stone is a single hub at world spawn.</p>
 */
public final class TributeExchange {

    /** A single exchange: pay {@code cost} fragments for a fresh {@code reward} stack. */
    public record Offer(int cost, String label, Supplier<ItemStack> reward) {}

    public static final List<Offer> OFFERS = List.of(
            new Offer(4,  "Crown",                  () -> new ItemStack(ModItems.CROWN.get())),
            new Offer(6,  "2x Golden Apple",        () -> new ItemStack(Items.GOLDEN_APPLE, 2)),
            new Offer(8,  "Slayer Sword",           () -> new ItemStack(ModItems.SLAYER_SWORD.get())),
            new Offer(8,  "Slayer Pickaxe",         () -> new ItemStack(ModItems.SLAYER_PICKAXE.get())),
            new Offer(8,  "Slayer Axe",             () -> new ItemStack(ModItems.SLAYER_AXE.get())),
            new Offer(10, "3x Diamond",             () -> new ItemStack(Items.DIAMOND, 3)),
            new Offer(14, "Enchanted Golden Apple", () -> new ItemStack(Items.ENCHANTED_GOLDEN_APPLE)),
            new Offer(20, "3x Crown",               () -> new ItemStack(ModItems.CROWN.get(), 3))
    );

    private static final Map<UUID, Integer> SELECTION = new HashMap<>();

    private TributeExchange() {}

    private static int selectedIndex(Player player) {
        return SELECTION.getOrDefault(player.getUUID(), 0) % OFFERS.size();
    }

    /** Advance to the next offer and preview it. */
    public static void cycle(ServerPlayer player) {
        SELECTION.put(player.getUUID(), (selectedIndex(player) + 1) % OFFERS.size());
        showOffer(player);
    }

    /** Action-bar preview of the currently selected offer + affordability. */
    public static void showOffer(ServerPlayer player) {
        Offer o = OFFERS.get(selectedIndex(player));
        int have = countFragments(player);
        String costColor = have >= o.cost() ? "§a" : "§c";
        player.displayClientMessage(Component.literal(
                "§6Tribute §7[" + (selectedIndex(player) + 1) + "/" + OFFERS.size() + "]  "
                        + costColor + o.cost() + " fragments §7→ §e" + o.label()
                        + " §7(you have " + have + ")"), true);
    }

    /** Attempt to buy the selected offer. Returns true on success. */
    public static boolean buy(ServerPlayer player) {
        Offer o = OFFERS.get(selectedIndex(player));
        int have = countFragments(player);
        if (have < o.cost()) {
            player.displayClientMessage(Component.literal(
                    "§cNeed " + o.cost() + " Crown Fragments §7— you have " + have
                            + ". Empty-hand right-click to browse offers."), true);
            return false;
        }
        removeFragments(player, o.cost());
        ItemStack reward = o.reward().get();
        if (!player.getInventory().add(reward)) {
            player.drop(reward, false);
        }
        player.displayClientMessage(Component.literal(
                "§6✦ §aReceived " + o.label() + " §7for " + o.cost() + " fragments"), true);
        return true;
    }

    public static int countFragments(Player player) {
        int total = 0;
        for (ItemStack s : player.getInventory().items) {
            if (s.is(ModItems.CROWN_FRAGMENT.get())) total += s.getCount();
        }
        return total;
    }

    private static void removeFragments(Player player, int amount) {
        int remaining = amount;
        for (ItemStack s : player.getInventory().items) {
            if (remaining <= 0) break;
            if (s.is(ModItems.CROWN_FRAGMENT.get())) {
                int take = Math.min(remaining, s.getCount());
                s.shrink(take);
                remaining -= take;
            }
        }
    }
}
