package com.koreykeipe.kingslayer.item;

import com.koreykeipe.kingslayer.KingSlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, KingSlayer.MOD_ID);

    public static final RegistryObject<CreativeModeTab> KINGSLAYER_ITEMS_TAB = CREATIVE_MODE_TABS.register("kingslayer_items_tabb",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.CROWN.get()))
                    .title(Component.translatable("creativetab.kcs_kingslayer.kingslayer_items"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModItems.CROWN.get());

                    }).build());


    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
