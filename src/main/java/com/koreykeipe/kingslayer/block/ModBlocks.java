package com.koreykeipe.kingslayer.block;

import com.koreykeipe.kingslayer.KingSlayer;
import com.koreykeipe.kingslayer.item.ModItems;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, KingSlayer.MOD_ID);

    // Loot Crates
    public static final RegistryObject<Block> BROKEN_CRATE = registerBlock("broken_crate",
        () -> new DropExperienceBlock(UniformInt.of(1, 1), BlockBehaviour.Properties.of()
            .strength(0.4f).sound(SoundType.WOOD)));

    public static final RegistryObject<Block> COMMON_CRATE = registerBlock("common_crate",
        () -> new DropExperienceBlock(UniformInt.of(2, 4), BlockBehaviour.Properties.of()
            .strength(0.8f).sound(SoundType.CHAIN)));

    public static final RegistryObject<Block> RARE_CRATE = registerBlock("rare_crate",
        () -> new DropExperienceBlock(UniformInt.of(4, 8), BlockBehaviour.Properties.of()
            .strength(1.5f).sound(SoundType.ANCIENT_DEBRIS)));

    public static final RegistryObject<Block> EPIC_CRATE = registerBlock("epic_crate",
        () -> new DropExperienceBlock(UniformInt.of(8, 16), BlockBehaviour.Properties.of()
            .strength(2.5f).sound(SoundType.NETHERITE_BLOCK)));

    // Airdrop Crates
    public static final RegistryObject<Block> BROKEN_AD_CRATE = registerBlock("broken_airdrop_crate",
            () -> new DropExperienceBlock(UniformInt.of(1, 1), BlockBehaviour.Properties.of()
                    .strength(0.4f).sound(SoundType.WOOD)));
    public static final RegistryObject<Block> COMMON_AD_CRATE = registerBlock("common_airdrop_crate",
            () -> new DropExperienceBlock(UniformInt.of(2, 4), BlockBehaviour.Properties.of()
                    .strength(0.8f).sound(SoundType.METAL)));

    public static final RegistryObject<Block> RARE_AD_CRATE = registerBlock("rare_airdrop_crate",
            () -> new DropExperienceBlock(UniformInt.of(4, 8), BlockBehaviour.Properties.of()
                    .strength(1.5f).sound(SoundType.ANCIENT_DEBRIS)));

    public static final RegistryObject<Block> EPIC_AD_CRATE = registerBlock("epic_airdrop_crate",
            () -> new DropExperienceBlock(UniformInt.of(8, 16), BlockBehaviour.Properties.of()
                    .strength(2.0f).sound(SoundType.NETHERITE_BLOCK)));

    // Bounty Crate — the special reward for slaying THE MARKED. Not part of the
    // airdrop-tier progression; it is handed directly to the killer's inventory
    // (see GameManager#handleMarkedKilled), who places and breaks it to claim the
    // richest loot table in the game.
    public static final RegistryObject<Block> BOUNTY_CRATE = registerBlock("bounty_crate",
            () -> new DropExperienceBlock(UniformInt.of(10, 20), BlockBehaviour.Properties.of()
                    .strength(2.0f).sound(SoundType.NETHERITE_BLOCK)));

    // Tribute Stone — indestructible exchange hub placed at world spawn (bedrock-grade
    // strength so it can't be mined or blown up). See TributeStoneBlock / TributeExchange.
    public static final RegistryObject<Block> TRIBUTE_STONE = registerBlock("tribute_stone",
            () -> new TributeStoneBlock(BlockBehaviour.Properties.of()
                    .strength(-1.0f, 3600000.0f)
                    .sound(SoundType.ANCIENT_DEBRIS)
                    .requiresCorrectToolForDrops()
                    .lightLevel(s -> 10)));

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, RegistryObject<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
