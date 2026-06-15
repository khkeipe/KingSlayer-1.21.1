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

        // A heraldic crown: a slim hollow band, tapered points (a wide base stepping to a
        // narrow tip, finished with a small orb finial), and an emerald gem on the brow.
        // Prominent front point, four medium corner points, and smaller tapered spikes at the
        // back and sides. (Cubes only, so the taper is stepped — the closest to the reference art.)
        CubeListBuilder cb = CubeListBuilder.create()
                // band — 16x16 outer footprint, 2 thick, 3 tall (top face at y=-1)
                .texOffs(0, 0).addBox(-8.0F, -1.0F, -8.0F, 16.0F, 3.0F, 2.0F)   // north (front) wall
                .texOffs(0, 0).addBox(-8.0F, -1.0F,  6.0F, 16.0F, 3.0F, 2.0F)   // south wall
                .texOffs(0, 12).addBox(-8.0F, -1.0F, -6.0F, 2.0F, 3.0F, 12.0F)  // west wall
                .texOffs(0, 12).addBox( 6.0F, -1.0F, -6.0F, 2.0F, 3.0F, 12.0F)  // east wall

                // prominent front-centre point: 2x2 base → 1x1 tip → orb
                .texOffs(20, 0).addBox(-1.0F, -3.0F, -8.0F, 2.0F, 2.0F, 2.0F)
                .texOffs(30, 0).addBox(-0.5F, -5.0F, -7.5F, 1.0F, 2.0F, 1.0F)
                .texOffs(20, 12).addBox(-1.0F, -7.0F, -8.0F, 2.0F, 2.0F, 2.0F)

                // four corner points: 2x2 base → 1x1 tip → orb (shorter than the front)
                .texOffs(20, 0).addBox(-8.0F, -3.0F, -8.0F, 2.0F, 2.0F, 2.0F)   // NW
                .texOffs(30, 0).addBox(-7.5F, -4.0F, -7.5F, 1.0F, 1.0F, 1.0F)
                .texOffs(20, 12).addBox(-8.0F, -6.0F, -8.0F, 2.0F, 2.0F, 2.0F)
                .texOffs(20, 0).addBox( 6.0F, -3.0F, -8.0F, 2.0F, 2.0F, 2.0F)   // NE
                .texOffs(30, 0).addBox( 6.5F, -4.0F, -7.5F, 1.0F, 1.0F, 1.0F)
                .texOffs(20, 12).addBox( 6.0F, -6.0F, -8.0F, 2.0F, 2.0F, 2.0F)
                .texOffs(20, 0).addBox(-8.0F, -3.0F,  6.0F, 2.0F, 2.0F, 2.0F)   // SW
                .texOffs(30, 0).addBox(-7.5F, -4.0F,  6.5F, 1.0F, 1.0F, 1.0F)
                .texOffs(20, 12).addBox(-8.0F, -6.0F,  6.0F, 2.0F, 2.0F, 2.0F)
                .texOffs(20, 0).addBox( 6.0F, -3.0F,  6.0F, 2.0F, 2.0F, 2.0F)   // SE
                .texOffs(30, 0).addBox( 6.5F, -4.0F,  6.5F, 1.0F, 1.0F, 1.0F)
                .texOffs(20, 12).addBox( 6.0F, -6.0F,  6.0F, 2.0F, 2.0F, 2.0F)

                // smaller tapered spikes at the back & sides: 2x1 base → 1x2 tip (no orb)
                .texOffs(20, 20).addBox(-1.0F, -2.0F,  6.0F, 2.0F, 1.0F, 2.0F)  // back (S)
                .texOffs(30, 0).addBox(-0.5F, -4.0F,  6.5F, 1.0F, 2.0F, 1.0F)
                .texOffs(20, 20).addBox( 6.0F, -2.0F, -1.0F, 2.0F, 1.0F, 2.0F)  // east
                .texOffs(30, 0).addBox( 6.5F, -4.0F, -0.5F, 1.0F, 2.0F, 1.0F)
                .texOffs(20, 20).addBox(-8.0F, -2.0F, -1.0F, 2.0F, 1.0F, 2.0F)  // west
                .texOffs(30, 0).addBox(-7.5F, -4.0F, -0.5F, 1.0F, 2.0F, 1.0F)

                // emerald gem on the brow (UV maps to the green region of the texture)
                .texOffs(44, 44).addBox(-1.5F, -0.5F, -8.6F, 3.0F, 3.0F, 1.0F);

        parts.addOrReplaceChild("crown", cb, PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer vc, int packedLight, int packedOverlay, int color) {
        root.render(pose, vc, packedLight, packedOverlay, color);
    }
}
