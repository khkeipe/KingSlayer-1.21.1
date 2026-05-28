package com.koreykeipe.kingslayer.airdrop;

import com.koreykeipe.kingslayer.KingSlayer;
import com.koreykeipe.kingslayer.entity.ModEntityTypes;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.Level;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.CampfireBlock;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A falling entity that descends from the sky, trailing smoke and flame particles,
 * then places a named loot chest on landing, spawns fireworks, and leaves a glowing
 * Slime marker over the chest for a configurable duration.
 */
public class AirdropEntity extends Entity {

    private static final EntityDataAccessor<String> DATA_TIER =
            SynchedEntityData.defineId(AirdropEntity.class, EntityDataSerializers.STRING);

    /** Gravity applied every tick (blocks/tick²). */
    private static final double GRAVITY = 0.04;
    /**
     * Discard after this many ticks if landing never fires (safety valve).
     * Must exceed the worst-case fall time: max SPAWN_HEIGHT (256) ÷ min FALL_SPEED (0.1) ≈ 2 560 ticks.
     * 6 000 ticks (5 minutes) comfortably covers every valid config combination.
     */
    private static final int TIMEOUT_TICKS = 6000;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public AirdropEntity(EntityType<AirdropEntity> type, Level level) {
        super(type, level);
    }

    /** Server-side factory: create and position a new airdrop before adding to the level. */
    public AirdropEntity(AirdropTier tier, Level level, double x, double y, double z) {
        this(ModEntityTypes.AIRDROP.get(), level);
        setPos(x, y, z);
        setTier(tier);
    }

