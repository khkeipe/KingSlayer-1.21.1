package com.koreykeipe.kingslayer.client;

import com.koreykeipe.kingslayer.airdrop.AirdropEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Renders a falling {@link AirdropEntity} as the crate block that matches its tier.
 */
public class AirdropEntityRenderer extends EntityRenderer<AirdropEntity> {

    private final BlockRenderDispatcher blockRenderer;

    public AirdropEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
        // No shadow — it would look odd on a fast-moving object
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(AirdropEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {

        poseStack.pushPose();

        // Block models render from (0,0,0) to (1,1,1) relative to the entity origin,
        // so we shift -0.5 on X and Z to centre the cube on the entity position.
        poseStack.translate(-0.5, 0.0, -0.5);

        BlockState state = entity.getTier().getCrate().get().defaultBlockState();
        this.blockRenderer.renderSingleBlock(state, poseStack, bufferSource,
                packedLight, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(AirdropEntity entity) {
        // Block models sample from the block atlas
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
