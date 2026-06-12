package com.koreykeipe.kingslayer.block;

import com.koreykeipe.kingslayer.event.MovementCombatHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The Launch Pad — a placeable mobility block. Anything that steps on it is flung skyward
 * with a fall-damage grace window, so you can vault walls, reach airdrops, or bail out of a
 * fight. Counter: a Rooted target (netted by a Bola) can't be launched — the root pins them
 * down and the upward velocity is cancelled.
 */
public class LaunchPadBlock extends Block {

    private static final double LAUNCH = 1.35;

    public LaunchPadBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level.isClientSide) {
            super.stepOn(level, pos, state, entity);
            return;
        }

        // Rooted entities are pinned — the Bola's counter to all mobility.
        if (entity instanceof LivingEntity le && MovementCombatHandler.isRooted(le)) {
            super.stepOn(level, pos, state, entity);
            return;
        }

        Vec3 m = entity.getDeltaMovement();
        entity.setDeltaMovement(m.x * 1.1, LAUNCH, m.z * 1.1);
        entity.hurtMarked = true;
        entity.fallDistance = 0;
        if (entity instanceof LivingEntity le) {
            MovementCombatHandler.grantFallImmunity(le, 200);
        }

        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    18, 0.3, 0.1, 0.3, 0.08);
            sl.playSound(null, pos, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.BLOCKS, 1.0f, 1.4f);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Place it down, then step on to launch skyward.")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("No fall damage from the bounce. Rooted players can't launch.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
