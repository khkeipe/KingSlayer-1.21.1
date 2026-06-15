package com.koreykeipe.kingslayer.item;

import com.koreykeipe.kingslayer.KingSlayer;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, KingSlayer.MOD_ID);

    public static final DeferredHolder<Item, Item> CROWN = ITEMS.register("crown",
            ()-> new CrownItem(new Item.Properties()));

    // Renewable currency for the Tribute Stone exchange. Drops from knights and hostiles.
    public static final DeferredHolder<Item, Item> CROWN_FRAGMENT = ITEMS.register("crown_fragment",
            ()-> new CrownFragmentItem(new Item.Properties()));

    // Gated Tribute Stone reward — right-click to summon The King (final phase only).
    public static final DeferredHolder<Item, Item> BOSS_KEY = ITEMS.register("boss_key",
            ()-> new BossKeyItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> SLAYER_SWORD = ITEMS.register("slayer_sword",
            () -> new SwordItem(ModToolTeirs.SLAYER, new Item.Properties()
                    .attributes(SwordItem.createAttributes(ModToolTeirs.SLAYER,3, -2.4f))));
     public static final DeferredHolder<Item, Item> SLAYER_PICKAXE = ITEMS.register("slayer_pickaxe",
            () -> new PickaxeItem(ModToolTeirs.SLAYER, new Item.Properties()
                    .attributes(PickaxeItem.createAttributes(ModToolTeirs.SLAYER,1, -2.8f))));
     public static final DeferredHolder<Item, Item> SLAYER_SHOVEL= ITEMS.register("slayer_shovel",
            () -> new ShovelItem(ModToolTeirs.SLAYER, new Item.Properties()
                    .attributes(ShovelItem.createAttributes(ModToolTeirs.SLAYER,1.5f, -3.0f))));
     public static final DeferredHolder<Item, Item> SLAYER_AXE = ITEMS.register("slayer_axe",
            () -> new AxeItem(ModToolTeirs.SLAYER, new Item.Properties()
                    .attributes(AxeItem.createAttributes(ModToolTeirs.SLAYER,6, -3.2f))));
     public static final DeferredHolder<Item, Item> SLAYER_HOE = ITEMS.register("slayer_hoe",
            () -> new HoeItem(ModToolTeirs.SLAYER, new Item.Properties()
                    .attributes(HoeItem.createAttributes(ModToolTeirs.SLAYER,0, -3.0f))));

    public static final DeferredHolder<Item, Item> SLAYER_BAT = ITEMS.register("slayer_bat",
            () -> new SlayerBatItem(Tiers.WOOD, new Item.Properties()
                    .attributes(SwordItem.createAttributes(Tiers.WOOD, 1, -1.5f))));

    // Combat fishing rod — hooks a player and yanks them toward you (see FishingYoinkHandler).
    public static final DeferredHolder<Item, Item> YOINK_ROD = ITEMS.register("yoink_rod",
            () -> new YoinkRodItem(new Item.Properties().durability(256)));

    // Storm Brand — sword that calls a lightning bolt where you aim (right-click). Its power is
    // the lightning, not the blade: melee is intentionally weak (~2 total attack damage).
    public static final DeferredHolder<Item, Item> STORM_BRAND = ITEMS.register("storm_brand",
            () -> new StormBrandItem(Tiers.IRON, new Item.Properties()
                    .attributes(SwordItem.createAttributes(Tiers.IRON, -1, -2.4f))));

    // King's Maul — mace with a right-click ground-slam shockwave.
    public static final DeferredHolder<Item, Item> KINGS_MAUL = ITEMS.register("kings_maul",
            () -> new KingsMaulItem(new Item.Properties().durability(500)
                    .attributes(MaceItem.createAttributes())));

    // Anchor Greaves — boots granting full knockback immunity; counter to all displacement.
    public static final DeferredHolder<Item, Item> ANCHOR_CHARM = ITEMS.register("anchor_charm",
            () -> new AnchorCharmItem(new Item.Properties()));

    // Grapple Crossbow — right-click hooks a block and zips you toward it (mobility special).
    public static final DeferredHolder<Item, Item> GRAPPLE_CROSSBOW = ITEMS.register("grapple_crossbow",
            () -> new GrappleCrossbowItem(new Item.Properties().durability(384)));

    // Bola — thrown net that Roots a target; the counter to every mobility special.
    public static final DeferredHolder<Item, Item> BOLA = ITEMS.register("bola",
            () -> new BolaItem(new Item.Properties().stacksTo(16)));

    // Wind Cannon — right-click cone blast that knocks enemies back (countered by the Anchor Charm).
    public static final DeferredHolder<Item, Item> WIND_CANNON = ITEMS.register("wind_cannon",
            () -> new WindCannonItem(new Item.Properties().durability(256)));

    // Shadow Cloak — chestplate; sneak to turn invisible (breaks on strike). See ArmorPerkHandler.
    public static final DeferredHolder<Item, Item> SHADOW_CLOAK = ITEMS.register("shadow_cloak",
            () -> new ShadowCloakItem(new Item.Properties()));

    // Truesight Visor — helmet; passive aura that reveals nearby invisible foes. Counter to the Cloak.
    public static final DeferredHolder<Item, Item> TRUESIGHT_LENS = ITEMS.register("truesight_lens",
            () -> new TruesightLensItem(new Item.Properties()));

    // Sunder Pike — anti-armor sword; bonus bypass damage scaling with the target's armor.
    public static final DeferredHolder<Item, Item> SUNDER_PIKE = ITEMS.register("sunder_pike",
            () -> new SunderPikeItem(Tiers.DIAMOND, new Item.Properties()
                    .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 3, -2.8f))));

    // Bulwark Legguards — leggings; flat cut to all incoming damage. Counter to the Sunder Pike.
    public static final DeferredHolder<Item, Item> BULWARK_LEGGUARDS = ITEMS.register("bulwark_legguards",
            () -> new BulwarkLegguardsItem(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
