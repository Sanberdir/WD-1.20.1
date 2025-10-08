package ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager;

import net.minecraft.client.gui.GuiGraphics;


/** Композитор: связывает модель и painter и вычисляет параметры отрисовки */
public class ProgressBarRenderer {
    private final ProgressBarPainter painter = new ProgressBarPainter();


    public void render(GuiGraphics graphics, int guiLeft, int guiTop, ProgressBarModel model) {
        int dstX = guiLeft + ProgressBarConfig.DST_X;
        int dstY = guiTop + ProgressBarConfig.DST_Y;


        float progress = model.getProgressFraction();
        int fillW = Math.max(0, (int) Math.floor(ProgressBarConfig.WIDTH * progress));


        painter.paintBackground(graphics, dstX, dstY);
        painter.paintFill(graphics, dstX, dstY, fillW);
        painter.paintLevelText(graphics, dstX, dstY, model.getLevel());
    }
}