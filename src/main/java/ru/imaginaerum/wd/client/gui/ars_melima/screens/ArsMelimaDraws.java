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


    public static void drawDimBackArrow(GuiGraphics graphics, ResourceLocation texture,
                                        int guiLeft, int guiTop,
                                        int srcU, int srcV, int width, int height,
                                        int xSize, int ySize, int relX, int relY) {
        int destX = guiLeft + relX;
        int destY = guiTop + relY;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F); // тусклая версия

        graphics.blit(texture, destX, destY, srcU, srcV, width, height, xSize, ySize);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    // теперь принимает hover и делает полную непрозрачность при наведении
    public static void drawBackArrow(GuiGraphics graphics, ResourceLocation texture,
                                     int guiLeft, int guiTop,
                                     int srcU, int srcV, int width, int height,
                                     int xSize, int ySize, boolean hover, int relX, int relY) {
        int destX = guiLeft + relX;
        int destY = guiTop + relY;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, hover ? 1.0f : 1.0F);

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
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        graphics.blit(texture, destX, destY, srcU, srcV, width, height, xSize, ySize);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    // теперь принимает hover и делает полную непрозрачность при наведении
    public static void drawForwardArrow(GuiGraphics graphics, ResourceLocation texture,
                                        int guiLeft, int guiTop,
                                        int srcU, int srcV, int width, int height,
                                        int xSize, int ySize, boolean hover, int relX, int relY) {
        int destX = guiLeft + relX;
        int destY = guiTop + relY;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, hover ? 1.0f : 0.7F);

        graphics.blit(texture, destX, destY, srcU, srcV, width, height, xSize, ySize);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void drawAreaBackground(GuiGraphics g, int x, int y, int w, int h) {
        int color = 0xFF222222; // чёрный

        // Верхняя
        g.fill(x - 1, y - 1, x + w + 1, y, color);
        // Нижняя
        g.fill(x - 1, y + h, x + w + 1, y + h + 1, color);
        // Левая
        g.fill(x - 1, y, x, y + h, color);
        // Правая
        g.fill(x + w, y, x + w + 1, y + h, color);
    }
    public static void drawScaledText(GuiGraphics graphics, Font font, Component text, int x, int y, int color, float scale) {
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
