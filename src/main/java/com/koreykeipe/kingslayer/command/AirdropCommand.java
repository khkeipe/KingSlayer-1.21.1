package com.koreykeipe.kingslayer.command;

import com.koreykeipe.kingslayer.airdrop.AirdropManager;
import com.koreykeipe.kingslayer.airdrop.AirdropTier;
import com.koreykeipe.kingslayer.event.KnightSpawnHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Operator commands for testing the airdrop and knight systems.
 *
 * <pre>
 *   /airdrop trigger &lt;tier|all&gt;  — fire a specific airdrop tier immediately
 *   /airdrop knight  &lt;tier|all&gt;  — spawn a knight near you for testing
 * </pre>
 *
 * Requires permission level 2 (op).
 */
public class AirdropCommand {

    private static final SuggestionProvider<CommandSourceStack> TIER_SUGGESTIONS =
            (ctx, builder) -> {
                for (AirdropTier tier : AirdropTier.values()) builder.suggest(tier.name().toLowerCase());
                builder.suggest("all");
                return builder.buildFuture();
            };

    private static final SuggestionProvider<CommandSourceStack> KNIGHT_SUGGESTIONS =
            (ctx, builder) -> {
                builder.suggest("footsoldier");
                builder.suggest("champion");
                builder.suggest("guard");
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
                                source.sendSuccess(() -> Component.literal("§6[Airdrop] §eFired all 4 tiers."), true);
                                return AirdropTier.values().length;
                            }

                            AirdropTier tier;
                            try {
                                tier = AirdropTier.valueOf(arg);
                            } catch (IllegalArgumentException e) {
                                source.sendFailure(Component.literal(
                                    "§cUnknown tier \"" + arg.toLowerCase() + "\". Valid: broken, common, rare, epic, all"));
                                return 0;
                            }

