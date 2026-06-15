package com.koreykeipe.kingslayer.client;

import com.koreykeipe.kingslayer.KingSlayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.WardenModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.warden.Warden;

/**
 * Hangs {@link KingCapeModel} down The King's back, riding the body bone's animation.
 * The body bone is the Warden model's {@code root → bone → body}. The constants below
 * place the cape at the shoulders; tweak them if the drape needs adjusting in-game.
 */
public class KingCapeLayer extends RenderLayer<Warden, WardenModel<Warden>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KingSlayer.MOD_ID, "textures/entity/king/king_cape.png");

    /** Offsets are in blocks (post-bone transform): up to the shoulders, just behind the spine. */
    private static final float CAPE_Y = -0.8F;
    private static final float CAPE_Z = 0.38F; // pulled in toward the back (~2px tighter)
    /** A slight outward tilt so it drapes rather than clipping straight down the legs. */
    private static final float CAPE_TILT = 5.0F;
    /** Upscaled to drape across the King's broad frame. */
    private static final float SCALE = 1.6F;

    private static boolean loggedMissingBody = false;

    private final KingCapeModel model;

    public KingCapeLayer(RenderLayerParent<Warden, WardenModel<Warden>> parent, ModelPart capePart) {
        super(parent);
        this.model = new KingCapeModel(capePart);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int packedLight, Warden entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        ModelPart bone, body;
        try {
            bone = getParentModel().root().getChild("bone");
            body = bone.getChild("body");
        } catch (Exception e) {
            if (!loggedMissingBody) {
                loggedMissingBody = true;
                KingSlayer.LOGGER.warn("KingCapeLayer: could not find Warden body bone — cape skipped.", e);
            }
            return;
        }
        pose.pushPose();
        bone.translateAndRotate(pose);
        body.translateAndRotate(pose);
        pose.translate(0.0F, CAPE_Y, CAPE_Z);
        pose.mulPose(Axis.XP.rotationDegrees(CAPE_TILT));
        pose.scale(SCALE, SCALE, SCALE);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        model.renderToBuffer(pose, vc, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        pose.popPose();
    }
}
