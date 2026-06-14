package com.koreykeipe.kingslayer.item;

import com.koreykeipe.kingslayer.airdrop.AirdropManager;
import com.koreykeipe.kingslayer.airdrop.AirdropTier;
import com.koreykeipe.kingslayer.entity.ModEntityTypes;
import com.koreykeipe.kingslayer.entity.TheKing;
import com.koreykeipe.kingslayer.game.GameManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Boss Key — right-click to summon The King. It only works once the final
 * phase has begun (EPIC airdrop tier triggered), so the boss can't be rushed early.
 * Obtained from the gated Tribute Stone trade.
 */
public class BossKeyItem extends Item {

    public BossKeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.success(stack);
        }
        if (!AirdropManager.get().isTierTriggered(AirdropTier.EPIC)) {
            sp.displayClientMessage(Component.literal(
                    "§cThe Boss Key hums faintly… but the final hour has not yet come."), true);
            return InteractionResultHolder.fail(stack);
        }

        ServerLevel sl = sp.serverLevel();
        TheKing king = new TheKing(ModEntityTypes.KING.get(), sl);
        BlockPos pos = sp.blockPosition().relative(sp.getDirection(), 6);
        king.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, sp.getYRot() + 180f, 0f);
        king.finalizeSpawn(sl, sl.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        sl.addFreshEntity(king);
        GameManager.get().markKingSummoned(); // so the auto-finale won't spawn a second King

        GameManager.get().broadcast("§4☠ §c" + sp.getName().getString()
                + " §4turned the Boss Key — §cTHE KING RISES!");
        if (!sp.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Turns only in the final hour of the round.")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("Right-click to summon The King.")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
