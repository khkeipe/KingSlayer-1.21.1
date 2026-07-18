package com.koreykeipe.kingslayer.event;

import com.koreykeipe.kingslayer.KingSlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Makes Storm Brand strikes sound local instead of server-wide.
 *
 * <p>Vanilla {@code LightningBolt} plays its thunder on {@link SoundSource#WEATHER} at volume
 * 10000, which Minecraft treats as "audible essentially everywhere" — fine for a natural storm,
 * wrong for a sword swing, and it drowned out the map every time someone right-clicked. This
 * handler cancels that one sound for bolts the Storm Brand called, so the item can play its own
 * quiet, {@link SoundSource#PLAYERS}-channel replacement at the strike.</p>
 *
 * <p>Natural lightning is untouched: only strikes registered through {@link #markStrike} within
 * the last {@link #STRIKE_WINDOW_TICKS} ticks are suppressed.</p>
 */
@EventBusSubscriber(modid = KingSlayer.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class StormBrandSoundHandler {

    /** The bolt plays its thunder on the tick it spawns; a little slack covers event ordering. */
    private static final long STRIKE_WINDOW_TICKS = 5;

    /** The sound fires from the bolt's own position, so this only guards against float drift. */
    private static final double MATCH_RADIUS_SQ = 4.0;

    private record Strike(double x, double y, double z, long tick) {}

    private static final List<Strike> recentStrikes = new ArrayList<>();

    /** Called by StormBrandItem right after it spawns a bolt, to claim that bolt's thunder. */
    public static void markStrike(Level level, BlockPos pos) {
        long now = level.getGameTime();
        recentStrikes.removeIf(s -> now - s.tick() > STRIKE_WINDOW_TICKS);
        recentStrikes.add(new Strike(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, now));
    }

    @SubscribeEvent
    public static void onLevelSound(PlayLevelSoundEvent.AtPosition event) {
        if (event.getSound() == null) return;
        if (event.getSound().value() != SoundEvents.LIGHTNING_BOLT_THUNDER) return;
        if (event.getSource() != SoundSource.WEATHER) return;
        if (event.getLevel() == null) return;

        long now = event.getLevel().getGameTime();
        double x = event.getPosition().x(), y = event.getPosition().y(), z = event.getPosition().z();

        boolean ours = recentStrikes.stream().anyMatch(s ->
                now - s.tick() <= STRIKE_WINDOW_TICKS
                        && distSqr(s.x() - x, s.y() - y, s.z() - z) <= MATCH_RADIUS_SQ);

        // A natural storm's thunder still gets to be loud and global.
        if (ours) event.setCanceled(true);
    }

    private static double distSqr(double dx, double dy, double dz) {
        return dx * dx + dy * dy + dz * dz;
    }
}
