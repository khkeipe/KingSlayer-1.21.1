package com.koreykeipe.kingslayer.command;

import com.koreykeipe.kingslayer.game.GameManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Operator test command for exercising player-count features solo, without a full server.
 *
 * <pre>
 *   /kssim players &lt;count&gt;   inject N fake roster members (alive, 0 deaths) + expand border
 *   /kssim deaths  &lt;total&gt;   spread N total deaths across the simulated players
 *   /kssim clear             remove all simulated players from the roster
 *   /kssim status            show current simulated count + roster stats
 * </pre>
 *
 * Simulated players are persistent offline participants, so they count toward death progress,
 * airdrop-tier pacing, the King finale math, milestones, and the win condition exactly like
 * real offline-but-alive players. Remember to {@code /kssim clear} before a real game.
 */
public class SimCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("kssim")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("players")
                                .then(Commands.argument("count", IntegerArgumentType.integer(0, 100))
                                        .executes(ctx -> {
                                            int n = IntegerArgumentType.getInteger(ctx, "count");
                                            int now = GameManager.get().setSimulatedPlayerCount(ctx.getSource().getServer(), n);
                                            reply(ctx.getSource(), "Simulated players set to §e" + now + "§7. Roster status:");
                                            status(ctx.getSource());
                                            return 1;
                                        })))
                        .then(Commands.literal("deaths")
                                .then(Commands.argument("total", IntegerArgumentType.integer(0))
                                        .executes(ctx -> {
                                            int total = IntegerArgumentType.getInteger(ctx, "total");
                                            int applied = GameManager.get().setSimulatedDeaths(total);
                                            reply(ctx.getSource(), "Applied §e" + applied + " §7deaths across the simulated players.");
                                            status(ctx.getSource());
                                            return 1;
                                        })))
                        .then(Commands.literal("clear")
                                .executes(ctx -> {
                                    GameManager.get().clearSimulatedPlayers(ctx.getSource().getServer());
                                    reply(ctx.getSource(), "Cleared all simulated players. §7(World border is not shrunk — use /worldborder to reset it.)");
                                    return 1;
                                }))
                        .then(Commands.literal("status")
                                .executes(ctx -> { status(ctx.getSource()); return 1; }))
        );
    }

    private static void status(CommandSourceStack source) {
        GameManager gm = GameManager.get();
        reply(source, "§7sim=§e" + gm.simulatedCount()
                + " §7| alive=§a" + gm.aliveCount()
                + " §7| lives left=§b" + gm.remainingLives()
                + " §7| death progress=§d" + String.format("%.0f%%", gm.eventDeathProgress() * 100));
    }

    private static void reply(CommandSourceStack source, String msg) {
        source.sendSystemMessage(Component.literal("§6[kssim] §r" + msg));
    }
}
