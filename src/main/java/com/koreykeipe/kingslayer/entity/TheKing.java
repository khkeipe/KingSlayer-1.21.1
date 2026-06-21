package com.koreykeipe.kingslayer.entity;

import com.koreykeipe.kingslayer.event.KnightSpawnHandler;
import com.koreykeipe.kingslayer.game.GameManager;
import com.koreykeipe.kingslayer.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.warden.SonicBoom;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/**
 * The King — a custom boss built on the Warden. We inherit the Warden's terrifying
 * brain AI (vibration sensing, relentless chase, sonic boom, massive melee) and bolt
 * a boss layer on top: a boss bar, HP-gated phases with enrage buffs, summoned Knight
 * adds, a death ceremony, and the "Slay the King" victory path.
 *
 * <p>Appearance is a scaled-up, royal-retextured Warden (see {@code KingRenderer}).</p>
 */
public class TheKing extends Warden {

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.literal("☠ The King ☠").withStyle(s -> s.withColor(ChatFormatting.DARK_RED).withBold(true)),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);

    /** 1 = >66% HP, 2 = 33–66%, 3 = <33%. Only ever increases. */
    private int phase = 1;
    private int summonCooldown = 200;

    // Ranged attack timers (staggered so they don't all fire at once).
    private int lightningCooldown = 300;
    private int hexCooldown = 160;
    /** Positions queued for a telegraphed lightning strike, and the tick they land. */
    private final java.util.List<BlockPos> pendingBolts = new java.util.ArrayList<>();
    private int boltStrikeTick = 0;

    /** Don't let summoned adds pile up unboundedly. */
    private static final int KNIGHT_CAP = 6;
    private static final double KNIGHT_CAP_RADIUS = 48.0;

    public TheKing(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();          // a boss never despawns
        this.bossEvent.setDarkenScreen(true);
        this.bossEvent.setCreateWorldFog(true);
        this.xpReward = 100;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Warden.createAttributes()
                .add(Attributes.MAX_HEALTH, 500.0)
                .add(Attributes.ATTACK_DAMAGE, 18.0)    // tuned down ~18% — threatening but beatable in diamond
                .add(Attributes.MOVEMENT_SPEED, 0.22)   // heavy, menacing, kitable pace (slower than a vanilla Warden)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 50.0)
                .add(Attributes.SCALE, 1.6);        // towers over players (scales hitbox too)
    }

    // -------------------------------------------------------------------------
    // Boss logic — runs every server AI tick on top of the Warden brain
    // -------------------------------------------------------------------------

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep(); // ticks the Warden's brain (chase + melee)

        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        // The King does NOT blind: strip the Warden's Darkness aura off nearby players
        // each tick (the Warden re-applies it every 120 ticks; this keeps it gone).
        if (this.level() instanceof ServerLevel sl) {
            for (Player p : sl.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(24.0))) {
                p.removeEffect(MobEffects.DARKNESS);
            }
        }
        // Suppress the sonic boom entirely by keeping it perpetually on cooldown, so the
        // behavior's "cooldown absent" start condition is never met (no charge, beam, or damage).
        if (this.tickCount % 20 == 0) {
            SonicBoom.setCooldown(this, 80);
        }

        // Stay locked on the nearest valid player so the King hunts relentlessly and
        // never calms down enough to dig away.
        if (this.tickCount % 40 == 0) {
            Player nearest = this.level().getNearestPlayer(this, 64.0);
            if (nearest != null && nearest.isAlive() && !nearest.isCreative() && !nearest.isSpectator()) {
                this.increaseAngerAt(nearest);
            }
        }

        // The King never fights his own knights. The Warden targets via its anger system, so
        // drop any knight he's locked onto (and the anger driving it) every tick — the
        // KingFactionHandler also cancels any stray damage as a backstop.
        if (this.getTarget() instanceof Mob m && KnightSpawnHandler.isKnight(m)) {
            this.clearAnger(m);
            this.setTarget(null);
            this.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET);
        }

        updatePhase();

        if (--summonCooldown <= 0) {
            summonCooldown = 500 - (phase - 1) * 100; // 25s / 20s / 15s by phase
            summonWave();
        }

        // Telegraphed lightning: spark markers warn where it'll hit, then the bolts land.
        if (boltStrikeTick > 0 && this.level() instanceof ServerLevel sl) {
            for (BlockPos bp : pendingBolts) {
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5, 6, 0.3, 0.6, 0.3, 0.0);
            }
            if (this.tickCount >= boltStrikeTick) {
                strikePendingBolts();
                boltStrikeTick = 0;
            }
        }

        if (--lightningCooldown <= 0) {
            lightningCooldown = 260 + this.random.nextInt(160); // ~13–21s
            chargeLightning();
        }
        if (--hexCooldown <= 0) {
            hexCooldown = 160 + this.random.nextInt(140);        // ~8–15s
            castHex();
        }
    }

    // -------------------------------------------------------------------------
    // Ranged attacks
    // -------------------------------------------------------------------------

    /** Picks strike points scattered around a target player and queues a 1.25s-telegraphed storm. */
    private void chargeLightning() {
        if (!(this.level() instanceof ServerLevel sl)) return;
        Player target = sl.getNearestPlayer(this, 32.0);
        if (target == null) return;

        pendingBolts.clear();
        BlockPos tp = target.blockPosition();
        int strikes = 3 + this.random.nextInt(3); // 3–5 bolts
        for (int i = 0; i < strikes; i++) {
            int x = tp.getX() + this.random.nextInt(11) - 5;
            int z = tp.getZ() + this.random.nextInt(11) - 5;
            int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            pendingBolts.add(new BlockPos(x, y, z));
        }
        boltStrikeTick = this.tickCount + 25; // ~1.25s warning window to dodge
        sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.TRIDENT_THUNDER, SoundSource.HOSTILE, 1.6f, 0.5f);
    }

    private void strikePendingBolts() {
        if (!(this.level() instanceof ServerLevel sl)) { pendingBolts.clear(); return; }
        for (BlockPos bp : pendingBolts) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
            if (bolt != null) {
                bolt.moveTo(bp.getX() + 0.5, bp.getY(), bp.getZ() + 0.5);
                sl.addFreshEntity(bolt);
            }
        }
        pendingBolts.clear();
    }

    /** Spawns a few lingering effect clouds around the King with a random "hex" — the
     *  King is immune to his own hexes (see {@link #canBeAffected}). */
    private void castHex() {
        if (!(this.level() instanceof ServerLevel sl)) return;
        if (sl.getNearestPlayer(this, 24.0) == null) return; // only when engaged

        int clouds = 2 + this.random.nextInt(2); // 2–3
        for (int i = 0; i < clouds; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2;
            double dist = 1.5 + this.random.nextDouble() * 3.0;
            double x = this.getX() + Math.cos(angle) * dist;
            double z = this.getZ() + Math.sin(angle) * dist;

            AreaEffectCloud cloud = new AreaEffectCloud(sl, x, this.getY(), z);
            cloud.setOwner(this);
            cloud.setRadius(2.5f);
            cloud.setDuration(160);          // ~8s
            cloud.setWaitTime(10);
            cloud.setRadiusPerTick(-0.005f);
            cloud.setParticle(ParticleTypes.WITCH);
            cloud.addEffect(randomHexEffect());
            sl.addFreshEntity(cloud);
        }
        sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.2f, 0.6f);
    }

    private MobEffectInstance randomHexEffect() {
        return switch (this.random.nextInt(6)) {
            case 0 -> new MobEffectInstance(MobEffects.WEAKNESS, 200, 0);
            case 1 -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 0);
            case 2 -> new MobEffectInstance(MobEffects.LEVITATION, 30, 0);  // brief float
            case 3 -> new MobEffectInstance(MobEffects.JUMP, 200, 2);       // chaotic jump boost
            case 4 -> new MobEffectInstance(MobEffects.CONFUSION, 140, 0);  // nausea — unsettling
            default -> new MobEffectInstance(MobEffects.GLOWING, 200, 0);   // marks the player
        };
    }

    /** Tame the inherited Warden audio (heartbeat, angry growls, hurt/death, steps) — at scale
     *  1.6 the King is already imposing; the vanilla Warden volume is overwhelming up close. */
    @Override
    protected float getSoundVolume() {
        return 0.5f;
    }

    /** The King is immune to his own hex effects so the clouds never weaken him. */
    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (effect.getEffect() == MobEffects.WEAKNESS
                || effect.getEffect() == MobEffects.MOVEMENT_SLOWDOWN
                || effect.getEffect() == MobEffects.LEVITATION
                || effect.getEffect() == MobEffects.JUMP
                || effect.getEffect() == MobEffects.CONFUSION
                || effect.getEffect() == MobEffects.GLOWING) {
            return false;
        }
        return super.canBeAffected(effect);
    }

    private void updatePhase() {
        float pct = this.getHealth() / this.getMaxHealth();
        int target = pct > 0.66f ? 1 : (pct > 0.33f ? 2 : 3);
        if (target > phase) {
            phase = target;
            onEnterPhase(phase);
        }
    }

    private void onEnterPhase(int p) {
        // Modest final-phase enrage only: a small damage bump, never faster, never stacking.
        if (p >= 3) {
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 999999, 0, false, false));
        }
        this.level().playSound(null, this.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.8f, 0.6f);
        GameManager.get().broadcast(p == 2 ? "§4☠ §cThe King rises in fury! §4☠"
                                            : "§4☠ §cThe King's wrath knows no bounds! §4☠");
        summonWave(); // a bonus wave punctuates the phase change
    }

    private void summonWave() {
        if (!(this.level() instanceof ServerLevel sl)) return;
        AABB box = this.getBoundingBox().inflate(KNIGHT_CAP_RADIUS);
        long nearbyKnights = sl.getEntitiesOfClass(Mob.class, box, KnightSpawnHandler::isKnight).size();
        if (nearbyKnights >= KNIGHT_CAP) return;

        String tier = switch (phase) {
            case 3 -> "GUARD";
            case 2 -> "CHAMPION";
            default -> "FOOTSOLDIER";
        };
        int count = phase; // 1 / 2 / 3 by phase — a measured trickle, not a swarm
        BlockPos base = this.blockPosition();
        for (int i = 0; i < count; i++) {
            int x = base.getX() + sl.random.nextInt(9) - 4;
            int z = base.getZ() + sl.random.nextInt(9) - 4;
            int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            KnightSpawnHandler.summonKnight(sl, new BlockPos(x, y, z), tier);
        }
    }

    // -------------------------------------------------------------------------
    // Boss bar visibility — show to anyone tracking the King
    // -------------------------------------------------------------------------

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    // -------------------------------------------------------------------------
    // Death — ceremony, trophy drops, and the Slay-the-King victory
    // -------------------------------------------------------------------------

    @Override
    public void die(DamageSource source) {
        if (this.level() instanceof ServerLevel sl) {
            this.bossEvent.removeAllPlayers();
            dropTrophies(sl);
            Entity killer = source.getEntity();
            ServerPlayer slayer = killer instanceof ServerPlayer sp ? sp : null;
            GameManager.get().onKingSlain(slayer);
        }
        super.die(source);
    }

    private void dropTrophies(ServerLevel level) {
        spawnAtThrone(level, new ItemStack(ModItems.CROWN.get()));

        ItemStack sword = new ItemStack(ModItems.SLAYER_SWORD.get());
        sword.set(DataComponents.CUSTOM_NAME, Component.literal("King Slayer")
                .withStyle(s -> s.withColor(ChatFormatting.GOLD).withBold(true).withItalic(false)));
        spawnAtThrone(level, sword);
    }

    private void spawnAtThrone(ServerLevel level, ItemStack stack) {
        ItemEntity item = new ItemEntity(level, this.getX(), this.getY() + 1.0, this.getZ(), stack);
        item.setDeltaMovement(0, 0.2, 0);
        level.addFreshEntity(item);
    }
}
