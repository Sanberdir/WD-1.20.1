package ru.imaginaerum.wd.common.blocks.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import ru.imaginaerum.wd.common.blocks.entity.DragoliteCageBlockEntity;

import java.util.Optional;

public class DragoliteCageEntityRenderer implements BlockEntityRenderer<DragoliteCageBlockEntity> {

    public DragoliteCageEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(DragoliteCageBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        String soulFirst = blockEntity.getSoulsTwo();

        if (soulFirst != null && !soulFirst.isEmpty()) {
            StringTag soulFirstTag = StringTag.valueOf(soulFirst);

            Optional<EntityType<?>> entityTypeOptional = EntityType.byString(soulFirstTag.getAsString());
            if (entityTypeOptional.isPresent()) {
                Entity entity = entityTypeOptional.get().create(Minecraft.getInstance().level);

                if (entity != null) {
                    float scale = 0.53125F;
                    float maxDimension = Math.max(entity.getBbWidth(), entity.getBbHeight());
                    if (maxDimension > 1.0F) {
                        scale /= maxDimension;
                    }

                    float spin = ((blockEntity.getLevel().getGameTime() + partialTick) * 25) % 360; // Ускоренное вращение
                    EntityRenderer<? super Entity> entityRenderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);

                    poseStack.pushPose();
                    poseStack.translate(0.5D, 0.4D, 0.5D); // Позиция в центре блока
                    poseStack.scale(scale, scale, scale); // Масштабирование
                    poseStack.mulPose(Axis.YP.rotationDegrees(spin)); // Вращение вокруг оси Y
                    poseStack.translate(0.0F, -0.2F, 0.0F); // Сдвиг для наклона
                    poseStack.mulPose(Axis.XP.rotationDegrees(-30.0F)); // Наклон на оси X

                    entityRenderer.render(entity, 0, partialTick, poseStack, bufferSource, packedLight);
                    poseStack.popPose();
                }
            }
        }
    }

}