package com.koreykeipe.kingslayer.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

public class AddItemModifier extends LootModifier {

    /**
     * Codec serialises: item, min_count (optional, default 1), max_count (optional, default 1).
     * Old entries without count fields will deserialise correctly and default to a stack of 1.
     */
    public static final MapCodec<AddItemModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            LootModifier.codecStart(inst)
                    .and(BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(e -> e.item))
                    .and(Codec.INT.optionalFieldOf("min_count", 1).forGetter(e -> e.minCount))
                    .and(Codec.INT.optionalFieldOf("max_count", 1).forGetter(e -> e.maxCount))
                    .apply(inst, AddItemModifier::new));

    private final Item item;
    private final int minCount;
    private final int maxCount;

    /** Convenience constructor — drops exactly one item. All existing registrations use this. */
    public AddItemModifier(LootItemCondition[] conditionsIn, Item item) {
        this(conditionsIn, item, 1, 1);
    }

    /** Full constructor — drops a uniformly random count in [minCount, maxCount]. */
    public AddItemModifier(LootItemCondition[] conditionsIn, Item item, int minCount, int maxCount) {
        super(conditionsIn);
        this.item = item;
        this.minCount = Math.max(1, minCount);
        this.maxCount = Math.max(this.minCount, maxCount);
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        for (LootItemCondition condition : this.conditions) {
            if (!condition.test(context)) {
                return generatedLoot;
            }
        }
        int count = (minCount == maxCount)
                ? minCount
                : context.getRandom().nextIntBetweenInclusive(minCount, maxCount);
        generatedLoot.add(new ItemStack(this.item, count));
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
