package com.koreykeipe.kingslayer.event;

import com.koreykeipe.kingslayer.KingSlayer;
import com.koreykeipe.kingslayer.game.CombatTracker;
import com.koreykeipe.kingslayer.game.GameManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.UUID;

/**
 * Credits spawn-egg kills to the player who threw the egg.
 *
 * <p>Spawn eggs are a real weapon in this mod (the loot tables lean on them heavily to keep
 * fights chaotic), but a mob-dealt killing blow is invisible to the kill feed and to the
 * threat score — the thrower got the kill and the game never noticed. This handler closes
 * that gap so egg users climb toward The Marked like anyone else.</p>
 *
 * <p>Flow: a right-click holding a spawn egg arms a short-lived {@link PendingSpawn}; the
 * mob that appears on the same tick is stamped with the thrower's UUID in its persistent
 * data (so ownership survives chunk unload and server restarts); any damage that mob later
 * deals to a player registers a {@link CombatTracker} attribution. Kill credit and the
 * resulting threat points then fall out of the existing resolveKill pipeline — no special
 * casing needed in {@link GameManager}.</p>
 */
@EventBusSubscriber(modid = KingSlayer.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class SpawnEggHandler {

    /** Persistent-data keys stamped onto a summoned mob. */
    private static final String OWNER_UUID_KEY = "kcs_egg_owner";
    private static final String OWNER_NAME_KEY = "kcs_egg_owner_name";

    /**
     * Priority 10 = "intentional setup", matching pressure-plate traps. Throwing an egg at
     * someone is a deliberate act, so it should beat an older stray-arrow tag but still lose
     * to a player actively landing hits in the last 10 seconds.
     */
    private static final int ATTRIBUTION_PRIORITY = 10;

    /** How far from the click a mob may appear and still be considered the egg's spawn. */
    private static final double SPAWN_MATCH_RADIUS_SQ = 100.0; // 10 blocks

    /**
     * The interact event fires immediately before the item resolves, so the mob joins the
     * level on the same tick. One tick of slack absorbs any ordering quirk without letting
     * an unrelated natural spawn steal the tag.
     */
    private static final long SPAWN_MATCH_TICKS = 1;

    private record PendingSpawn(UUID uuid, String name, double x, double y, double z, long tick) {}

    /** Single slot is enough — the window is one tick, and eggs resolve synchronously. */
    private static PendingSpawn pending = null;

    // -------------------------------------------------------------------------
    // 1. Arm on right-click
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        arm(event);
    }

    /** Eggs used on water, on another mob, or into open air don't fire RightClickBlock. */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        arm(event);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        arm(event);
    }

    private static void arm(PlayerInteractEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getItemStack().getItem() instanceof SpawnEggItem)) return;
        if (!GameManager.get().isGameActive()) return;

        pending = new PendingSpawn(
                player.getUUID(), player.getName().getString(),
                player.getX(), player.getY(), player.getZ(),
                player.level().getGameTime());
    }

    // -------------------------------------------------------------------------
    // 2. Stamp the mob that appears
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (pending == null) return;
        if (!(event.getEntity() instanceof Mob mob)) return;

        PendingSpawn p = pending;
        if (event.getLevel().getGameTime() - p.tick() > SPAWN_MATCH_TICKS) {
            pending = null;   // stale — a natural spawn, not ours
            return;
        }
        if (mob.distanceToSqr(p.x(), p.y(), p.z()) > SPAWN_MATCH_RADIUS_SQ) return;

        CompoundTag data = mob.getPersistentData();
        data.putUUID(OWNER_UUID_KEY, p.uuid());
        data.putString(OWNER_NAME_KEY, p.name());

        // One egg, one mob. Clearing here stops a single click from claiming a mob that
        // some other system spawns later in the same tick.
        pending = null;
    }

    // -------------------------------------------------------------------------
    // 3. Attribute damage the summoned mob deals
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        if (!GameManager.get().isGameActive()) return;

        // getEntity() resolves a projectile back to its shooter, so a summoned skeleton's
        // arrows credit the egg thrower too.
        if (!(event.getSource().getEntity() instanceof Mob mob)) return;

        CompoundTag data = mob.getPersistentData();
        if (!data.hasUUID(OWNER_UUID_KEY)) return;

        UUID owner = data.getUUID(OWNER_UUID_KEY);
        if (owner.equals(victim.getUUID())) return;   // your own egg turned on you — that's on you

        CombatTracker.registerAttribution(
                victim.getUUID(), owner, data.getString(OWNER_NAME_KEY),
                mob.getType().getDescription().getString() + " (summoned)",
                ATTRIBUTION_PRIORITY);
    }
}
