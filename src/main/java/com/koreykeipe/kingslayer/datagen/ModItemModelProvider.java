package com.koreykeipe.kingslayer.datagen;

import com.koreykeipe.kingslayer.KingSlayer;
import com.koreykeipe.kingslayer.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, KingSlayer.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(ModItems.CROWN.get());
        basicItem(ModItems.CROWN_FRAGMENT.get());
        vanillaFlat(ModItems.BOSS_KEY, "item/trial_key");


        handheldItem(ModItems.SLAYER_SWORD);
        handheldItem(ModItems.SLAYER_PICKAXE);
        handheldItem(ModItems.SLAYER_SHOVEL);
        handheldItem(ModItems.SLAYER_AXE);
        handheldItem(ModItems.SLAYER_HOE);
        handheldItem(ModItems.SLAYER_BAT);

        basicItem(ModItems.YOINK_ROD.get());
        handheldItem(ModItems.KINGS_MAUL);

        // These reuse vanilla item textures rather than custom art.
        vanillaFlat(ModItems.WIND_CANNON, "item/feather");
        vanillaFlat(ModItems.STORM_BRAND, "item/nether_star");
        vanillaHandheld(ModItems.GRAPPLE_CROSSBOW, "item/crossbow_standby");
        vanillaFlat(ModItems.BOLA, "item/lead");
        vanillaFlat(ModItems.ANCHOR_CHARM, "item/netherite_boots");
        vanillaFlat(ModItems.SHADOW_CLOAK, "item/netherite_chestplate");
        vanillaFlat(ModItems.TRUESIGHT_LENS, "item/netherite_helmet");
        vanillaHandheld(ModItems.SUNDER_PIKE, "item/trident");
        vanillaFlat(ModItems.BULWARK_LEGGUARDS, "item/netherite_leggings");
    }

    private ItemModelBuilder handheldItem(DeferredHolder<Item, Item> item) {
        return withExistingParent(item.getId().getPath(),
                ResourceLocation.parse("item/handheld")).texture("layer0",
                ResourceLocation.fromNamespaceAndPath(KingSlayer.MOD_ID, "item/" + item.getId().getPath()));
    }

    /** A flat (item/generated) model whose icon is a vanilla texture, e.g. "item/lead". */
    private ItemModelBuilder vanillaFlat(DeferredHolder<Item, Item> item, String mcTexture) {
        return withExistingParent(item.getId().getPath(), mcLoc("item/generated"))
                .texture("layer0", mcLoc(mcTexture));
    }

    /** A held (item/handheld) model whose icon is a vanilla texture, e.g. "item/crossbow_standby". */
    private ItemModelBuilder vanillaHandheld(DeferredHolder<Item, Item> item, String mcTexture) {
        return withExistingParent(item.getId().getPath(), mcLoc("item/handheld"))
                .texture("layer0", mcLoc(mcTexture));
    }
}