                            AirdropManager.get().triggerManual(server, tier);
                            source.sendSuccess(() -> Component.literal(
                                "§6[Airdrop] §eFired a " + tier.coloredName() + "§e."), true);
                            return 1;
                        })
                    )
                )

                // /airdrop knight <footsoldier|champion|guard|all>
                .then(Commands.literal("knight")
                    .then(Commands.argument("tier", StringArgumentType.word())
                        .suggests(KNIGHT_SUGGESTIONS)
                        .executes(ctx -> {
                            String arg = StringArgumentType.getString(ctx, "tier").toUpperCase();
                            CommandSourceStack source = ctx.getSource();

                            ServerPlayer player;
                            try {
                                player = source.getPlayerOrException();
                            } catch (Exception e) {
                                source.sendFailure(Component.literal("§cMust be run by a player."));
                                return 0;
                            }

                            ServerLevel level = player.serverLevel();
                            String[] tiers = arg.equals("ALL")
                                    ? new String[]{"FOOTSOLDIER", "CHAMPION", "GUARD"}
                                    : new String[]{arg};

                            // Validate
                            for (String t : tiers) {
                                if (!t.equals("FOOTSOLDIER") && !t.equals("CHAMPION") && !t.equals("GUARD")) {
                                    source.sendFailure(Component.literal(
                                        "§cUnknown knight tier \"" + t.toLowerCase()
                                        + "\". Valid: footsoldier, champion, guard, all"));
                                    return 0;
                                }
                            }

                            int spawned = 0;
                            for (String t : tiers) {
                                BlockPos pos = findSpawnNear(level, player.blockPosition());
                                Mob knight = buildKnight(t, level);
                                if (knight == null || pos == null) continue;

                                KnightSpawnHandler.tagKnight(knight, t);
                                knight.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                                        level.random.nextFloat() * 360f, 0f);
                                knight.finalizeSpawn(level, level.getCurrentDifficultyAt(pos),
                                        MobSpawnType.MOB_SUMMONED, null);
                                equipKnight(knight, t);
                                level.addFreshEntity(knight);
                                spawned++;
                            }

                            final int count = spawned;
                            source.sendSuccess(() -> Component.literal(
                                "§6[Knight] §eSpawned " + count + " knight(s) near you."), true);
                            return count;
                        })
                    )
                )
        );
    }

    // ------------------------------------------------------------------
    // Knight-building helpers (mirrors KnightSpawnHandler internals,
    // kept here to avoid exposing private methods on that class)
    // ------------------------------------------------------------------

    private static Mob buildKnight(String tier, ServerLevel level) {
        return switch (tier) {
            case "FOOTSOLDIER" -> {
                Zombie z = new Zombie(EntityType.ZOMBIE, level);
                z.setCustomName(net.minecraft.network.chat.Component.literal("King's Footsoldier")
                    .withStyle(s -> s.withColor(net.minecraft.ChatFormatting.GRAY).withItalic(false)));
                z.setCustomNameVisible(true);
                z.setPersistenceRequired();
                yield z;
            }
            case "CHAMPION" -> {
                Vindicator v = new Vindicator(EntityType.VINDICATOR, level);
                v.setCustomName(net.minecraft.network.chat.Component.literal("King's Champion")
                    .withStyle(s -> s.withColor(net.minecraft.ChatFormatting.GOLD).withItalic(false)));
                v.setCustomNameVisible(true);
                v.setPersistenceRequired();
                AttributeInstance hp = v.getAttribute(Attributes.MAX_HEALTH);
                if (hp != null) { hp.setBaseValue(40.0); v.setHealth(40f); }
                AttributeInstance dmg = v.getAttribute(Attributes.ATTACK_DAMAGE);
                if (dmg != null) dmg.setBaseValue(1.0);
                yield v;
            }
            case "GUARD" -> {
                WitherSkeleton w = new WitherSkeleton(EntityType.WITHER_SKELETON, level);
                w.setCustomName(net.minecraft.network.chat.Component.literal("King's Guard")
                    .withStyle(s -> s.withColor(net.minecraft.ChatFormatting.DARK_PURPLE).withItalic(false)));
                w.setCustomNameVisible(true);
                w.setPersistenceRequired();
                AttributeInstance hp = w.getAttribute(Attributes.MAX_HEALTH);
                if (hp != null) { hp.setBaseValue(60.0); w.setHealth(60f); }
                AttributeInstance dmg = w.getAttribute(Attributes.ATTACK_DAMAGE);
                if (dmg != null) dmg.setBaseValue(2.0);
                yield w;
            }
            default -> null;
        };
    }

    private static void equipKnight(Mob mob, String tier) {
        switch (tier) {
            case "FOOTSOLDIER" -> {
                mob.setItemSlot(EquipmentSlot.HEAD,     new ItemStack(Items.CHAINMAIL_HELMET));
                mob.setItemSlot(EquipmentSlot.CHEST,    new ItemStack(Items.IRON_CHESTPLATE));
                mob.setItemSlot(EquipmentSlot.LEGS,     new ItemStack(Items.CHAINMAIL_LEGGINGS));
                mob.setItemSlot(EquipmentSlot.FEET,     new ItemStack(Items.IRON_BOOTS));
                mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
            }
            case "CHAMPION" -> {
                // Vindicators don't render worn armor — leave them with their natural axe only
            }
            case "GUARD" -> {
                mob.setItemSlot(EquipmentSlot.HEAD,  new ItemStack(Items.DIAMOND_HELMET));
                mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
            }
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) mob.setDropChance(slot, 0f);
    }

    private static BlockPos findSpawnNear(ServerLevel level, BlockPos anchor) {
        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = level.random.nextDouble() * 2 * Math.PI;
            int dist = 6 + level.random.nextInt(10); // close for testing
            int x = anchor.getX() + (int)(Math.cos(angle) * dist);
            int z = anchor.getZ() + (int)(Math.sin(angle) * dist);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos candidate = new BlockPos(x, y, z);
            if (level.getBlockState(candidate.below()).isSolid()
                    && level.isEmptyBlock(candidate)
                    && level.isEmptyBlock(candidate.above())) {
                return candidate;
            }
        }
        return anchor.above(); // fallback: right on top of caller
    }
}
