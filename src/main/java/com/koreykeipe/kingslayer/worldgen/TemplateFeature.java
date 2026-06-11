package com.koreykeipe.kingslayer.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.Optional;

/**
 * Stamps a saved NBT structure template into the world at the placement origin,
 * randomly rotated and roughly centered. A simple "Route A" placer that rides the
 * same configured/placed/biome-modifier pipeline as the decorative piles — no
 * jigsaw, no structure-set, no {@code /locate}.
 *
 * <p>If the named template doesn't exist yet (e.g. you haven't built/saved the NBT),
 * this is a harmless no-op, so the slot can be wired up ahead of the build.</p>
 *
 * <p>Templates load from {@code data/<namespace>/structure/<path>.nbt} — note the
 * <strong>singular</strong> {@code structure} folder in 1.21.</p>
 */
public class TemplateFeature extends Feature<TemplateConfiguration> {

    public TemplateFeature(Codec<TemplateConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<TemplateConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        TemplateConfiguration cfg = ctx.config();

        StructureTemplateManager manager = level.getLevel().getStructureManager();
        Optional<StructureTemplate> opt = manager.get(cfg.template());
        if (opt.isEmpty()) return false; // NBT not present yet — nothing to place
        StructureTemplate template = opt.get();

        Rotation rotation = Rotation.getRandom(ctx.random());
        Vec3i size = template.getSize();
        // Centre the (unrotated) footprint on the origin; pivot rotation about that centre.
        BlockPos pivot = new BlockPos(size.getX() / 2, 0, size.getZ() / 2);
        BlockPos origin = ctx.origin().offset(-pivot.getX(), 0, -pivot.getZ());

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(rotation)
                .setMirror(Mirror.NONE)
                .setRotationPivot(pivot)
                .setIgnoreEntities(false);
        if (cfg.integrity() < 1.0f) {
            settings.addProcessor(new BlockRotProcessor(cfg.integrity())); // weathered / damaged look
        }

        return template.placeInWorld(level, origin, origin, settings, ctx.random(), Block.UPDATE_CLIENTS);
    }
}
