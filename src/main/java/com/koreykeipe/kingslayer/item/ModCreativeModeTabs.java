package com.koreykeipe.kingslayer.item;

import com.koreykeipe.kingslayer.KingSlayer;
import com.koreykeipe.kingslayer.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, KingSlayer.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> KINGSLAYER_ITEMS_TAB = CREATIVE_MODE_TABS.register("kingslayer_items_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.CROWN.get()))
                    .title(Component.translatable("creativetab.kcs_kingslayer.kingslayer_items"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModItems.CROWN.get());
                        output.accept(ModItems.CROWN_FRAGMENT.get());
                        output.accept(ModItems.BOSS_KEY.get());
                        output.accept(ModBlocks.TRIBUTE_STONE.get());
                        output.accept(ModBlocks.MYSTERY_CRATE.get());
                        output.accept(ModBlocks.BROKEN_CRATE.get());
                        output.accept(ModBlocks.COMMON_CRATE.get());
                        output.accept(ModBlocks.RARE_CRATE.get());
                        output.accept(ModBlocks.EPIC_CRATE.get());
                        output.accept(ModBlocks.LAUNCH_PAD.get());

                        output.accept(ModItems.SLAYER_SWORD.get());
                        output.accept(ModItems.SLAYER_PICKAXE.get());
                        output.accept(ModItems.SLAYER_SHOVEL.get());
                        output.accept(ModItems.SLAYER_AXE.get());
                        output.accept(ModItems.SLAYER_HOE.get());
                        output.accept(ModItems.SLAYER_BAT.get());
                        output.accept(ModItems.YOINK_ROD.get());
                        output.accept(ModItems.STORM_BRAND.get());
                        output.accept(ModItems.KINGS_MAUL.get());
                        output.accept(ModItems.ANCHOR_CHARM.get());
                        output.accept(ModItems.GRAPPLE_CROSSBOW.get());
                        output.accept(ModItems.BOLA.get());
                        output.accept(ModItems.WIND_CANNON.get());
                        output.accept(ModItems.SHADOW_CLOAK.get());
                        output.accept(ModItems.TRUESIGHT_LENS.get());
                        output.accept(ModItems.SUNDER_PIKE.get());
                        output.accept(ModItems.BULWARK_LEGGUARDS.get());

                    }).build());


    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
