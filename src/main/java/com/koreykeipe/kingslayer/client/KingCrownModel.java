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
 * A simple golden crown — a banded circlet with four spikes — rendered as a layer atop The
 * King's head. Geometry is in model units (1/16 block); tune sizing here if it needs to sit
 * differently on the (large, scaled) Warden head.
 */
public class KingCrownModel extends Model {

    private final ModelPart root;

    public KingCrownModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();
        PartDefinition crown = parts.addOrReplaceChild("crown",
                CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -2.0F, -8.0F, 16.0F, 4.0F, 16.0F),
                PartPose.ZERO);
        // Four spikes around the band.
        crown.addOrReplaceChild("spike_n", CubeListBuilder.create().texOffs(0, 24).addBox(-1.5F, -7.0F, -9.0F, 3.0F, 6.0F, 3.0F), PartPose.ZERO);
        crown.addOrReplaceChild("spike_s", CubeListBuilder.create().texOffs(0, 24).addBox(-1.5F, -7.0F,  6.0F, 3.0F, 6.0F, 3.0F), PartPose.ZERO);
        crown.addOrReplaceChild("spike_w", CubeListBuilder.create().texOffs(0, 24).addBox(-9.0F, -7.0F, -1.5F, 3.0F, 6.0F, 3.0F), PartPose.ZERO);
        crown.addOrReplaceChild("spike_e", CubeListBuilder.create().texOffs(0, 24).addBox( 6.0F, -7.0F, -1.5F, 3.0F, 6.0F, 3.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer vc, int packedLight, int packedOverlay, int color) {
        root.render(pose, vc, packedLight, packedOverlay, color);
    }
}
