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

    /** Vertical lift (blocks) above the head pivot so the crown sits on top of the head.
     *  Negative is UP in model space. The Warden head is tall, so this clears it. */
    private static final float LIFT = -1.0F; // lowered ~3px so it sits down on the head
    /** Crown size. Z (front-to-back) is kept smaller so the crown isn't bulky/deep. */
    private static final float SCALE_XY = 1.3F;
    private static final float SCALE_Z  = 1.0F;

    private static boolean loggedMissingHead = false;

    private final KingCrownModel model;

    public KingCrownLayer(RenderLayerParent<Warden, WardenModel<Warden>> parent, ModelPart crownPart) {
        super(parent);
        this.model = new KingCrownModel(crownPart);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int packedLight, Warden entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        // The Warden head bone is root → bone → body → head. Apply the full chain so the
        // crown rides the head's animated transform.
        ModelPart bone, body, head;
        try {
            bone = getParentModel().root().getChild("bone");
            body = bone.getChild("body");
            head = body.getChild("head");
        } catch (Exception e) {
            if (!loggedMissingHead) {
                loggedMissingHead = true;
                KingSlayer.LOGGER.warn("KingCrownLayer: could not find Warden head bone — crown skipped.", e);
            }
            return;
        }
        pose.pushPose();
        bone.translateAndRotate(pose);
        body.translateAndRotate(pose);
        head.translateAndRotate(pose);
        pose.translate(0.0F, LIFT, 0.0F);
        pose.scale(SCALE_XY, SCALE_XY, SCALE_Z);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        model.renderToBuffer(pose, vc, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        pose.popPose();
    }
}
