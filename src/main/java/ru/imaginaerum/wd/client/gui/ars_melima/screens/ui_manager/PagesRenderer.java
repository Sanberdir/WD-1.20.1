package ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;

public class PagesRenderer {
    public static void renderPages(GuiGraphics graphics, int guiLeft, int guiTop) {
        RenderSystem.setShaderTexture(0, ArsMelimaConstants.TEXTURE);
        // Только белые страницы
        graphics.blit(ArsMelimaConstants.TEXTURE, guiLeft, guiTop, 8, 12,
                ArsMelimaConstants.FG_W, ArsMelimaConstants.FG_H, 512, 512);
    }
}
