package ru.imaginaerum.wd.common.blocks.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;
import org.joml.Matrix4f;
import ru.imaginaerum.wd.common.blocks.custom.EchotronBlock;
import ru.imaginaerum.wd.common.blocks.entity.EchotronBlockEntity;
import ru.imaginaerum.wd.common.blocks.entity.model.EchotronModel;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
public class EchotronRenderer extends GeoBlockRenderer<EchotronBlockEntity> {
    public EchotronRenderer(BlockEntityRendererProvider.Context context) {
        super(new EchotronModel());

    }
    @Override
    public void actuallyRender(PoseStack poseStack,
                               EchotronBlockEntity animatable,
                               BakedGeoModel model,
                               RenderType renderType,
                               MultiBufferSource bufferSource,
                               VertexConsumer buffer,
                               boolean isReRender,
                               float partialTick,
                               int packedLight,
                               int packedOverlay,
                               float red, float green, float blue, float alpha) {

        // Сначала рисуем модель блока
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay,
                red, green, blue, alpha);

        // Потом добавляем осколок
        poseStack.pushPose();

        // Возвращаем кристалл на правильную позицию относительно смещенной оси
        poseStack.translate(0, 1.7, -0.1);

        // Масштабируем
        poseStack.scale(0.75f, 0.75f, 0.75f);

        if (animatable.getLevel() != null) {
            float time = animatable.getLevel().getGameTime() + partialTick;
            int stage = animatable.getBlockState().getValue(EchotronBlock.STAGE);
            float spinZ = (time * stage * 3) % 360;

            // Сначала применяем вращение по Z
            poseStack.mulPose(Axis.ZN.rotationDegrees(spinZ));

            // Затем другие повороты
            poseStack.mulPose(Axis.YP.rotationDegrees(90));
            poseStack.mulPose(Axis.ZP.rotationDegrees(45));
            poseStack.translate(0, -0.1, 0);
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    Items.ECHO_SHARD.getDefaultInstance(),
                    ItemDisplayContext.GROUND,
                    packedLight,
                    packedOverlay,
                    poseStack,
                    bufferSource,
                    animatable.getLevel(),
                    0
            );
        }
        poseStack.popPose();

        if (animatable.getLevel() != null) {
            float time = animatable.getLevel().getGameTime() + partialTick;
            int stage = animatable.getBlockState().getValue(EchotronBlock.STAGE);

            float raysF = stage / 2f;
            int maxRays = (int)Math.ceil(raysF);
            float partialRay = raysF - (maxRays - 1);

            float baseRadius = 0.3f;
            float baseHeight = 0.5f;

            VertexConsumer vc = bufferSource.getBuffer(RenderType.lightning());

            poseStack.pushPose();
            poseStack.translate(0, 1.7f, 0);
            Matrix4f matrix = poseStack.last().pose();
            java.util.Random rnd = new java.util.Random(animatable.getBlockPos().asLong());

            for (int i = 0; i < maxRays; i++) {
                double angle = i * (360.0 / maxRays) + time * 2;
                float radius = baseRadius + 0.05f * (float)Math.sin(time / 20.0 + i);
                float heightVariation = 0.1f * (float)Math.cos(time / 25.0 + i * 2);

                // Случайное фиксированное направление для этого блока и луча
                int dir = rnd.nextInt(3) - 1; // -1 = вниз, 0 = вбок, 1 = вверх
                float heightOffset = switch (dir) {
                    case 1 -> 0.15f;
                    case -1 -> -0.15f;
                    default -> 0f;
                };

                float currentHeight = baseHeight + heightVariation + heightOffset;

                // дальше как у тебя было
                float x = (float)(Math.cos(Math.toRadians(angle)) * radius);
                float z = (float)(Math.sin(Math.toRadians(angle)) * radius);
                float y = currentHeight;

                double nextAngle = angle + 15.0;
                float x2 = (float)(Math.cos(Math.toRadians(nextAngle)) * (radius * 0.8f));
                float z2 = (float)(Math.sin(Math.toRadians(nextAngle)) * (radius * 0.8f));
                float y2 = currentHeight - 0.05f;

                float alphaMultiplier = (i == maxRays - 1) ? partialRay : 1f;

                vc.vertex(matrix, 0, 0, 0).color(0, 255, 255, (int)(150 * alphaMultiplier)).endVertex();
                vc.vertex(matrix, x, y, z).color(0, 200, 255, (int)(30 * alphaMultiplier)).endVertex();
                vc.vertex(matrix, x2, y2, z2).color(0, 200, 255, (int)(10 * alphaMultiplier)).endVertex();

                if (i % 2 == 0) {
                    float lowerY = currentHeight - 0.15f;
                    vc.vertex(matrix, 0, 0, 0).color(0, 255, 255, (int)(120 * alphaMultiplier)).endVertex();
                    vc.vertex(matrix, x * 0.7f, lowerY, z * 0.7f).color(0, 200, 255, (int)(15 * alphaMultiplier)).endVertex();
                    vc.vertex(matrix, x2 * 0.7f, lowerY - 0.03f, z2 * 0.7f).color(0, 200, 255, (int)(5 * alphaMultiplier)).endVertex();
                }
            }

            poseStack.popPose();
        }

    }
}