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

        blockWithItem(ModBlocks.TRIBUTE_STONE);
    }

    private void blockWithItem(RegistryObject<Block> blocksRegistryObject){
        simpleBlockWithItem(blocksRegistryObject.get(),cubeAll(blocksRegistryObject.get()));
    }
}
