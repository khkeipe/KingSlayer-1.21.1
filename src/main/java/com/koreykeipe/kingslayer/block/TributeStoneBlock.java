package com.koreykeipe.kingslayer.block;

import com.koreykeipe.kingslayer.exchange.TributeExchange;
import com.koreykeipe.kingslayer.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Tribute Stone — an indestructible hub block (registered with bedrock-grade
 * strength) sitting at world spawn. Right-clicking drives the {@link TributeExchange}:
 * empty hand browses offers, Crown Fragments in hand buys the selected one.
 */
public class TributeStoneBlock extends Block {

    public TributeStoneBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            TributeExchange.cycle(sp); // browse
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.7f, 1.3f);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            if (stack.is(ModItems.CROWN_FRAGMENT.get())) {
                boolean ok = TributeExchange.buy(sp);
                level.playSound(null, pos, ok ? SoundEvents.PLAYER_LEVELUP : SoundEvents.VILLAGER_NO,
                        SoundSource.BLOCKS, 0.8f, ok ? 1.2f : 1.0f);
                if (ok && level instanceof ServerLevel slv) {
                    slv.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            pos.getX() + 0.5, pos.getY() + 1.15, pos.getZ() + 0.5, 14, 0.4, 0.4, 0.4, 0.0);
                }
            } else {
                TributeExchange.cycle(sp); // holding a non-fragment also just browses
            }
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.END_ROD,
                    pos.getX() + 0.2 + random.nextDouble() * 0.6,
                    pos.getY() + 1.0 + random.nextDouble() * 0.3,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6,
                    0.0, 0.02, 0.0);
        }
    }
}
