package com.koreykeipe.kingslayer.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Adds <strong>exactly one</strong> item, chosen at random (by weight) from a group —
 * the mutually-exclusive counterpart to {@link AddItemModifier}. Use it when a crate
 * should give one of several tools/weapons rather than rolling each independently, so
 * you can keep adding options without inflating how much a crate drops.
 *
 * <p>The overall drop is still gated by this modifier's {@code conditions} (typically a
 * block-state match plus a {@code random_chance}); when they pass, one choice is added.
 * Each choice may carry its own count range and enchantments.</p>
 */
public class OneOfItemModifier extends LootModifier {

    /** One option in the group: item, relative {@code weight}, count range, and enchantments. */
    public record Choice(Item item, int weight, int minCount, int maxCount, Map<ResourceLocation, Integer> enchantments) {
        public static final Codec<Choice> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Choice::item),
                Codec.INT.optionalFieldOf("weight", 1).forGetter(Choice::weight),
                Codec.INT.optionalFieldOf("min_count", 1).forGetter(Choice::minCount),
                Codec.INT.optionalFieldOf("max_count", 1).forGetter(Choice::maxCount),
                Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT)
                        .optionalFieldOf("enchantments", Map.of()).forGetter(Choice::enchantments)
        ).apply(inst, Choice::new));

        /** Single-count, no enchantments. */
        public Choice(Item item, int weight) { this(item, weight, 1, 1, Map.of()); }
        /** Count range, no enchantments. */
        public Choice(Item item, int weight, int minCount, int maxCount) { this(item, weight, minCount, maxCount, Map.of()); }
        /** Single-count, enchanted. */
        public Choice(Item item, int weight, Map<ResourceLocation, Integer> enchantments) { this(item, weight, 1, 1, enchantments); }
    }

    public static final MapCodec<OneOfItemModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            LootModifier.codecStart(inst)
                    .and(Choice.CODEC.listOf().fieldOf("choices").forGetter(e -> e.choices))
                    .apply(inst, OneOfItemModifier::new));

    private final List<Choice> choices;

    public OneOfItemModifier(LootItemCondition[] conditionsIn, List<Choice> choices) {
        super(conditionsIn);
        this.choices = choices;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        for (LootItemCondition condition : this.conditions) {
            if (!condition.test(context)) return generatedLoot;
        }
        if (choices.isEmpty()) return generatedLoot;

        int total = 0;
        for (Choice c : choices) total += Math.max(1, c.weight());
        int r = context.getRandom().nextInt(total);

        Choice chosen = choices.get(choices.size() - 1);
        for (Choice c : choices) {
            r -= Math.max(1, c.weight());
            if (r < 0) { chosen = c; break; }
        }

        int min = Math.max(1, chosen.minCount());
        int max = Math.max(min, chosen.maxCount());
        int count = (min == max) ? min : context.getRandom().nextIntBetweenInclusive(min, max);
        ItemStack stack = new ItemStack(chosen.item(), count);

        if (!chosen.enchantments().isEmpty()) {
            var lookup = context.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            chosen.enchantments().forEach((loc, level) ->
                    lookup.get(ResourceKey.create(Registries.ENCHANTMENT, loc))
                            .ifPresent(holder -> mutable.set(holder, level)));
            stack.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
        }

        generatedLoot.add(stack);
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
