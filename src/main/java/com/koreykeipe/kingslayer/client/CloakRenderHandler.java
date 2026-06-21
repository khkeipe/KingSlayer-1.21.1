package com.koreykeipe.kingslayer.client;

import com.koreykeipe.kingslayer.KingSlayer;
import com.koreykeipe.kingslayer.item.ModItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

/**
 * Makes the Shadow Cloak hide the wearer completely while it has turned them invisible.
 *
 * <p>Vanilla invisibility still renders worn armor and held items, so a sneaking, cloaked
 * player would otherwise float their chestplate (and other gear) around in plain sight. We
 * cancel the entire entity render for a player who is <em>both</em> invisible and wearing the
 * cloak — the body is already hidden by invisibility, so this only removes the give-away gear.
 *
 * <p>Scoped to cloak wearers: a player invisible from a potion keeps the normal vanilla look.</p>
 */
@EventBusSubscriber(modid = KingSlayer.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class CloakRenderHandler {

    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        if (event.getEntity() instanceof Player player
                && player.isInvisible()
                && player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.SHADOW_CLOAK.get())) {
            event.setCanceled(true); // fully hide body + armor + held items while cloaked
        }
    }
}
