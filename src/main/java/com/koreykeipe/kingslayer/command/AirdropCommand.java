package com.koreykeipe.kingslayer.command;

import com.koreykeipe.kingslayer.airdrop.AirdropManager;
import com.koreykeipe.kingslayer.airdrop.AirdropTier;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

/**
 * /airdrop — operator commands for testing the airdrop system.
 *
 * <pre>
 *   /airdrop trigger <tier>   — spawn a specific tier drop right now
 *   /airdrop trigger all      — spawn one drop of every tier
 * </pre>
 *
 * Requires permission level 2 (op).
 */
public class AirdropCommand {

    /** Tab-complete suggestion provider listing all tier names + "all". */
    private static final SuggestionProvider<CommandSourceStack> TIER_SUGGESTIONS =
            (ctx, builder) -> {
                for (AirdropTier tier : AirdropTier.values()) {
                    builder.suggest(tier.name().toLowerCase());
                }
                builder.suggest("all");
                return builder.buildFuture();
            };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("airdrop")
                .requires(source -> source.hasPermission(2))

                // /airdrop trigger <tier|all>
                .then(Commands.literal("trigger")
                    .then(Commands.argument("tier", StringArgumentType.word())
                        .suggests(TIER_SUGGESTIONS)
                        .executes(ctx -> {
                            String arg = StringArgumentType.getString(ctx, "tier").toUpperCase();
                            MinecraftServer server = ctx.getSource().getServer();
                            CommandSourceStack source = ctx.getSource();

                            if (arg.equals("ALL")) {
                                for (AirdropTier tier : AirdropTier.values()) {
                                    AirdropManager.get().triggerManual(server, tier);
                                }
                                source.sendSuccess(
                                    () -> Component.literal("§6[Airdrop] §eFired all 4 tiers."), true);
                                return AirdropTier.values().length;
                            }

                            AirdropTier tier;
                            try {
                                tier = AirdropTier.valueOf(arg);
                            } catch (IllegalArgumentException e) {
                                source.sendFailure(Component.literal(
                                    "§cUnknown tier \"" + arg.toLowerCase()
                                    + "\". Valid: common, rare, epic, legendary, all"));
                                return 0;
                            }

                            AirdropManager.get().triggerManual(server, tier);
                            source.sendSuccess(
                                () -> Component.literal("§6[Airdrop] §eFired a " + tier.coloredName() + "§e."), true);
                            return 1;
                        })
                    )
                )
        );
    }
}
