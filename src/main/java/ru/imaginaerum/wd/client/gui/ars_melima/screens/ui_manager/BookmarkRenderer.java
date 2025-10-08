package ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Items;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ArsMelimaDraws;


public class BookmarkRenderer {
    public static void renderBookmark(GuiGraphics graphics, int guiLeft, int guiTop) {
        int bookmarkLeft = guiLeft - 50;
        int bookmarkTop = guiTop + 10;


        RenderSystem.setShaderColor(1.0F, 0.0F, 0.0F, 1.0F);
        graphics.blit(ArsMelimaConstants.TEXTURE, bookmarkLeft, bookmarkTop, 208, 227, 49, 20, 512, 512);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);


        ArsMelimaDraws.renderItem(graphics, Items.APPLE.getDefaultInstance(), bookmarkLeft + 4, bookmarkTop);
    }
}