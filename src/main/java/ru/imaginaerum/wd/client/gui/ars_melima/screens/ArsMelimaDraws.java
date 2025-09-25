package ru.imaginaerum.wd.client.gui.ars_melima.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;

public class ArsMelimaDraws {

    public static void drawBackChaptersArrow(GuiGraphics graphics, ResourceLocation texture,
                                             int guiLeft, int guiTop, int arrowU, int arrowV,
                                             int arrowW, int arrowH, int xSize, int ySize, boolean hover,
                                             int relX, int relY) {
        int destX = guiLeft + relX;
        int destY = guiTop + relY;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, hover ? 1.0f : 0.7f);

        graphics.blit(texture, destX, destY, arrowU, arrowV, arrowW, arrowH, xSize, ySize);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void drawDimBackArrow(GuiGraphics graphics, ResourceLocation texture,
                                        int guiLeft, int guiTop,
                                        int srcU, int srcV, int width, int height,
                                        int xSize, int ySize, int relX, int relY) {
        int destX = guiLeft + relX;
        int destY = guiTop + relY;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.5F);

        graphics.blit(texture, destX, destY, srcU, srcV, width, height, xSize, ySize);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
    public static void drawBackArrow(GuiGraphics graphics, ResourceLocation texture,
                                        int guiLeft, int guiTop,
                                        int srcU, int srcV, int width, int height,
                                        int xSize, int ySize, int relX, int relY) {
        int destX = guiLeft + relX;
        int destY = guiTop + relY;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.7F);

        graphics.blit(texture, destX, destY, srcU, srcV, width, height, xSize, ySize);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
    public static void drawDimForwardArrow(GuiGraphics graphics, ResourceLocation texture,
                                        int guiLeft, int guiTop,
                                        int srcU, int srcV, int width, int height,
                                        int xSize, int ySize, int relX, int relY) {
        int destX = guiLeft + relX;
        int destY = guiTop + relY;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.5F);

        graphics.blit(texture, destX, destY, srcU, srcV, width, height, xSize, ySize);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
    public static void drawForwardArrow(GuiGraphics graphics, ResourceLocation texture,
                                           int guiLeft, int guiTop,
                                           int srcU, int srcV, int width, int height,
                                           int xSize, int ySize, int relX, int relY) {
        int destX = guiLeft + relX;
        int destY = guiTop + relY;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.7F);

        graphics.blit(texture, destX, destY, srcU, srcV, width, height, xSize, ySize);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void drawAreaBackground(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
        g.fill(x, y, x + w, y + h, 0xFFEEEEDD);
    }

    public static void drawScaledText(GuiGraphics graphics, Font font, String text, int x, int y, int color, float scale) {
        drawScaledText(graphics, font, Component.literal(text), x, y, color, scale);
    }

    public static void drawScaledText(GuiGraphics graphics, Font font, Component text, int x, int y, int color, float scale) {
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(x, y, 0);
        poseStack.scale(scale, scale, 1.0f);
        graphics.drawString(font, text, 0, 0, color, false);
        poseStack.popPose();
    }

    public static void drawScaledText(GuiGraphics graphics, Font font, FormattedCharSequence text, int x, int y, int color, float scale) {
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(x, y, 0);
        poseStack.scale(scale, scale, 1.0f);
        graphics.drawString(font, text, 0, 0, color, false);
        poseStack.popPose();
    }


    public static void renderItem(GuiGraphics graphics, ItemStack stack, int x, int y) {
        graphics.renderItem(stack, x, y);
    }
}
