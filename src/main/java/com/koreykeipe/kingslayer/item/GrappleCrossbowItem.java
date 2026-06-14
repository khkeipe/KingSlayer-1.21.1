package com.koreykeipe.kingslayer.item;

import com.koreykeipe.kingslayer.event.MovementCombatHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The Grapple Crossbow — a mobility special. Right-click fires a hook along your aim; if it
 * catches a solid block, you're yanked toward the anchor point with a fall-damage grace window.
 * Counter: being Rooted (by a Bola/Net) pins you down so the launch is cancelled.
 */
public class GrappleCrossbowItem extends Item {

    private static final double RANGE          = 32.0;
    private static final int    COOLDOWN_TICKS = 60;   // 3 seconds
    private static final int    NOFALL_TICKS   = 200;  // safety window while flying
    private static final double MAX_PULL       = 2.0;  // blocks per tick cap

    public GrappleCrossbowItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.success(stack);
        }

        // Rooted players can't grapple — that's the Bola's counter.
        if (MovementCombatHandler.isRooted(sp)) {
            sp.displayClientMessage(Component.literal("⛓ Rooted — you can't grapple!")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        ServerLevel sl = sp.serverLevel();
        Vec3 eye = sp.getEyePosition();
        Vec3 end = eye.add(sp.getViewVector(1.0f).scale(RANGE));

        BlockHitResult block = sl.clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, sp));
        if (block.getType() == HitResult.Type.MISS) {
            sp.displayClientMessage(Component.literal("✖ No anchor in range.")
                    .withStyle(ChatFormatting.GRAY), true);
            sl.playSound(null, sp.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 0.7f, 1.4f);
            sp.getCooldowns().addCooldown(this, COOLDOWN_TICKS / 2);
            return InteractionResultHolder.fail(stack);
        }

        // Yank toward the anchor: speed scales with distance, with a small upward arc.
        Vec3 pull = block.getLocation().subtract(sp.position());
        double dist = pull.length();
        Vec3 vel = pull.normalize().scale(Math.min(MAX_PULL, 0.25 * dist + 0.5));
        sp.setDeltaMovement(vel.x, vel.y + 0.25, vel.z);
        sp.hurtMarked = true;
        sp.fallDistance = 0;
        MovementCombatHandler.grantFallImmunity(sp, NOFALL_TICKS);

        sl.playSound(null, sp.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0f, 0.9f);
        sl.playSound(null, sp.blockPosition(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 0.8f, 0.7f);
        sp.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        stack.hurtAndBreak(1, sp, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("⌖ Grapple")
                .withStyle(s -> s.withColor(ChatFormatting.GREEN).withBold(true).withItalic(false));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click: hook a block and zip toward it.")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("Lands you safely — no fall damage from the swing.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("Counter: a Bola/Net root pins you so you can't grapple.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
