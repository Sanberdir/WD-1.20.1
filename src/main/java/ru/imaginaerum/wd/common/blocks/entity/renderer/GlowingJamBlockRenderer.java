package ru.imaginaerum.wd.common.blocks.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import ru.imaginaerum.wd.common.blocks.entity.GlowingJamBlockEntity;
import ru.imaginaerum.wd.common.blocks.entity.model.GlowingJamModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class GlowingJamBlockRenderer extends GeoBlockRenderer<GlowingJamBlockEntity> {
    public GlowingJamBlockRenderer(BlockEntityRendererProvider.Context context) {
        super(new GlowingJamModel());
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));

    }

    @Override
    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        super.rotateBlock(facing, poseStack);

        if (this.animatable != null) {
            int rotation = this.animatable.getBlockState()
                    .getValue(BlockStateProperties.ROTATION_16);
            float degrees = rotation * 22.5f;
            poseStack.mulPose(Axis.YP.rotationDegrees(degrees));
        }
    }
}