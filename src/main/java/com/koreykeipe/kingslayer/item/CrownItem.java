package com.koreykeipe.kingslayer.item;

import com.koreykeipe.kingslayer.event.CrownHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Crown — a dual-purpose relic:
 * <ul>
 *   <li><b>Held</b> (anywhere in your inventory): acts like a Totem — a Crown is
 *       auto-consumed to cancel a lethal hit (see {@link CrownHandler}).</li>
 *   <li><b>Consumed</b> (right-click): spends one Crown for +1 permanent heart, up to a cap.</li>
 * </ul>
 * Also the repair material for the Slayer tool tier and the King's victory drop.
 */
public class CrownItem extends Item {

    public CrownItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.success(stack);
        }
        if (CrownHandler.grantHeart(sp)) {
            if (!sp.getAbilities().instabuild) stack.shrink(1);
            sp.serverLevel().playSound(null, sp.blockPosition(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.3f);
            return InteractionResultHolder.consume(stack);
        }
        sp.displayClientMessage(Component.literal(
                "§6♛ §7You already bear the maximum crown hearts."), true);
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Store: shatters to spare you from death.")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("Use: consume for +1 permanent heart.")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
