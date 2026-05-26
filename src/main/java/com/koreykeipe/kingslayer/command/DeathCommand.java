package com.koreykeipe.kingslayer.command;

import com.koreykeipe.kingslayer.event.ModEvents;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.DyeColor;

public class DeathCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){

        dispatcher.register(
                Commands.literal("setdeaths")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("player", StringArgumentType.word())
                            .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                .executes(ctx ->{
                                    String playerName = StringArgumentType.getString(ctx, "player");
                                    int value = IntegerArgumentType.getInteger(ctx,"value");
                                    return setDeaths(ctx.getSource(),playerName, value);
                                })
                    )
                )
        );
    }

    private static int setDeaths(CommandSourceStack source, String playername, int value){
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(playername);
        if(player == null){
            source.sendFailure(Component.literal("No Player " + playername + "was found: "));
            return 0;
        }

        Stat<ResourceLocation> stat = Stats.CUSTOM.get(Stats.DEATHS);
        player.getStats().setValue(player, stat, value);
        ModEvents.updateDeaths(player);
        Component msg =
                        Component.literal(player.getScoreboardName())
                        .append(Component.literal("'s Deaths have been set to "))
                        .append(Component.literal(String.valueOf(value)));
        source.sendSystemMessage(msg);

        return 1;
    }
}
