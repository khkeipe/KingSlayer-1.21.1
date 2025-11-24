package com.koreykeipe.kingslayer.datagen;

import com.koreykeipe.kingslayer.KingSlayer;
import com.koreykeipe.kingslayer.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, KingSlayer.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(ModItems.CROWN.get());


        handheldItem(ModItems.SLAYER_SWORD);
        handheldItem(ModItems.SLAYER_PICKAXE);
        handheldItem(ModItems.SLAYER_SHOVEL);
        handheldItem(ModItems.SLAYER_AXE);
        handheldItem(ModItems.SLAYER_HOE);
    }

    private ItemModelBuilder handheldItem(RegistryObject<Item> item) {
        return withExistingParent(item.getId().getPath(),
                ResourceLocation.parse("item/handheld")).texture("layer0",
                ResourceLocation.fromNamespaceAndPath(KingSlayer.MOD_ID, "item/" + item.getId().getPath()));
    }
}
