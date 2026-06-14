package com.koreykeipe.kingslayer.datagen;

import com.koreykeipe.kingslayer.KingSlayer;
import com.koreykeipe.kingslayer.block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, KingSlayer.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        blockWithItem(ModBlocks.BROKEN_CRATE);
        blockWithItem(ModBlocks.COMMON_CRATE);
        blockWithItem(ModBlocks.RARE_CRATE);
        blockWithItem(ModBlocks.EPIC_CRATE);

        blockWithItem(ModBlocks.BROKEN_AD_CRATE);
        blockWithItem(ModBlocks.COMMON_AD_CRATE);
        blockWithItem(ModBlocks.RARE_AD_CRATE);
        blockWithItem(ModBlocks.EPIC_AD_CRATE);

        blockWithItem(ModBlocks.BOUNTY_CRATE);

        // Launch Pad is a half-slab shape — give it a slab model (single texture all faces).
        net.minecraft.resources.ResourceLocation padTex = modLoc("block/launch_pad");
        simpleBlockWithItem(ModBlocks.LAUNCH_PAD.get(),
                models().slab("launch_pad", padTex, padTex, padTex));

        // Tribute Stone reuses the vanilla lodestone model/textures.
        simpleBlockWithItem(ModBlocks.TRIBUTE_STONE.get(),
                new net.minecraftforge.client.model.generators.ModelFile.UncheckedModelFile(mcLoc("block/lodestone")));

        blockWithItem(ModBlocks.MYSTERY_CRATE);
    }

    private void blockWithItem(RegistryObject<Block> blocksRegistryObject){
        simpleBlockWithItem(blocksRegistryObject.get(),cubeAll(blocksRegistryObject.get()));
    }
}
