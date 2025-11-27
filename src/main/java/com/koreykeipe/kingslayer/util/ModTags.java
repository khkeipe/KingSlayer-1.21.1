package com.koreykeipe.kingslayer.util;

import com.koreykeipe.kingslayer.KingSlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModTags {
    public static class Blocks {
        public static final TagKey<Block> NEEDS_SLAYER_TOOL = createTag("needs_slayer_tool");
        public static final TagKey<Block> INCORRECT_FOR_SLAYER_TOOL = createTag("incorrect_for_slayer_tool");

        private static TagKey<Block> createTag(String name){
            return BlockTags.create(ResourceLocation.fromNamespaceAndPath(KingSlayer.MOD_ID, name));
        }
    }

    public static class Items {
        // Custom Tags Get added here.

        private static TagKey<Item> createTag(String name){
            return ItemTags.create(ResourceLocation.fromNamespaceAndPath(KingSlayer.MOD_ID, name));
        }
    }
}
