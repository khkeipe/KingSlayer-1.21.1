package com.koreykeipe.kingslayer.event;

import com.koreykeipe.kingslayer.KingSlayer;
import com.koreykeipe.kingslayer.airdrop.AirdropManager;
import com.koreykeipe.kingslayer.airdrop.AirdropTier;
import com.koreykeipe.kingslayer.game.GameManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.List;

/**
 * Spawns the King's knight mobs near alive Overworld players, gated by the
 * current airdrop tier progress.
 *
 * <h3>Tiers and mobs</h3>
 * <ul>
 *   <li><b>Footsoldier</b> (COMMON, 35%) — Zombie, full iron armour + sword.</li>
 *   <li><b>Champion</b>   (RARE,   60%) — Vindicator, full iron armour, HP ×2.</li>
 *   <li><b>Guard</b>      (EPIC,   85%) — Wither Skeleton, diamond helm/chest, HP ×3.</li>
 * </ul>
 *
 * <h3>Identifying knights</h3>
 * Every spawned knight has {@value #TAG_IS_KNIGHT}{@code =true} in its
 * {@code PersistentData} and {@value #TAG_TIER} set to the tier string.
 * Use {@link #isKnight} and {@link #getKnightTier} from other systems.
 *
 * <h3>Tuning</h3>
 * Adjust {@link #SPAWN_INTERVAL_TICKS}, {@link #SPAWN_CHANCE},
 * {@link #MAX_KNIGHTS_NEAR_PLAYER}, and the distance constants below.
 */
