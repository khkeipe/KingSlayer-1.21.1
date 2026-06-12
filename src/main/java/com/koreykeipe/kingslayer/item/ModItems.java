package com.koreykeipe.kingslayer.item;

import com.koreykeipe.kingslayer.KingSlayer;
import net.minecraft.world.item.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, KingSlayer.MOD_ID);

    public static final RegistryObject<Item> CROWN = ITEMS.register("crown",
            ()-> new CrownItem(new Item.Properties()));

    // Renewable currency for the Tribute Stone exchange. Drops from knights and hostiles.
    public static final RegistryObject<Item> CROWN_FRAGMENT = ITEMS.register("crown_fragment",
            ()-> new CrownFragmentItem(new Item.Properties()));

    // Gated Tribute Stone reward — right-click to summon The King (final phase only).
    public static final RegistryObject<Item> BOSS_KEY = ITEMS.register("boss_key",
            ()-> new BossKeyItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SLAYER_SWORD = ITEMS.register("slayer_sword",
            () -> new SwordItem(ModToolTeirs.SLAYER, new Item.Properties()
                    .attributes(SwordItem.createAttributes(ModToolTeirs.SLAYER,3, -2.4f))));
     public static final RegistryObject<Item> SLAYER_PICKAXE = ITEMS.register("slayer_pickaxe",
            () -> new PickaxeItem(ModToolTeirs.SLAYER, new Item.Properties()
                    .attributes(PickaxeItem.createAttributes(ModToolTeirs.SLAYER,1, -2.8f))));
     public static final RegistryObject<Item> SLAYER_SHOVEL= ITEMS.register("slayer_shovel",
            () -> new ShovelItem(ModToolTeirs.SLAYER, new Item.Properties()
                    .attributes(ShovelItem.createAttributes(ModToolTeirs.SLAYER,1.5f, -3.0f))));
     public static final RegistryObject<Item> SLAYER_AXE = ITEMS.register("slayer_axe",
            () -> new AxeItem(ModToolTeirs.SLAYER, new Item.Properties()
                    .attributes(AxeItem.createAttributes(ModToolTeirs.SLAYER,6, -3.2f))));
     public static final RegistryObject<Item> SLAYER_HOE = ITEMS.register("slayer_hoe",
            () -> new HoeItem(ModToolTeirs.SLAYER, new Item.Properties()
                    .attributes(HoeItem.createAttributes(ModToolTeirs.SLAYER,0, -3.0f))));

    public static final RegistryObject<Item> SLAYER_BAT = ITEMS.register("slayer_bat",
            () -> new SlayerBatItem(Tiers.WOOD, new Item.Properties()
                    .attributes(SwordItem.createAttributes(Tiers.WOOD, 1, -1.5f))));

    // Combat fishing rod — hooks a player and yanks them toward you (see FishingYoinkHandler).
    public static final RegistryObject<Item> YOINK_ROD = ITEMS.register("yoink_rod",
            () -> new YoinkRodItem(new Item.Properties().durability(256)));

    // Storm Brand — sword that calls a lightning bolt where you aim (right-click).
    public static final RegistryObject<Item> STORM_BRAND = ITEMS.register("storm_brand",
            () -> new StormBrandItem(Tiers.IRON, new Item.Properties()
                    .attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.4f))));

    // King's Maul — mace with a right-click ground-slam shockwave.
    public static final RegistryObject<Item> KINGS_MAUL = ITEMS.register("kings_maul",
            () -> new KingsMaulItem(new Item.Properties().durability(500)
                    .attributes(MaceItem.createAttributes())));

    // Anchor Greaves — boots granting full knockback immunity; counter to all displacement.
    public static final RegistryObject<Item> ANCHOR_CHARM = ITEMS.register("anchor_charm",
            () -> new AnchorCharmItem(new Item.Properties()));

    // Grapple Crossbow — right-click hooks a block and zips you toward it (mobility special).
    public static final RegistryObject<Item> GRAPPLE_CROSSBOW = ITEMS.register("grapple_crossbow",
            () -> new GrappleCrossbowItem(new Item.Properties().durability(384)));

    // Bola — thrown net that Roots a target; the counter to every mobility special.
    public static final RegistryObject<Item> BOLA = ITEMS.register("bola",
            () -> new BolaItem(new Item.Properties().stacksTo(16)));

    // Wind Cannon — right-click cone blast that knocks enemies back (countered by the Anchor Charm).
    public static final RegistryObject<Item> WIND_CANNON = ITEMS.register("wind_cannon",
            () -> new WindCannonItem(new Item.Properties().durability(256)));

    // Shadow Cloak — chestplate; sneak to turn invisible (breaks on strike). See ArmorPerkHandler.
    public static final RegistryObject<Item> SHADOW_CLOAK = ITEMS.register("shadow_cloak",
            () -> new ShadowCloakItem(new Item.Properties()));

    // Truesight Visor — helmet; passive aura that reveals nearby invisible foes. Counter to the Cloak.
    public static final RegistryObject<Item> TRUESIGHT_LENS = ITEMS.register("truesight_lens",
            () -> new TruesightLensItem(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
