package com.koreykeipe.kingslayer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.WardenModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import com.koreykeipe.kingslayer.KingSlayer;
import net.minecraft.world.entity.monster.warden.Warden;

/**
 * Renders {@link KingCrownModel} atop The King's head, following the head bone's animation.
 * The head is the Warden model's {@code bone → head} part. {@link #LIFT} nudges the crown up
 * so it rests on the crown of the head; tweak it if the fit needs adjusting in-game.
 */
public class KingCrownLayer extends RenderLayer<Warden, WardenModel<Warden>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KingSlayer.MOD_ID, "textures/entity/king/king_crown.png");

    /** Vertical lift (blocks) above the head pivot so the crown sits on top of the head. */
    private static final float LIFT = -1.0F;

    private final KingCrownModel model;

    public KingCrownLayer(RenderLayerParent<Warden, WardenModel<Warden>> parent, ModelPart crownPart) {
        super(parent);
        this.model = new KingCrownModel(crownPart);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int packedLight, Warden entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        ModelPart head;
        try {
            ModelPart bone = getParentModel().root().getChild("bone");
            head = bone.getChild("head");
            pose.pushPose();
            bone.translateAndRotate(pose);
            head.translateAndRotate(pose);
        } catch (Exception e) {
            return; // model layout changed — fail safe, just skip the crown
        }
        pose.translate(0.0F, LIFT, 0.0F);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        model.renderToBuffer(pose, vc, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        pose.popPose();
    }
}
