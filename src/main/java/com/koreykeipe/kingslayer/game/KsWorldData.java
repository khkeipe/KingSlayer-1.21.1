package com.koreykeipe.kingslayer.game;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Per-world persistent flags for KingSlayer. Currently just tracks whether the
 * one-time first-start setup (e.g. defaulting keepInventory on) has run, so it
 * happens exactly once per world and doesn't fight later operator changes.
 */
public class KsWorldData extends SavedData {

    private static final String NAME = "kingslayer_world";

    public boolean firstStartDone = false;

    public static KsWorldData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(KsWorldData::new, KsWorldData::load, null), NAME);
    }

    private static KsWorldData load(CompoundTag tag, HolderLookup.Provider registries) {
        KsWorldData data = new KsWorldData();
        data.firstStartDone = tag.getBoolean("firstStartDone");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("firstStartDone", firstStartDone);
        return tag;
    }
}
