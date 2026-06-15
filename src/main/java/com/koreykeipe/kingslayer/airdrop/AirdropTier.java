package com.koreykeipe.kingslayer.airdrop;

import com.koreykeipe.kingslayer.block.ModBlocks;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The four escalating tiers of airdrops, each mapped to a crate block
 * used for the falling entity renderer.
 */
public enum AirdropTier {
    BROKEN ("Broken", "§7",    ModBlocks.BROKEN_AD_CRATE),
    COMMON ("Common", "§a",    ModBlocks.COMMON_AD_CRATE),
    RARE   ("Rare",   "§9",    ModBlocks.RARE_AD_CRATE),
    EPIC   ("Epic",   "§5§l",  ModBlocks.EPIC_AD_CRATE);

    private final String displayName;
    private final String color;        // chat format prefix
    private final DeferredHolder<Block, Block> crate;

    AirdropTier(String displayName, String color, DeferredHolder<Block, Block> crate) {
        this.displayName = displayName;
        this.color = color;
        this.crate = crate;
    }

    public String getDisplayName()        { return displayName; }
    public String getColor()              { return color; }
    public DeferredHolder<Block, Block> getCrate() { return crate; }

    /** Returns a chat-formatted label, e.g. "§aCommon Airdrop§r". */
    public String coloredName() {
        return color + displayName + " Airdrop§r";
    }
}
