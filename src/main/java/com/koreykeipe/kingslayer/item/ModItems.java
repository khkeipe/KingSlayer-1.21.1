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
            ()-> new Item(new Item.Properties().food(ModFoodProperties.CROWN)));

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



    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
