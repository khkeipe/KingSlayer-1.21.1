package com.koreykeipe.kingslayer.client;

import com.koreykeipe.kingslayer.KingSlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WardenRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.warden.Warden;

/**
 * Renders The King by reusing the Warden's renderer (model, animations, and glow
 * layers) and swapping in a royal texture. The King is enlarged via the SCALE
 * attribute, so no render-side scaling is needed.
 */
public class KingRenderer extends WardenRenderer {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KingSlayer.MOD_ID, "textures/entity/king/king.png");

    public KingRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Warden entity) {
        return TEXTURE;
    }
}
