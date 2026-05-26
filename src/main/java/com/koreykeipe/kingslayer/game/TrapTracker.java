package com.koreykeipe.kingslayer.game;

import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks pressure plates (and any future trap blocks) placed by players so that
 * when those traps trigger a kill the correct player receives attribution.
 *
 * Usage pattern:
 *   1. Call recordPressurePlate() when a player places a tracked trap block.
 *   2. Call attributeTntToPlate() when a PrimedTnt entity spawns near a tracked plate.
 *   3. Call getTntPlacer() in ExplosionEvent.Detonate to look up the placer and
 *      register attribution via CombatTracker before the explosion damage is applied.
 *   4. Call removePressurePlate() when the plate is broken so the slot is freed.
 */
public class TrapTracker {

    public record PlacerInfo(UUID uuid, String name) {}

    // BlockPos of each tracked pressure plate -> who placed it
    private static final Map<BlockPos, PlacerInfo> pressurePlates = new HashMap<>();
    // Entity ID of primed TNT -> the placer attributed to it
    private static final Map<Integer, PlacerInfo> tntAttribution = new HashMap<>();

    // -------------------------------------------------------------------------
    // Pressure plate registry
    // -------------------------------------------------------------------------

    public static void recordPressurePlate(BlockPos pos, UUID uuid, String name) {
        pressurePlates.put(pos.immutable(), new PlacerInfo(uuid, name));
    }

    public static void removePressurePlate(BlockPos pos) {
        pressurePlates.remove(pos);
    }

    @Nullable
    public static PlacerInfo getPressurePlatePlacer(BlockPos pos) {
        return pressurePlates.get(pos);
    }

    // -------------------------------------------------------------------------
    // TNT attribution
    // -------------------------------------------------------------------------

    public static void attributeTntToPlate(int tntEntityId, PlacerInfo placer) {
        tntAttribution.put(tntEntityId, placer);
    }

    @Nullable
    public static PlacerInfo getTntPlacer(int tntEntityId) {
        return tntAttribution.get(tntEntityId);
    }

    public static void removeTnt(int tntEntityId) {
        tntAttribution.remove(tntEntityId);
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    public static void clear() {
        pressurePlates.clear();
        tntAttribution.clear();
    }
}
