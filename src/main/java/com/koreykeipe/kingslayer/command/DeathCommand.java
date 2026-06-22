package com.koreykeipe.kingslayer.command;

import com.koreykeipe.kingslayer.event.ModEvents;
import com.koreykeipe.kingslayer.game.GameManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Operator command: <pre>/setlives &lt;player&gt; &lt;lives&gt;</pre>
 *
 * Sets a player's remaining lives directly. This writes the REAL life counter in
 * {@link GameManager} (persisted death count) — the one that drives name colour, death
 * progress and airdrop-tier pacing — then refreshes the player's team colour. The old
 * {@code /setdeaths} command set the vanilla death statistic, which the game never reads.
 */
public class DeathCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(
                Commands.literal("setlives")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("player", StringArgumentType.word())
                            .then(Commands.argument("lives", IntegerArgumentType.integer(0))
                                .executes(ctx -> {
                                    String playerName = StringArgumentType.getString(ctx, "player");
                                    int lives = IntegerArgumentType.getInteger(ctx, "lives");
                                    return setLives(ctx.getSource(), playerName, lives);
                                })
                    )
                )
        );
    }

    private static int setLives(CommandSourceStack source, String playername, int lives){
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(playername);
        if (player == null){
            source.sendFailure(Component.literal("No player named " + playername + " was found."));
            return 0;
        }

        int max = GameManager.get().maxLives();
        int clamped = Math.max(0, Math.min(max, lives));

        GameManager.get().setLives(player, clamped);   // updates the real life/death counter
        ModEvents.updateDeaths(player);                // refresh name colour + lives message

        source.sendSystemMessage(Component.literal(
                player.getScoreboardName() + "'s lives have been set to " + clamped + "/" + max + "."));
        return 1;
    }
}
