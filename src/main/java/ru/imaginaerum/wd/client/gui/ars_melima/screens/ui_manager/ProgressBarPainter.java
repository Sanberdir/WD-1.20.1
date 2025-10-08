package ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Отвечает только за визуальную часть полоски: фон, заливка, текст, пульсация */
public class ProgressBarPainter {
    public void paintBackground(GuiGraphics graphics, int dstX, int dstY) {
        RenderSystem.setShaderTexture(0, ArsMelimaConstants.ICONS_TEXTURE);
        graphics.blit(ArsMelimaConstants.ICONS_TEXTURE, dstX + 36, dstY - 12, 0, 87, ProgressBarConfig.WIDTH, ProgressBarConfig.HEIGHT, 512, 512);
    }

    public void paintFill(GuiGraphics graphics, int dstX, int dstY, int fillW) {
        if (fillW <= 0) return;
        RenderSystem.setShaderTexture(0, ArsMelimaConstants.ICONS_TEXTURE);
        graphics.blit(ArsMelimaConstants.ICONS_TEXTURE, dstX + 36, dstY - 12, 0, 92, fillW, ProgressBarConfig.HEIGHT, 512, 512);
    }

    public void paintLevelText(GuiGraphics graphics, int dstX, int dstY, int level) {
        String levelText = Integer.toString(level);
        Font mcFont = Minecraft.getInstance().font;
        int textW = mcFont.width(levelText);
        int levelTextX = dstX + 36 + (ProgressBarConfig.WIDTH / 2) - (textW / 2);
        int levelTextY = dstY - 20;

        int outlineColor = ProgressBarConfig.OUTLINE_COLOR;
        int mainColor = ProgressBarConfig.MAIN_COLOR;

        double time = (System.currentTimeMillis() % 2000L) / 2000.0;
        float pulse = (float) ((Math.sin(time * Math.PI * 2) + 1) / 2.0);
        int glowColor = UIUtils.interpolateColor(ProgressBarConfig.GLOW_BASE, ProgressBarConfig.GLOW_PEAK, pulse);

        graphics.drawString(mcFont, levelText, levelTextX - 1, levelTextY, outlineColor, false);
        graphics.drawString(mcFont, levelText, levelTextX + 1, levelTextY, outlineColor, false);
        graphics.drawString(mcFont, levelText, levelTextX, levelTextY - 1, outlineColor, false);
        graphics.drawString(mcFont, levelText, levelTextX, levelTextY + 1, outlineColor, false);

        graphics.drawString(mcFont, levelText, levelTextX, levelTextY - 1, glowColor, false);
        graphics.drawString(mcFont, levelText, levelTextX, levelTextY, mainColor, false);
    }
}