package com.koreykeipe.kingslayer.client;

import com.koreykeipe.kingslayer.KingSlayer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

/** Custom client model layers for KingSlayer. */
public class ModModelLayers {

    /** The King's crown, rendered as a layer atop the (Warden) head. */
    public static final ModelLayerLocation KING_CROWN = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(KingSlayer.MOD_ID, "king_crown"), "main");

    /** The King's cape, hanging from his back. */
    public static final ModelLayerLocation KING_CAPE = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(KingSlayer.MOD_ID, "king_cape"), "main");
}
