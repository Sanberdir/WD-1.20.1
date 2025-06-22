package ru.imaginaerum.wd.common.armor.elytra;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.ArmorStandArmorModel;
import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ru.imaginaerum.wd.WD;
import ru.imaginaerum.wd.common.items.ItemsWD;

@OnlyIn(Dist.CLIENT)
public class DragoliteElytraArmorStandLayer extends ElytraLayer<ArmorStand, ArmorStandArmorModel> {
    private static final ResourceLocation TEXTURE_ELYTRA = new ResourceLocation(WD.MODID,
            "textures/entity/nezydra.png");
    private static final ResourceLocation TEXTURE_ELYTRA_GLOW = new ResourceLocation(WD.MODID,
            "textures/entity/nezydra_glow.png");

    // Собственная модель элитры (чтобы не лезть в приватное поле родителя)
    private final ElytraModel<ArmorStand> elytraModel;

    public DragoliteElytraArmorStandLayer(ArmorStandRenderer renderer,
                                          EntityModelSet entityModelSet) {
        super(renderer, entityModelSet);
        // Инициализируем свою модель
        this.elytraModel = new ElytraModel<>(entityModelSet.bakeLayer(ModelLayers.ELYTRA));
    }

    @Override
    public boolean shouldRender(ItemStack stack, ArmorStand entity) {
        return stack.getItem() == ItemsWD.MAG_ELYTRA.get();
    }

    @Override
    public ResourceLocation getElytraTexture(ItemStack stack, ArmorStand entity) {
        return TEXTURE_ELYTRA;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       ArmorStand entity, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {

        ItemStack stack = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (stack.getItem() != ItemsWD.MAG_ELYTRA.get()) return;

        // 1) Основной слой элитры (скопируем логику родителя)
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, 0.125F);
        this.getParentModel().copyPropertiesTo(this.elytraModel);
        this.elytraModel.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        VertexConsumer mainBuffer = buffer.getBuffer(
                RenderType.armorCutoutNoCull(TEXTURE_ELYTRA)
        );
        this.elytraModel.renderToBuffer(poseStack, mainBuffer, packedLight,
                OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
        poseStack.popPose();

        // 2) Светящийся слой с fullbright и небольшим Z-смещением
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, 0.130F);
        this.getParentModel().copyPropertiesTo(this.elytraModel);
        this.elytraModel.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        int fullBright = 0xF000F0;
        VertexConsumer glowBuffer = buffer.getBuffer(
                RenderType.entityTranslucentEmissive(TEXTURE_ELYTRA_GLOW)
        );
        this.elytraModel.renderToBuffer(poseStack, glowBuffer, fullBright,
                OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
        poseStack.popPose();
    }
}