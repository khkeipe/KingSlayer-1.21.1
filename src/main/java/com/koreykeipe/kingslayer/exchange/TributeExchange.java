package com.koreykeipe.kingslayer.exchange;

import com.koreykeipe.kingslayer.airdrop.AirdropManager;
import com.koreykeipe.kingslayer.airdrop.AirdropTier;
import com.koreykeipe.kingslayer.block.ModBlocks;
import com.koreykeipe.kingslayer.item.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * The no-GUI trade table behind the Tribute Stone. Browse offers by right-clicking
 * the stone empty-handed (cycles + previews in the action bar); buy the selected
 * offer by right-clicking with Crown Fragments in hand. All trades are paid in
 * {@link ModItems#CROWN_FRAGMENT}.
 *
 * <p>Offers may carry a {@code requiredTier} that gates them until that airdrop tier
 * has fired — e.g. the Boss Key only unlocks in the final (EPIC) phase, so The King
 * can't be summoned early.</p>
 */
public final class TributeExchange {

    /**
     * A single exchange. {@code requiredTier} (nullable) locks the offer until that
     * airdrop tier has been triggered this round.
     */
    public record Offer(int cost, String label, Supplier<ItemStack> reward, AirdropTier requiredTier) {
        public Offer(int cost, String label, Supplier<ItemStack> reward) {
            this(cost, label, reward, null);
        }
    }

    public static final List<Offer> OFFERS = List.of(
            new Offer(8,  "Mystery Crate",          () -> new ItemStack(ModBlocks.MYSTERY_CRATE.get())),
            new Offer(64,  "Crown",                  () -> new ItemStack(ModItems.CROWN.get())),
            new Offer(24,  "2x Golden Apple",        () -> new ItemStack(Items.GOLDEN_APPLE, 2)),
            new Offer(10,  "Slayer Sword",           () -> new ItemStack(ModItems.SLAYER_SWORD.get())),
            new Offer(10,  "Slayer Pickaxe",         () -> new ItemStack(ModItems.SLAYER_PICKAXE.get())),
            new Offer(10,  "Slayer Axe",             () -> new ItemStack(ModItems.SLAYER_AXE.get())),
            new Offer(10, "3x Diamond",             () -> new ItemStack(Items.DIAMOND, 3)),
            new Offer(32, "Boss Key — summons The King",
                    () -> new ItemStack(ModItems.BOSS_KEY.get()), AirdropTier.EPIC)
    );

    private static final Map<UUID, Integer> SELECTION = new HashMap<>();

    // Action-bar text is re-sent each tick for HOLD_TICKS so the offer lingers on screen.
    private static final int HOLD_TICKS = 100; // ~5 seconds
    private record Held(Component text, int untilTick) {}
    private static final Map<UUID, Held> ACTIVE = new HashMap<>();

    private TributeExchange() {}

    private static int selectedIndex(Player player) {
        return SELECTION.getOrDefault(player.getUUID(), 0) % OFFERS.size();
    }

    private static boolean isLocked(Offer offer) {
        return offer.requiredTier() != null && !AirdropManager.get().isTierTriggered(offer.requiredTier());
    }

    /** Advance to the next offer and preview it. */
    public static void cycle(ServerPlayer player) {
        SELECTION.put(player.getUUID(), (selectedIndex(player) + 1) % OFFERS.size());
        showOffer(player);
    }

    /** Action-bar preview of the currently selected offer. */
    public static void showOffer(ServerPlayer player) {
        Offer o = OFFERS.get(selectedIndex(player));
        String idx = "§6Tribute §7[" + (selectedIndex(player) + 1) + "/" + OFFERS.size() + "]  ";
        if (isLocked(o)) {
            hold(player, Component.literal(idx + "§8🔒 §7" + o.label() + " §8— unlocks in the final phase"));
            return;
        }
        int have = countFragments(player);
        String costColor = have >= o.cost() ? "§a" : "§c";
        hold(player, Component.literal(idx + costColor + o.cost() + " fragments §7→ §e" + o.label()
                + " §7(you have " + have + ")  §8» hold fragments & click to buy"));
    }

    /** Attempt to buy the selected offer. Returns true on success. */
    public static boolean buy(ServerPlayer player) {
        Offer o = OFFERS.get(selectedIndex(player));
        if (isLocked(o)) {
            hold(player, Component.literal("§8🔒 §7\"" + o.label() + "\" unlocks in the final phase of the round."));
            return false;
        }
        int have = countFragments(player);
        if (have < o.cost()) {
            hold(player, Component.literal("§cNeed " + o.cost() + " Crown Fragments §7— you have " + have
                    + ". Empty-hand right-click to browse."));
            return false;
        }
        removeFragments(player, o.cost());
        ItemStack reward = o.reward().get();
        if (!player.getInventory().add(reward)) {
            player.drop(reward, false);
        }
        hold(player, Component.literal("§6✦ §aReceived " + o.label() + " §7for " + o.cost() + " fragments"));
        return true;
    }

    // -------------------------------------------------------------------------
    // Action-bar "hold" — re-send the last message each tick so it lingers
    // -------------------------------------------------------------------------

    private static void hold(ServerPlayer player, Component text) {
        player.displayClientMessage(text, true);
        MinecraftServer server = player.getServer();
        if (server != null) {
            ACTIVE.put(player.getUUID(), new Held(text, server.getTickCount() + HOLD_TICKS));
        }
    }

    /** Called every server tick to keep active offer text on screen. */
    public static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) return;
        int now = server.getTickCount();
        Iterator<Map.Entry<UUID, Held>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Held> e = it.next();
            if (now >= e.getValue().untilTick()) { it.remove(); continue; }
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p == null) { it.remove(); continue; }
            p.displayClientMessage(e.getValue().text(), true);
        }
    }

    // -------------------------------------------------------------------------
    // Fragment accounting
    // -------------------------------------------------------------------------

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
