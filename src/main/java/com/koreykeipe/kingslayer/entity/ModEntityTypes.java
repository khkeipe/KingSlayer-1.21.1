package com.koreykeipe.kingslayer.entity;

import com.koreykeipe.kingslayer.KingSlayer;
import com.koreykeipe.kingslayer.airdrop.AirdropEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, KingSlayer.MOD_ID);

    public static final RegistryObject<EntityType<AirdropEntity>> AIRDROP =
            ENTITY_TYPES.register("airdrop",
                    () -> EntityType.Builder
                            .<AirdropEntity>of(AirdropEntity::new, MobCategory.MISC)
                            .sized(1.0f, 1.0f)          // bounding box — 1×1×1 cube
                            .clientTrackingRange(64)     // visible up to 64 chunks
                            .updateInterval(1)           // sync every tick for smooth fall
                            .build(KingSlayer.MOD_ID + ":airdrop"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
