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
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class AddItemModifier extends LootModifier {

    /**
     * Codec fields (all optional extras default to safe values so old entries still load):
     * <ul>
     *   <li>{@code item}         — required</li>
     *   <li>{@code min_count}    — default 1</li>
     *   <li>{@code max_count}    — default 1</li>
     *   <li>{@code enchantments} — default empty; map of "namespace:id" → integer level</li>
     * </ul>
     */
    public static final MapCodec<AddItemModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            LootModifier.codecStart(inst)
                    .and(BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(e -> e.item))
                    .and(Codec.INT.optionalFieldOf("min_count", 1).forGetter(e -> e.minCount))
                    .and(Codec.INT.optionalFieldOf("max_count", 1).forGetter(e -> e.maxCount))
                    .and(Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT)
                            .optionalFieldOf("enchantments", Map.of())
                            .forGetter(e -> e.enchantments))
                    .apply(inst, AddItemModifier::new));

    private final Item item;
    private final int minCount;
    private final int maxCount;
    /**
     * Enchantments to bake onto the dropped stack.
     * Keys are full resource locations like {@code "minecraft:knockback"}.
     * Resolved at drop time via the loot context's live registry, so no
     * registry access is needed at registration time.
     */
    private final Map<ResourceLocation, Integer> enchantments;

    // ------------------------------------------------------------------
    // Convenience constructors (backward compatible with existing usages)
    // ------------------------------------------------------------------

    /** Drops exactly 1 item with no enchantments. All existing GLM entries use this. */
    public AddItemModifier(LootItemCondition[] conditionsIn, Item item) {
        this(conditionsIn, item, 1, 1, Map.of());
    }

    /** Count range, no enchantments. */
    public AddItemModifier(LootItemCondition[] conditionsIn, Item item, int minCount, int maxCount) {
        this(conditionsIn, item, minCount, maxCount, Map.of());
    }

    /** Full constructor — count range + enchantments. Also the target for the CODEC. */
    public AddItemModifier(LootItemCondition[] conditionsIn, Item item,
                           int minCount, int maxCount, Map<ResourceLocation, Integer> enchantments) {
        super(conditionsIn);
        this.item         = item;
        this.minCount     = Math.max(1, minCount);
        this.maxCount     = Math.max(this.minCount, maxCount);
        this.enchantments = enchantments;
    }

    // ------------------------------------------------------------------
    // Core logic
    // ------------------------------------------------------------------

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
        ItemStack stack = new ItemStack(this.item, count);

        // Apply enchantments if configured.
        // We use context.getLevel().registryAccess() so enchantment holders are resolved
        // against the live world registry — no registry access needed at registration time.
        if (!enchantments.isEmpty()) {
            var lookup  = context.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            enchantments.forEach((loc, level) ->
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
