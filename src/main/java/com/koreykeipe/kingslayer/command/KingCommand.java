package com.koreykeipe.kingslayer.command;

import com.koreykeipe.kingslayer.entity.ModEntityTypes;
import com.koreykeipe.kingslayer.entity.TheKing;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;

/**
 * Operator command to summon The King for testing.
 *
 * <pre>/king spawn</pre>
 *
 * Requires permission level 2 (op).
 */
public class KingCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("king")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("spawn")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    ServerLevel level = player.serverLevel();

                                    TheKing king = new TheKing(ModEntityTypes.KING.get(), level);
                                    // Spawn a few blocks ahead of the player.
                                    BlockPos pos = player.blockPosition().relative(player.getDirection(), 6);
                                    king.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                                            player.getYRot() + 180f, 0f);
                                    king.finalizeSpawn(level, level.getCurrentDifficultyAt(pos),
                                            MobSpawnType.COMMAND, null);
                                    level.addFreshEntity(king);

                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "§4☠ §cThe King has been summoned."), true);
                                    return 1;
                                })
                        )
        );
    }
}
