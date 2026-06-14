package com.koreykeipe.kingslayer.game;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Per-world persistent state for KingSlayer. The event runs as one continuous tournament
 * that spans as many sessions (server restarts) as it takes to reach a victory — so the
 * roster, per-player death counts, and finale flags all survive restarts here. The only
 * true reset is starting a brand-new world (fresh save data).
 */
public class KsWorldData extends SavedData {

    private static final String NAME = "kingslayer_world";

    /** One-time first-start setup guard (e.g. defaulting keepInventory on). */
    public boolean firstStartDone = false;

    /** Every player who has joined the event (online or not) — the full roster. */
    public final Set<UUID> participants = new LinkedHashSet<>();

    /** Per-participant death count this event. Elimination latches at 5. Survives restarts. */
    public final Map<UUID, Integer> deaths = new HashMap<>();

    /** Last-known name per participant, so an offline winner can still be announced. */
    public final Map<UUID, String> names = new HashMap<>();

    /** True once The King has been summoned (by key or auto-finale) — auto-summon fires once. */
    public boolean kingSummoned = false;

    /** True once a victor (or draw) has been declared — the event is over until a new world. */
    public boolean concluded = false;

    public static KsWorldData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(KsWorldData::new, KsWorldData::load, null), NAME);
    }

    private static KsWorldData load(CompoundTag tag, HolderLookup.Provider registries) {
        KsWorldData data = new KsWorldData();
        data.firstStartDone = tag.getBoolean("firstStartDone");
        data.kingSummoned   = tag.getBoolean("kingSummoned");
        data.concluded      = tag.getBoolean("concluded");

        for (Tag t : tag.getList("participants", Tag.TAG_STRING)) {
            data.participants.add(UUID.fromString(t.getAsString()));
        }
        for (Tag t : tag.getList("deaths", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            data.deaths.put(UUID.fromString(c.getString("id")), c.getInt("n"));
        }
        for (Tag t : tag.getList("names", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            data.names.put(UUID.fromString(c.getString("id")), c.getString("name"));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("firstStartDone", firstStartDone);
        tag.putBoolean("kingSummoned", kingSummoned);
        tag.putBoolean("concluded", concluded);

        ListTag plist = new ListTag();
        for (UUID id : participants) plist.add(net.minecraft.nbt.StringTag.valueOf(id.toString()));
        tag.put("participants", plist);

        ListTag dlist = new ListTag();
        for (Map.Entry<UUID, Integer> e : deaths.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putString("id", e.getKey().toString());
            c.putInt("n", e.getValue());
            dlist.add(c);
        }
        tag.put("deaths", dlist);

        ListTag nlist = new ListTag();
        for (Map.Entry<UUID, String> e : names.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putString("id", e.getKey().toString());
            c.putString("name", e.getValue());
            nlist.add(c);
        }
        tag.put("names", nlist);
        return tag;
    }
}
