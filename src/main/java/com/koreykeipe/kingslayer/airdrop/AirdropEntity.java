package com.koreykeipe.kingslayer.airdrop;

import com.koreykeipe.kingslayer.KingSlayer;
import com.koreykeipe.kingslayer.entity.ModEntityTypes;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * A falling entity that descends from the sky, trailing smoke and flame particles,
 * then places a loot chest on landing.
 *
 * <p>Tier is synced to the client so the renderer can display the matching crate model.</p>
 */
public class AirdropEntity extends Entity {

    private static final EntityDataAccessor<String> DATA_TIER =
            SynchedEntityData.defineId(AirdropEntity.class, EntityDataSerializers.STRING);

    /** Maximum fall speed (blocks/tick). */
    private static final double MAX_FALL_SPEED = 1.5;
    /** Gravity applied every tick (blocks/tick²). */
    private static final double GRAVITY = 0.04;
    /** Discard after this many ticks if landing never fires (safety valve). */
    private static final int TIMEOUT_TICKS = 1200; // 60 seconds

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

        // Apply gravity and cap fall speed
        double vy = this.getDeltaMovement().y - GRAVITY;
        if (vy < -MAX_FALL_SPEED) vy = -MAX_FALL_SPEED;
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
        BlockPos chestPos = findPlacementPos(level, landPos);
        if (chestPos == null) {
            KingSlayer.LOGGER.warn("AirdropEntity: no valid placement position near {} — skipping chest.", landPos);
            return;
        }

        // Place the chest
        level.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
        BlockEntity be = level.getBlockEntity(chestPos);
        if (be instanceof ChestBlockEntity chest) {
            fillChest(chest);
        }

        // Landing burst — explosion particles and anvil thud
        level.playSound(null, chestPos, SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 1.5f, 0.6f);

        // Announce landing coordinates
        AirdropTier tier = getTier();
        String msg = "§6§l☆ " + tier.coloredName()
                + " §ehas landed at §f("
                + chestPos.getX() + ", " + chestPos.getY() + ", " + chestPos.getZ()
                + ")§e!";
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(Component.literal(msg));
        }

        KingSlayer.LOGGER.info("KingSlayer Airdrop: {} landed at {}", tier.getDisplayName(), chestPos);
    }

    /**
     * Finds the lowest empty block at or above {@code startPos} that has a clear block above it.
     * This handles both "entity is standing in air" and "entity just clipped into the ground" cases.
     */
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
     * Rolls loot from the tier's config and fills the chest.
     * Each config entry: {@code "namespace:item_id chance min max"}.
     */
    private void fillChest(ChestBlockEntity chest) {
        AirdropConfig.TierConfig cfg = switch (getTier()) {
            case COMMON    -> AirdropConfig.COMMON;
            case RARE      -> AirdropConfig.RARE;
            case EPIC      -> AirdropConfig.EPIC;
            case LEGENDARY -> AirdropConfig.LEGENDARY;
        };

        List<? extends String> entries = cfg.loot.get();
        int slot = 0;

        for (String entry : entries) {
            if (slot >= chest.getContainerSize()) break;
            String[] parts = entry.trim().split("\\s+");
            if (parts.length != 4) continue;

            try {
                ResourceLocation id  = ResourceLocation.tryParse(parts[0]);
                double chance        = Double.parseDouble(parts[1]);
                int    min           = Integer.parseInt(parts[2]);
                int    max           = Integer.parseInt(parts[3]);

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
