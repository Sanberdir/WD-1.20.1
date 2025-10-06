package ru.imaginaerum.wd.client.gui.ars_melima;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ArsMelimaDraws;

import java.util.List;

public class ArsMelimaRenderer {
    public static final int CONTENT_PADDING = 4;
    public static final int CHAPTERS_PER_PAGE = 10;
    public static final int CHAPTERS_PER_COLUMN = 5;

    public static void renderChapterList(GuiGraphics graphics, int mouseX, int mouseY,
                                         int leftContentLeft, int leftContentTop, int leftContentWidth, int leftContentHeight,
                                         int rightContentLeft, int rightContentTop, int rightContentWidth, int rightContentHeight,
                                         Font font, ArsMelimaMenu menu, float textScale, int currentPage) {

        List<Chapter> chapters = menu.getChapters();

        if (chapters.isEmpty()) {
            ArsMelimaDraws.drawScaledText(graphics, font,
                    Component.translatable("screen.wd.ars_melima.no_chapters"),
                    leftContentLeft + CONTENT_PADDING, leftContentTop + CONTENT_PADDING,
                    0xFF222222, textScale);
            return;
        }

        int startIdx = currentPage * CHAPTERS_PER_PAGE;
        int endIdx = Math.min(startIdx + CHAPTERS_PER_PAGE, chapters.size());
        int midPoint = startIdx + CHAPTERS_PER_COLUMN;

        ArsMelimaRenders.renderChapterColumn(graphics, mouseX, mouseY,
                leftContentLeft, leftContentTop, leftContentWidth, leftContentHeight,
                font, chapters, startIdx, Math.min(midPoint, endIdx), textScale);

        ArsMelimaRenders.renderChapterColumn(graphics, mouseX, mouseY,
                rightContentLeft, rightContentTop, rightContentWidth, rightContentHeight,
                font, chapters, midPoint, endIdx, textScale);
    }
}