@EventBusSubscriber(modid = KingSlayer.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class KnightSpawnHandler {

    // ------------------------------------------------------------------
    // PersistentData tag keys — public so other systems can read them
    // ------------------------------------------------------------------

    /** Boolean tag present on every knight entity. */
    public static final String TAG_IS_KNIGHT = "ks_knight";
    /** String tag containing the tier name: FOOTSOLDIER, CHAMPION, or GUARD. */
    public static final String TAG_TIER = "ks_knight_tier";

    // ------------------------------------------------------------------
    // Spawn tuning
    // ------------------------------------------------------------------

    /** Server ticks between spawn attempts per player. 450 = 22.5 s. */
    private static final int SPAWN_INTERVAL_TICKS = 450;

    /** Probability of spawning on each attempt (0.0–1.0). */
    private static final float SPAWN_CHANCE = 0.25f;

    /** Maximum knights within this many blocks of a player before skipping spawn. */
    private static final double MAX_KNIGHTS_SEARCH_RADIUS = 64.0;
    private static final int    MAX_KNIGHTS_NEAR_PLAYER   = 3;

    /** Minimum and maximum spawn distance from the target player (blocks). */
    private static final int SPAWN_DIST_MIN = 28;
    private static final int SPAWN_DIST_MAX = 48;

    // ------------------------------------------------------------------
    // Attribute overrides
    // ------------------------------------------------------------------

    private static final double FOOTSOLDIER_MAX_HP = 50.0;
    private static final double CHAMPION_MAX_HP = 50.0;
    private static final double GUARD_MAX_HP    = 60.0;

    // ------------------------------------------------------------------
    // Spawn tick
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!GameManager.get().isGameActive()) return;

        MinecraftServer server = event.getServer();
        if (server.getTickCount() % SPAWN_INTERVAL_TICKS != 0) return;

        AirdropManager am = AirdropManager.get();
        boolean commonFired = am.isTierTriggered(AirdropTier.COMMON);
        if (!commonFired) return; // nothing spawns before 35 % progress

        boolean rareFired = am.isTierTriggered(AirdropTier.RARE);
        boolean epicFired = am.isTierTriggered(AirdropTier.EPIC);

        ServerLevel overworld = server.overworld();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isCreative() || player.isSpectator()) continue;
            if (!player.level().dimension().equals(Level.OVERWORLD)) continue;

            // Skip if already at the knight cap near this player
            if (countNearbyKnights(overworld, player.blockPosition()) >= MAX_KNIGHTS_NEAR_PLAYER) continue;

            if (overworld.random.nextFloat() > SPAWN_CHANCE) continue;

            // Higher tiers available = pick a weighted random tier to spawn
            String tier = pickTier(rareFired, epicFired, overworld);
            BlockPos spawnPos = findSpawnPos(overworld, player.blockPosition());
            if (spawnPos == null) continue;

            Mob knight = buildKnight(tier, overworld);
            if (knight == null) continue;

            tagKnight(knight, tier);
            knight.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                    overworld.random.nextFloat() * 360f, 0f);
            knight.finalizeSpawn(overworld, overworld.getCurrentDifficultyAt(spawnPos),
                    MobSpawnType.MOB_SUMMONED, null);
            // Override any equipment finalizeSpawn may have added
            equip(knight, tier);
            overworld.addFreshEntity(knight);
        }
    }

    // ------------------------------------------------------------------
    // Death detection
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!isKnight(mob)) return;
        if (!GameManager.get().isGameActive()) return;

        String tier = getKnightTier(mob);
        String display = tierDisplayName(tier);

        // Crown Fragment drop (renewable currency) — scales with tier, dropped regardless of killer.
        int frags = switch (tier) {
            case "CHAMPION" -> 2;
            case "GUARD"    -> 3;
            default         -> 1; // FOOTSOLDIER
        };
        mob.spawnAtLocation(new net.minecraft.world.item.ItemStack(
                com.koreykeipe.kingslayer.item.ModItems.CROWN_FRAGMENT.get(), frags));

        // Identify the killer
        net.minecraft.world.entity.Entity killerEntity = event.getSource().getEntity();
        if (killerEntity instanceof ServerPlayer killer) {
            GameManager.get().broadcast(
                    "§6⚔ §a" + killer.getName().getString()
                    + " §ehas slain the " + display + "§e!");
            // Award threat score: Footsoldier=1, Champion=2, Guard=3
            int points = switch (tier) {
                case "CHAMPION" -> 2;
                case "GUARD"    -> 3;
                default         -> 1; // FOOTSOLDIER
            };
            GameManager.get().awardThreatScore(killer.getUUID(), points);
        } else {
            // Killed by environment or another mob — still announce
            GameManager.get().broadcast("§7The " + display + " §7has been slain.");
        }
    }

    // ------------------------------------------------------------------
    // Public helpers
    // ------------------------------------------------------------------

    public static boolean isKnight(Mob mob) {
        return mob.getPersistentData().getBoolean(TAG_IS_KNIGHT);
    }

    public static String getKnightTier(Mob mob) {
        return mob.getPersistentData().getString(TAG_TIER);
    }

    // ------------------------------------------------------------------
    // Internal — tier selection
    // ------------------------------------------------------------------

    /**
     * Picks a tier to spawn. When multiple tiers are available the higher-tier
     * knights are rarer: GUARD 15 %, CHAMPION 30 %, FOOTSOLDIER the rest.
     */
    private static String pickTier(boolean rareFired, boolean epicFired, ServerLevel level) {
        if (epicFired) {
            float r = level.random.nextFloat();
            if (r < 0.15f) return "GUARD";
            if (r < 0.45f) return "CHAMPION";
            return "FOOTSOLDIER";
        }
        if (rareFired) {
            return level.random.nextFloat() < 0.35f ? "CHAMPION" : "FOOTSOLDIER";
        }
        return "FOOTSOLDIER";
    }

    // ------------------------------------------------------------------
    // Internal — mob construction
    // ------------------------------------------------------------------

    private static Mob buildKnight(String tier, ServerLevel level) {
        return switch (tier) {
            case "FOOTSOLDIER" -> {
                Zombie z = new Zombie(EntityType.ZOMBIE, level);
                z.setCustomName(Component.literal("King's Footsoldier")
                        .withStyle(s -> s.withColor(ChatFormatting.GRAY).withBold(false).withItalic(false)));
                z.setCustomNameVisible(true);
                AttributeInstance hp = z.getAttribute(Attributes.MAX_HEALTH);
                if (hp != null) hp.setBaseValue(FOOTSOLDIER_MAX_HP);
                z.setHealth((float) FOOTSOLDIER_MAX_HP);
                z.setPersistenceRequired();
                yield z;
            }
            case "CHAMPION" -> {
                Vindicator v = new Vindicator(EntityType.VINDICATOR, level);
                v.setCustomName(Component.literal("King's Champion")
                        .withStyle(s -> s.withColor(ChatFormatting.GOLD).withBold(false).withItalic(false)));
                v.setCustomNameVisible(true);
                v.setPersistenceRequired();
                // Boost max health
                AttributeInstance hp = v.getAttribute(Attributes.MAX_HEALTH);
                if (hp != null) hp.setBaseValue(CHAMPION_MAX_HP);
                v.setHealth((float) CHAMPION_MAX_HP);
                // Lower base attack so the iron axe doesn't out-hit The King (≈9 total).
                AttributeInstance dmg = v.getAttribute(Attributes.ATTACK_DAMAGE);
                if (dmg != null) dmg.setBaseValue(1.0);
                yield v;
            }
            case "GUARD" -> {
                WitherSkeleton w = new WitherSkeleton(EntityType.WITHER_SKELETON, level);
                w.setCustomName(Component.literal("King's Guard")
                        .withStyle(s -> s.withColor(ChatFormatting.DARK_PURPLE).withBold(false).withItalic(false)));
                w.setCustomNameVisible(true);
                w.setPersistenceRequired();
                // Boost max health
                AttributeInstance hp = w.getAttribute(Attributes.MAX_HEALTH);
                if (hp != null) hp.setBaseValue(GUARD_MAX_HP);
                w.setHealth((float) GUARD_MAX_HP);
                // Lower base attack; its sword + wither DoT still make it dangerous (≈6 + wither).
                AttributeInstance dmg = w.getAttribute(Attributes.ATTACK_DAMAGE);
                if (dmg != null) dmg.setBaseValue(2.0);
                yield w;
            }
            default -> null;
        };
    }

    /** Sets equipment and zeroes all drop chances so knights drop no gear. */
    private static void equip(Mob mob, String tier) {
        switch (tier) {
            case "FOOTSOLDIER" -> {
                mob.setItemSlot(EquipmentSlot.HEAD,      new ItemStack(Items.IRON_HELMET));
                mob.setItemSlot(EquipmentSlot.CHEST,     new ItemStack(Items.IRON_CHESTPLATE));
                mob.setItemSlot(EquipmentSlot.LEGS,      new ItemStack(Items.IRON_LEGGINGS));
                mob.setItemSlot(EquipmentSlot.FEET,      new ItemStack(Items.IRON_BOOTS));
                mob.setItemSlot(EquipmentSlot.MAINHAND,  new ItemStack(Items.IRON_SWORD));
            }
            case "CHAMPION" -> {
                mob.setItemSlot(EquipmentSlot.HEAD,  new ItemStack(Items.IRON_HELMET));
                mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                mob.setItemSlot(EquipmentSlot.LEGS,  new ItemStack(Items.IRON_LEGGINGS));
                mob.setItemSlot(EquipmentSlot.FEET,  new ItemStack(Items.IRON_BOOTS));
                // Vindicator keeps its natural axe — no MAINHAND override
            }
            case "GUARD" -> {
                mob.setItemSlot(EquipmentSlot.HEAD,  new ItemStack(Items.DIAMOND_HELMET));
                mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                // Wither Skeleton keeps its natural stone sword
            }
        }
        // Zero drop chances so no gear falls on death
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            mob.setDropChance(slot, 0f);
        }
    }

    public static void tagKnight(Mob mob, String tier) {
        mob.getPersistentData().putBoolean(TAG_IS_KNIGHT, true);
        mob.getPersistentData().putString(TAG_TIER, tier);
    }

    /**
     * Builds, tags, equips and spawns a single knight of {@code tier} at {@code pos}.
     * Used by The King to summon adds during the boss fight.
     */
    public static void summonKnight(ServerLevel level, BlockPos pos, String tier) {
        Mob knight = buildKnight(tier, level);
        if (knight == null) return;
        tagKnight(knight, tier);
        knight.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                level.random.nextFloat() * 360f, 0f);
        knight.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.MOB_SUMMONED, null);
        equip(knight, tier);
        level.addFreshEntity(knight);
    }

    // ------------------------------------------------------------------
    // Internal — spawn position
    // ------------------------------------------------------------------

    /**
     * Attempts up to 10 times to find a valid surface position within the spawn
     * distance ring around {@code anchor}. Returns {@code null} if none found.
     */
    private static BlockPos findSpawnPos(ServerLevel level, BlockPos anchor) {
        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = level.random.nextDouble() * 2 * Math.PI;
            int dist = SPAWN_DIST_MIN + level.random.nextInt(SPAWN_DIST_MAX - SPAWN_DIST_MIN);
            int x = anchor.getX() + (int)(Math.cos(angle) * dist);
            int z = anchor.getZ() + (int)(Math.sin(angle) * dist);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

            BlockPos candidate = new BlockPos(x, y, z);
            if (level.getBlockState(candidate.below()).isSolid()
                    && level.isEmptyBlock(candidate)
                    && level.isEmptyBlock(candidate.above())) {
                return candidate;
            }
        }
        return null;
    }

    private static int countNearbyKnights(ServerLevel level, BlockPos pos) {
        AABB box = new AABB(pos).inflate(MAX_KNIGHTS_SEARCH_RADIUS);
        List<Mob> nearby = level.getEntitiesOfClass(Mob.class, box, KnightSpawnHandler::isKnight);
        return nearby.size();
    }

    // ------------------------------------------------------------------
    // Internal — display helpers
    // ------------------------------------------------------------------

    private static String tierDisplayName(String tier) {
        return switch (tier) {
            case "FOOTSOLDIER" -> "§7King's Footsoldier";
            case "CHAMPION"    -> "§6King's Champion";
            case "GUARD"       -> "§5King's Guard";
            default            -> "§fKnight";
        };
    }
}
