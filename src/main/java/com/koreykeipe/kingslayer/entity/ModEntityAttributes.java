package com.koreykeipe.kingslayer.entity;

import com.koreykeipe.kingslayer.KingSlayer;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Supplies attribute maps for the mod's living entities. Runs on the MOD event bus.
 */
@EventBusSubscriber(modid = KingSlayer.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModEntityAttributes {

    @SubscribeEvent
    public static void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(ModEntityTypes.KING.get(), TheKing.createAttributes().build());
    }
}
