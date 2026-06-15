package com.koreykeipe.kingslayer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;

/**
 * A simple flowing cape — a wide, thin cloth panel that hangs down The King's back.
 * Rendered by {@link KingCapeLayer}; positioning constants live there.
 */
public class KingCapeModel extends Model {

    private final ModelPart root;

    public KingCapeModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();
        // 12 wide, 26 tall, 1 thick — hangs straight down from y=0 (the top, at the shoulders).
        parts.addOrReplaceChild("cape",
                CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, 0.0F, 0.0F, 12.0F, 26.0F, 1.0F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer vc, int packedLight, int packedOverlay, int color) {
        root.render(pose, vc, packedLight, packedOverlay, color);
    }
}
