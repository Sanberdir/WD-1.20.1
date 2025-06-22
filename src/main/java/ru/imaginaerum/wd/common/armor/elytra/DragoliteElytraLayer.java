package ru.imaginaerum.wd.common.armor.elytra;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ru.imaginaerum.wd.WD;
import ru.imaginaerum.wd.common.items.ItemsWD;

@OnlyIn(Dist.CLIENT)
public class DragoliteElytraLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation TEXTURE_ELYTRA = new ResourceLocation(WD.MODID,
            "textures/entity/nezydra.png");
    private static final ResourceLocation TEXTURE_ELYTRA_GLOW = new ResourceLocation(WD.MODID,
            "textures/entity/nezydra_glow.png");

    private final ElytraModel<AbstractClientPlayer> elytraModel;

    public DragoliteElytraLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                                EntityModelSet modelSet) {
        super(parent);
        this.elytraModel = new ElytraModel<>(modelSet.bakeLayer(ModelLayers.ELYTRA));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       AbstractClientPlayer entity, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {

        ItemStack chestItem = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (chestItem.getItem() != ItemsWD.MAG_ELYTRA.get()) return;

        // Основная элитра
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, 0.125F);
        this.getParentModel().copyPropertiesTo(this.elytraModel);
        this.elytraModel.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        VertexConsumer main = ItemRenderer.getArmorFoilBuffer(buffer,
                RenderType.armorCutoutNoCull(TEXTURE_ELYTRA),
                false, chestItem.hasFoil());
        this.elytraModel.renderToBuffer(poseStack, main, packedLight, OverlayTexture.NO_OVERLAY,
                1f, 1f, 1f, 1f);
        poseStack.popPose();

        // Glow-слой с небольшим Z-смещением, чтобы избежать z-fighting
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, 0.130F);
        this.getParentModel().copyPropertiesTo(this.elytraModel);
        this.elytraModel.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        int fullBright = 0xF000F0;
        VertexConsumer glow = buffer.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE_ELYTRA_GLOW));
        this.elytraModel.renderToBuffer(poseStack, glow, fullBright, OverlayTexture.NO_OVERLAY,
                1f, 1f, 1f, 1f);
        poseStack.popPose();
    }
}