    // -------------------------------------------------------------------------
    // Synced data
    // -------------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_TIER, AirdropTier.COMMON.name());
    }

    public AirdropTier getTier() {
        try {
            return AirdropTier.valueOf(this.entityData.get(DATA_TIER));
        } catch (IllegalArgumentException e) {
            return AirdropTier.COMMON;
        }
    }

    public void setTier(AirdropTier tier) {
        this.entityData.set(DATA_TIER, tier.name());
    }

    // -------------------------------------------------------------------------
    // NBT persistence
    // -------------------------------------------------------------------------

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("airdrop_tier")) {
            try {
                setTier(AirdropTier.valueOf(tag.getString("airdrop_tier")));
            } catch (IllegalArgumentException e) {
                setTier(AirdropTier.COMMON);
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("airdrop_tier", getTier().name());
    }

    // -------------------------------------------------------------------------
    // Physics & effects
    // -------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();

        // Safety timeout
        if (!this.level().isClientSide() && this.tickCount > TIMEOUT_TICKS) {
            KingSlayer.LOGGER.warn("AirdropEntity timed out without landing — discarding.");
            this.discard();
            return;
        }

        // Apply gravity, capped by the configurable terminal velocity
        double maxFallSpeed = AirdropConfig.FALL_SPEED.get();
        double vy = this.getDeltaMovement().y - GRAVITY;
        if (vy < -maxFallSpeed) vy = -maxFallSpeed;
        this.setDeltaMovement(0.0, vy, 0.0);
        this.move(MoverType.SELF, this.getDeltaMovement());

        if (this.level().isClientSide()) {
            // Smoke + flame trail every 3 ticks
            if (this.tickCount % 3 == 0) {
                double ox = (this.random.nextDouble() - 0.5) * 0.4;
                double oz = (this.random.nextDouble() - 0.5) * 0.4;
                this.level().addParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                        this.getX() + ox, this.getY(), this.getZ() + oz, 0.0, 0.08, 0.0);
                this.level().addParticle(ParticleTypes.FLAME,
                        this.getX() + ox, this.getY(), this.getZ() + oz, 0.0, 0.04, 0.0);
            }
        } else {
            // Periodic whoosh sound broadcast to nearby players
            if (this.tickCount % 40 == 0) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.NEUTRAL, 1.2f, 0.7f);
            }

            // Water landing — catch the entity the moment it enters any fluid so it
            // doesn't sink to the seafloor before triggering the landing logic.
            if (this.isInWater()) {
                BlockPos surface = findWaterSurface((ServerLevel) this.level(), this.blockPosition());
                onLand((ServerLevel) this.level(), surface);
                this.discard();
                return;
            }

            // Landing detection — onGround() is set by move() when the entity hits a floor
            if (this.onGround()) {
                onLand((ServerLevel) this.level(), this.blockPosition());
                this.discard();
            }
        }
    }

    // -------------------------------------------------------------------------
    // Landing logic (server-side only)
    // -------------------------------------------------------------------------

    private void onLand(ServerLevel level, BlockPos landPos) {
        // Determine whether this is a water landing (entity entered fluid before hitting the floor)
        boolean waterLanding = level.getFluidState(landPos).is(FluidTags.WATER);

        BlockPos campfirePos = null; // non-null for land landings; null for water landings
        BlockPos chestPos;

        if (waterLanding) {
            // Float the chest just above the water surface — no campfire in water
            chestPos = landPos.above();
        } else {
            // Land: campfire at the placement slot, chest sits on top of it
            campfirePos = findPlacementPos(level, landPos);
            if (campfirePos == null) {
                KingSlayer.LOGGER.warn("AirdropEntity: no valid placement position near {} — skipping chest.", landPos);
                return;
            }
            chestPos = campfirePos.above();
        }

        AirdropTier tier = getTier();

        // 1 — Place a lit signal campfire (land landings only — produces tall smoke beacon)
        if (campfirePos != null) {
            level.setBlockAndUpdate(campfirePos,
                    Blocks.CAMPFIRE.defaultBlockState()
                            .setValue(CampfireBlock.SIGNAL_FIRE, true));
        }

        // 2 — Place and name the chest.
        // setCustomName is inaccessible in 1.21.1 through the public API, so we write
        // the custom name the same way the game itself does it: via NBT load.
        // This is called before fillChest so the empty-items load is harmless.
        level.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
        BlockEntity be = level.getBlockEntity(chestPos);
        if (be != null) {
            CompoundTag nameTag = new CompoundTag();
            nameTag.putString("CustomName",
                    Component.Serializer.toJson(buildChestName(tier), level.registryAccess()));
            be.loadWithComponents(nameTag, level.registryAccess());

            if (be instanceof ChestBlockEntity chest) {
                fillChest(chest);
            }
        }

        // 3 — Landing thud
        level.playSound(null, chestPos, SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 1.5f, 0.6f);

        // 4 — Firework burst (4 rockets, slight random spread)
        spawnFireworks(level, chestPos, tier);

        // 5 — Glowing Slime marker (invisible, cube hitbox fits the chest)
        spawnGlowMarker(level, chestPos, campfirePos);

        // 6 — Announce landing coordinates
        String msg = "§6§l☆ " + tier.coloredName()
                + " §ehas landed at §f("
                + chestPos.getX() + ", " + chestPos.getY() + ", " + chestPos.getZ()
                + ")§e!";
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(Component.literal(msg));
        }

        KingSlayer.LOGGER.info("KingSlayer Airdrop: {} landed at {}", tier.getDisplayName(), chestPos);
    }

    // -------------------------------------------------------------------------
    // Chest naming
    // -------------------------------------------------------------------------

    /** Builds a styled chest title shown when the player opens it. */
    private static Component buildChestName(AirdropTier tier) {
        ChatFormatting color = switch (tier) {
            case BROKEN -> ChatFormatting.GRAY;
            case COMMON -> ChatFormatting.GREEN;
            case RARE   -> ChatFormatting.BLUE;
            case EPIC   -> ChatFormatting.DARK_PURPLE;
        };
        boolean bold = (tier == AirdropTier.EPIC);
        return Component.literal("✦ " + tier.getDisplayName() + " Airdrop")
                .withStyle(style -> style.withColor(color).withBold(bold).withItalic(false));
    }

    // -------------------------------------------------------------------------
    // Fireworks
    // -------------------------------------------------------------------------

    private void spawnFireworks(ServerLevel level, BlockPos origin, AirdropTier tier) {
        int color = switch (tier) {
            case BROKEN -> 0x999999;  // gray
            case COMMON -> 0x55FF55;  // bright green
            case RARE   -> 0x5555FF;  // blue
            case EPIC   -> 0xAA00AA;  // purple
        };

        // Build a colored firework item using 1.21.1 DataComponents
        FireworkExplosion explosion = new FireworkExplosion(
                FireworkExplosion.Shape.LARGE_BALL,
                IntArrayList.of(color),
                new IntArrayList(),   // no fade
                true,                 // trail
                true                  // twinkle
        );
        ItemStack rocketItem = new ItemStack(Items.FIREWORK_ROCKET);
        rocketItem.set(DataComponents.FIREWORKS, new Fireworks(1, List.of(explosion)));

        // Launch 4 rockets in a small spread
        for (int i = 0; i < 4; i++) {
            double ox = (this.random.nextDouble() - 0.5) * 1.2;
            double oz = (this.random.nextDouble() - 0.5) * 1.2;
            FireworkRocketEntity rocket = new FireworkRocketEntity(
                    level,
                    origin.getX() + 0.5 + ox,
                    origin.getY(),
                    origin.getZ() + 0.5 + oz,
                    rocketItem.copy()
            );
            // Give each rocket an upward boost so they shoot up before exploding
            rocket.setDeltaMovement(
                    ox * 0.1,
                    0.25 + this.random.nextDouble() * 0.25,
                    oz * 0.1
            );
            level.addFreshEntity(rocket);
        }
    }

    // -------------------------------------------------------------------------
    // Glow marker
    // -------------------------------------------------------------------------

    private void spawnGlowMarker(ServerLevel level, BlockPos chestPos, @Nullable BlockPos campfirePos) {
        // A size-2 Slime has a ~1×1×1 block hitbox — the glow outline fits the chest
        // far better than an ArmorStand's tall/narrow humanoid silhouette.
        // It's invisible, has AI disabled, and can't be targeted or damaged.
        Slime marker = new Slime(EntityType.SLIME, level);
        marker.setSize(2, false);
        // Centre the slime's hitbox over the chest block
        marker.setPos(chestPos.getX() + 0.5, chestPos.getY(), chestPos.getZ() + 0.5);
        marker.setInvisible(true);
        marker.setNoGravity(true);
        marker.setInvulnerable(true);
        marker.setSilent(true);
        marker.setNoAi(true);
        // Prevent natural despawn — AirdropManager will explicitly discard it after GLOW_DURATION
        marker.setPersistenceRequired();
        // setGlowing() is inaccessible in 1.21.1; MobEffects.GLOWING achieves the same result.
        marker.addEffect(new MobEffectInstance(
                MobEffects.GLOWING, AirdropConfig.GLOW_DURATION.get(), 0, false, false));
        level.addFreshEntity(marker);

        AirdropManager.get().trackGlowMarker(marker.getUUID(), level.getServer(), campfirePos);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private BlockPos findPlacementPos(ServerLevel level, BlockPos startPos) {
        for (int offset = 0; offset <= 5; offset++) {
            BlockPos candidate = startPos.above(offset);
            if (level.isEmptyBlock(candidate) && level.isEmptyBlock(candidate.above())) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Starting from {@code start} (which must be inside water), climbs upward until
     * the block above is no longer water, then returns the topmost water block.
     * The caller should place the chest one block above the returned position.
     */
    private static BlockPos findWaterSurface(ServerLevel level, BlockPos start) {
        BlockPos pos = start;
        // Walk up while the next block is still water
        while (level.getFluidState(pos.above()).is(FluidTags.WATER)) {
            pos = pos.above();
            if (pos.getY() >= level.getMaxBuildHeight() - 1) break;
        }
        return pos; // topmost water block; pos.above() is air (or build-height cap)
    }

    private void fillChest(ChestBlockEntity chest) {
        AirdropConfig.TierConfig cfg = switch (getTier()) {
            case BROKEN -> AirdropConfig.BROKEN;
            case COMMON -> AirdropConfig.COMMON;
            case RARE   -> AirdropConfig.RARE;
            case EPIC   -> AirdropConfig.EPIC;
        };

        List<? extends String> entries = cfg.loot.get();
        int slot = 0;

        for (String entry : entries) {
            if (slot >= chest.getContainerSize()) break;
            String[] parts = entry.trim().split("\\s+");
            if (parts.length != 4) continue;

            try {
                ResourceLocation id = ResourceLocation.tryParse(parts[0]);
                double chance       = Double.parseDouble(parts[1]);
                int    min          = Integer.parseInt(parts[2]);
                int    max          = Integer.parseInt(parts[3]);

                if (this.random.nextDouble() < chance) {
                    Item item = ForgeRegistries.ITEMS.getValue(id);
                    if (item != null && item != Items.AIR) {
                        int count = (max <= min) ? min : min + this.random.nextInt(max - min + 1);
                        chest.setItem(slot++, new ItemStack(item, count));
                    }
                }
            } catch (NumberFormatException e) {
                KingSlayer.LOGGER.warn("AirdropEntity: malformed loot entry '{}' — skipping.", entry);
            }
        }
    }
}
