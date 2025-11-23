package com.koreykeipe.kingslayer.item;

import com.koreykeipe.kingslayer.KingSlayer;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, KingSlayer.MOD_ID);

    public static final RegistryObject<Item> CROWN = ITEMS.register("crown",
            ()-> new Item(new Item.Properties().food(ModFoodProperties.CROWN)));


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
