package com.koreykeipe.kingslayer.item;

import com.koreykeipe.kingslayer.game.CombatTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The Storm Brand — a melee sword that also calls a lightning bolt where the wielder
 * aims (right-click, on a cooldown). Reuses vanilla lightning, so the bolt's cause is
 * the wielder (direct kill credit). Counter: a vanilla Lightning Rod near the target
 * redirects the strike to itself.
 */
public class StormBrandItem extends SwordItem {

    private static final double RANGE = 28.0;
    private static final int    COOLDOWN_TICKS = 100;  // 5 seconds
    private static final int    REDIRECT_RADIUS = 8;   // Lightning Rod search radius

    public StormBrandItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.success(stack);
        }
        ServerLevel sl = sp.serverLevel();

        Vec3 eye  = sp.getEyePosition();
        Vec3 look = sp.getViewVector(1.0f);
        Vec3 end  = eye.add(look.scale(RANGE));

        // Aim: nearest of (block hit, entity hit) along the look ray.
        BlockHitResult block = sl.clip(new ClipContext(eye, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, sp));
        Vec3 target = block.getType() != HitResult.Type.MISS ? block.getLocation() : end;
        double bestSqr = eye.distanceToSqr(target);

        EntityHitResult entity = ProjectileUtil.getEntityHitResult(sl, sp, eye, end,
                sp.getBoundingBox().expandTowards(look.scale(RANGE)).inflate(1.0),
                e -> e instanceof LivingEntity && e != sp && !e.isSpectator());
        if (entity != null && eye.distanceToSqr(entity.getLocation()) < bestSqr) {
            target = entity.getEntity().position();
        }

        // Counter: a placed Lightning Rod near the target soaks the strike.
        BlockPos strike = BlockPos.containing(target);
        BlockPos rod = findNearbyRod(sl, strike);
        if (rod != null) strike = rod;

        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
        if (bolt != null) {
            bolt.moveTo(strike.getX() + 0.5, strike.getY(), strike.getZ() + 0.5);
            bolt.setCause(sp); // credits the wielder for the kill
            sl.addFreshEntity(bolt);
        }

        // The vanilla lightning damage source carries no attacker, so register attribution for
        // every player in the strike zone — this is how a lightning kill credits the wielder.
        AABB hitBox = new AABB(strike).inflate(3.0);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, hitBox,
                t -> t != sp && t.isAlive())) {
            if (e instanceof ServerPlayer victim) {
                CombatTracker.registerAttribution(victim.getUUID(), sp.getUUID(),
                        sp.getName().getString(), "lightning", 7);
            }
        }

        sp.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.consume(stack);
    }

    private static BlockPos findNearbyRod(ServerLevel level, BlockPos center) {
        BlockPos best = null;
        double bestSqr = Double.MAX_VALUE;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -REDIRECT_RADIUS; dx <= REDIRECT_RADIUS; dx++) {
            for (int dy = -REDIRECT_RADIUS; dy <= REDIRECT_RADIUS; dy++) {
                for (int dz = -REDIRECT_RADIUS; dz <= REDIRECT_RADIUS; dz++) {
                    m.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (level.getBlockState(m).is(Blocks.LIGHTNING_ROD)) {
                        double d = center.distSqr(m);
                        if (d < bestSqr) { bestSqr = d; best = m.immutable(); }
                    }
                }
            }
        }
        return best;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("⚡ Storm Brand")
                .withStyle(s -> s.withColor(ChatFormatting.AQUA).withBold(true).withItalic(false));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click: call a bolt where you aim.")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("Counter: a Lightning Rod nearby redirects the strike.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
