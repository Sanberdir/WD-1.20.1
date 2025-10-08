package ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager;

import net.minecraft.client.gui.GuiGraphics;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ArsMelimaDraws;


public class ContentAreasRenderer {
    public static void renderContentAreas(GuiGraphics graphics, int guiLeft, int guiTop) {
        int contentLeft = guiLeft + ArsMelimaConstants.CONTENT_X1;
        int contentTop = guiTop + ArsMelimaConstants.CONTENT_Y1;
        int contentWidth = ArsMelimaConstants.CONTENT_X2 - ArsMelimaConstants.CONTENT_X1;
        int contentHeight = ArsMelimaConstants.CONTENT_Y2 - ArsMelimaConstants.CONTENT_Y1;


        int rightContentLeft = guiLeft + ArsMelimaConstants.RIGHT_CONTENT_X1;
        int rightContentTop = guiTop + ArsMelimaConstants.RIGHT_CONTENT_Y1;
        int rightContentWidth = ArsMelimaConstants.RIGHT_CONTENT_X2 - ArsMelimaConstants.RIGHT_CONTENT_X1;
        int rightContentHeight = ArsMelimaConstants.RIGHT_CONTENT_Y2 - ArsMelimaConstants.RIGHT_CONTENT_Y1;


        ArsMelimaDraws.drawAreaBackground(graphics, contentLeft, contentTop, contentWidth, contentHeight);
        ArsMelimaDraws.drawAreaBackground(graphics, rightContentLeft, rightContentTop, rightContentWidth, rightContentHeight);
    }
}