package com.koreykeipe.kingslayer.entity;

import com.koreykeipe.kingslayer.KingSlayer;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Supplies attribute maps for the mod's living entities. Runs on the MOD event bus.
 */
@Mod.EventBusSubscriber(modid = KingSlayer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntityAttributes {

    @SubscribeEvent
    public static void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(ModEntityTypes.KING.get(), TheKing.createAttributes().build());
    }
}
