package com.koreykeipe.kingslayer.loot;

import com.koreykeipe.kingslayer.KingSlayer;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModLootModifiers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, KingSlayer.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, ?> ADD_ITEM =
            LOOT_MODIFIER_SERIALIZERS.register("add_item",() -> AddItemModifier.CODEC);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, ?> ONE_OF_ITEM =
            LOOT_MODIFIER_SERIALIZERS.register("one_of_item",() -> OneOfItemModifier.CODEC);

    public static void register(IEventBus eventBus){
        LOOT_MODIFIER_SERIALIZERS.register(eventBus);
    }
}
