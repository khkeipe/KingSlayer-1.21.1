package com.koreykeipe.kingslayer.client;

import com.koreykeipe.kingslayer.KingSlayer;
import net.minecraft.client.model.WardenModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.WardenEmissiveLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.warden.Warden;

/**
 * Renders The King: the Warden model/animations with a royal base texture and the glow layers
 * recolored from cyan to royal gold (the heart pulses crimson), plus a golden crown layer.
 * The King is enlarged via the SCALE attribute, so no render-side scaling is needed.
 */
public class KingRenderer extends MobRenderer<Warden, WardenModel<Warden>> {

    private static ResourceLocation tex(String file) {
        return ResourceLocation.fromNamespaceAndPath(KingSlayer.MOD_ID, "textures/entity/king/" + file);
    }

    private static final ResourceLocation TEXTURE = tex("king.png");

    public KingRenderer(EntityRendererProvider.Context context) {
        super(context, new WardenModel<>(context.bakeLayer(ModelLayers.WARDEN)), 0.9F);

        // Ambient bioluminescence (always on).
        this.addLayer(new WardenEmissiveLayer<>(this, tex("king_bioluminescent_layer.png"),
                (e, partial, age) -> 1.0F, WardenModel::getBioluminescentLayerModelParts));
        // Two out-of-phase pulsating spot passes.
        this.addLayer(new WardenEmissiveLayer<>(this, tex("king_pulsating_spots_1.png"),
                (e, partial, age) -> Math.max(0.0F, Mth.cos(age * 0.045F) * 0.25F),
                WardenModel::getPulsatingSpotsLayerModelParts));
        this.addLayer(new WardenEmissiveLayer<>(this, tex("king_pulsating_spots_2.png"),
                (e, partial, age) -> Math.max(0.0F, Mth.cos(age * 0.045F + (float) Math.PI) * 0.25F),
                WardenModel::getPulsatingSpotsLayerModelParts));
        // Tendrils (reuse the bioluminescent texture, driven by the tendril animation).
        this.addLayer(new WardenEmissiveLayer<>(this, tex("king_bioluminescent_layer.png"),
                (e, partial, age) -> e.getTendrilAnimation(partial), WardenModel::getTendrilsLayerModelParts));
        // Crimson heart pulse.
        this.addLayer(new WardenEmissiveLayer<>(this, tex("king_heart.png"),
                (e, partial, age) -> e.getHeartAnimation(partial), WardenModel::getHeartLayerModelParts));

        // The crown and cape.
        this.addLayer(new KingCrownLayer(this, context.bakeLayer(ModModelLayers.KING_CROWN)));
        this.addLayer(new KingCapeLayer(this, context.bakeLayer(ModModelLayers.KING_CAPE)));
    }

    @Override
    public ResourceLocation getTextureLocation(Warden entity) {
        return TEXTURE;
    }
}
