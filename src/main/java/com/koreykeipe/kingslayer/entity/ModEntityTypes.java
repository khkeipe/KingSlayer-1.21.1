package com.koreykeipe.kingslayer.entity;

import com.koreykeipe.kingslayer.KingSlayer;
import com.koreykeipe.kingslayer.airdrop.AirdropEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import com.koreykeipe.kingslayer.entity.TheKing;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, KingSlayer.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<AirdropEntity>> AIRDROP =
            ENTITY_TYPES.register("airdrop",
                    () -> EntityType.Builder
                            .<AirdropEntity>of(AirdropEntity::new, MobCategory.MISC)
                            .sized(1.0f, 1.0f)          // bounding box — 1×1×1 cube
                            .clientTrackingRange(64)     // visible up to 64 chunks
                            .updateInterval(1)           // sync every tick for smooth fall
                            .build(KingSlayer.MOD_ID + ":airdrop"));

    public static final DeferredHolder<EntityType<?>, EntityType<TheKing>> KING =
            ENTITY_TYPES.register("king",
                    () -> EntityType.Builder
                            .of(TheKing::new, MobCategory.MONSTER)
                            .sized(0.9f, 2.9f)           // Warden-sized base; SCALE attribute enlarges it
                            .clientTrackingRange(20)
                            .fireImmune()
                            .build(KingSlayer.MOD_ID + ":king"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
