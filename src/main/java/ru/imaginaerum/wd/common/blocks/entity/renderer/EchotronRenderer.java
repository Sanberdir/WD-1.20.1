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
        poseStack.translate(0, 1.6, -0.01);

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

            // количество "логических" лучей (можно регулировать для производительности)
            float raysF = Math.max(1f, stage * 3f / 2f); // чем выше stage — больше лучей
            int maxRays = (int)Math.ceil(raysF);
            float partialRay = raysF - (maxRays - 1);

            // базовые параметры сферы
            float sphereBase = 0.45f; // средняя длина луча
            float sphereJitter = 0.25f; // вариативность длины луча
            float pulsationSpeed = 15.0f;

            VertexConsumer vc = bufferSource.getBuffer(RenderType.lightning());

            poseStack.pushPose();
            poseStack.translate(0, 1.6f, 0);
            Matrix4f matrix = poseStack.last().pose();

            // Основной RNG: детерминирован относительно позиции блока, но
            // для каждого луча мы добавляем индекс, чтобы получился стабильный набор направлений
            long baseSeed = animatable.getBlockPos().asLong();

            for (int i = 0; i < maxRays; i++) {
                // детерминированный RNG для каждого луча
                java.util.Random rnd = new java.util.Random(baseSeed ^ (i * 0x9E3779B97F4A7C15L));

                // Равномерная выборка направлений на сфере:
                // theta ∈ [0, 2π), z ∈ [-1,1] с равномерным распределением по площади
                double u = rnd.nextDouble();
                double v = rnd.nextDouble();
                double theta = 2.0 * Math.PI * v + (time * 0.02); // добавляем медленную общую вращательную модуляцию
                double z = 2.0 * u - 1.0; // cos(phi)
                double r = Math.sqrt(Math.max(0.0, 1.0 - z * z));

                float dirX = (float) (r * Math.cos(theta));
                float dirZ = (float) (r * Math.sin(theta));
                float dirY = (float) z;

                // длина луча — базовая + случайная + пульсация по времени
                float length = sphereBase + sphereJitter * rnd.nextFloat() + 0.08f * (float)Math.sin(time / pulsationSpeed + i);
                // маленькая вариация, чтобы некоторые лучи были короче (плавный эффект)
                float lengthInner = length * (0.45f + 0.2f * rnd.nextFloat());

                float alphaMultiplier = (i == maxRays - 1) ? partialRay : 1f;

                // концы луча: от центра к точке на сфере
                float x = dirX * length;
                float y = dirY * length;
                float z2 = dirZ * length;

                float xInner = dirX * lengthInner;
                float yInner = dirY * lengthInner;
                float zInner = dirZ * lengthInner;

                // основной "тройной" треугольник (плотный центр -> средняя -> внешний)
                int a1 = Math.min(255, (int)(180 * alphaMultiplier));
                int a2 = Math.min(255, (int)(90 * alphaMultiplier));
                int a3 = Math.min(255, (int)(35 * alphaMultiplier));

                vc.vertex(matrix, 0f, 0f, 0f).color(0, 255, 255, a1).endVertex();
                vc.vertex(matrix, xInner, yInner, zInner).color(0, 200, 255, a2).endVertex();
                vc.vertex(matrix, x, y, z2).color(0, 200, 255, a3).endVertex();

                // иногда добавляем тонкую вторичную ветвь для глубины
                if (i % 3 == 0) {
                    // небольшое отклонение направления для ветви
                    float branchLen = length * (0.6f + 0.2f * rnd.nextFloat());
                    // образуем слегка сдвинутую точку (меняем угол маленькой петлей)
                    double branchTheta = theta + (rnd.nextDouble() - 0.5) * 0.6;
                    double branchZ = Math.max(-1.0, Math.min(1.0, z + (rnd.nextDouble() - 0.5) * 0.2));
                    double branchR = Math.sqrt(Math.max(0.0, 1.0 - branchZ * branchZ));
                    float bdx = (float)(branchR * Math.cos(branchTheta));
                    float bdz = (float)(branchR * Math.sin(branchTheta));
                    float bdy = (float)branchZ;

                    float bx = bdx * branchLen;
                    float by = bdy * branchLen;
                    float bz = bdz * branchLen;

                    int ab1 = Math.min(255, (int)(120 * alphaMultiplier));
                    int ab2 = Math.min(255, (int)(50 * alphaMultiplier));
                    int ab3 = Math.min(255, (int)(15 * alphaMultiplier));

                    vc.vertex(matrix, 0f, 0f, 0f).color(0, 255, 255, ab1).endVertex();
                    vc.vertex(matrix, bx * 0.55f, by * 0.55f, bz * 0.55f).color(0, 200, 255, ab2).endVertex();
                    vc.vertex(matrix, bx, by, bz).color(0, 200, 255, ab3).endVertex();
                }
            }

            poseStack.popPose();
        }
    }
}