package ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;

public class BackgroundRenderer {
    public static void renderBackground(GuiGraphics graphics, int guiLeft, int guiTop) {
        int bgLeft = guiLeft + (ArsMelimaConstants.FG_W - ArsMelimaConstants.BG_W) / 2;
        int bgTop = guiTop + (ArsMelimaConstants.FG_H - ArsMelimaConstants.BG_H) / 2;

        RenderSystem.setShaderTexture(0, ArsMelimaConstants.TEXTURE);
        // Только фон (обложка)
        graphics.blit(ArsMelimaConstants.TEXTURE, bgLeft, bgTop, 4, 273,
                ArsMelimaConstants.BG_W, ArsMelimaConstants.BG_H, 512, 512);
    }
}