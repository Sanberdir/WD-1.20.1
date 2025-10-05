package ru.imaginaerum.wd.client.gui.ars_melima.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaMenu;
import ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaRenderer;
import ru.imaginaerum.wd.client.gui.ars_melima.Chapter;
import ru.imaginaerum.wd.common.items.custom.ArsMelima;

public class ArsMelimaUIManager {
    private static final ResourceLocation TEXTURE = new ResourceLocation("wd", "textures/gui/ars_melima/ars_melima.png");
    private static final ResourceLocation ICONS_TEXTURE = new ResourceLocation("wd", "textures/gui/ars_melima/ars_melima_icons.png");

    private int currentPage = 0;
    private int guiLeft, guiTop;

    // Константы размеров
    private static final int FG_W = 297, FG_H = 185;
    private static final int BG_W = 305, BG_H = 184;
    private static final int CONTENT_X1 = 8, CONTENT_Y1 = 20, CONTENT_X2 = 137, CONTENT_Y2 = 160;

    public void render(GuiGraphics graphics, int mouseX, int mouseY, int screenWidth, int screenHeight,
                       ArsMelimaMenu menu, ItemStack book, Font font) {
        calculatePosition(screenWidth, screenHeight);

        renderBackground(graphics);
        renderBookmark(graphics);
        renderProgressBar(graphics, book);
        renderContentAreas(graphics);
        renderNavigation(graphics, mouseX, mouseY, menu, font);
        renderContent(graphics, mouseX, mouseY, menu, font);
    }

    private void calculatePosition(int screenWidth, int screenHeight) {
        guiLeft = (screenWidth - FG_W) / 2;
        guiTop = (screenHeight - FG_H) / 2;
    }

    private void renderBackground(GuiGraphics graphics) {
        int bgLeft = guiLeft + (FG_W - BG_W) / 2;
        int bgTop = guiTop + (FG_H - BG_H) / 2;

        RenderSystem.setShaderTexture(0, TEXTURE);
        graphics.blit(TEXTURE, bgLeft, bgTop, 4, 273, BG_W, BG_H, 512, 512);
        graphics.blit(TEXTURE, guiLeft, guiTop, 8, 12, FG_W, FG_H, 512, 512);
    }

    private void renderBookmark(GuiGraphics graphics) {
        int bookmarkLeft = guiLeft - 50;
        int bookmarkTop = guiTop + 10;

        RenderSystem.setShaderColor(1.0F, 0.0F, 0.0F, 1.0F);
        graphics.blit(TEXTURE, bookmarkLeft, bookmarkTop, 208, 227, 49, 20, 512, 512);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        ArsMelimaDraws.renderItem(graphics, Items.APPLE.getDefaultInstance(), bookmarkLeft + 4, bookmarkTop);
    }

    private void renderProgressBar(GuiGraphics graphics, ItemStack book) {
        RenderSystem.setShaderTexture(0, ICONS_TEXTURE);

        int dstX = guiLeft + 13;
        int dstY = guiTop + 162;

        // Фон прогресс-бара
        graphics.blit(ICONS_TEXTURE, dstX, dstY, 216, 96, 120, 5, 512, 512);

        // Заполнение прогресс-бара
        int xp = ArsMelima.getStoredXp(book);
        float progress = Math.min(1.0f, xp / (float) ArsMelima.MAX_XP);
        int fillW = Math.max(0, (int) Math.floor(120 * progress));

        if (fillW > 0) {
            graphics.blit(ICONS_TEXTURE, dstX, dstY, 216, 91, fillW, 5, 512, 512);
        }

        RenderSystem.setShaderTexture(0, TEXTURE);
    }

    private void renderContentAreas(GuiGraphics graphics) {
        int contentLeft = guiLeft + CONTENT_X1;
        int contentTop = guiTop + CONTENT_Y1;
        int contentWidth = CONTENT_X2 - CONTENT_X1;
        int contentHeight = CONTENT_Y2 - CONTENT_Y1;

        int rightContentLeft = guiLeft + 159;
        int rightContentTop = guiTop + 20;
        int rightContentWidth = 298 - 170;
        int rightContentHeight = 174 - 34;

        ArsMelimaDraws.drawAreaBackground(graphics, contentLeft, contentTop, contentWidth, contentHeight);
        ArsMelimaDraws.drawAreaBackground(graphics, rightContentLeft, rightContentTop, rightContentWidth, rightContentHeight);
    }

    private void renderNavigation(GuiGraphics graphics, int mouseX, int mouseY, ArsMelimaMenu menu, Font font) {
        if (menu.getCurrentIndex() != -1) {
            renderBackArrow(graphics, mouseX, mouseY);
            renderPageArrows(graphics, mouseX, mouseY, menu, font);
        }
    }

    private void renderBackArrow(GuiGraphics graphics, int mouseX, int mouseY) {
        boolean hover = isPointInRect(guiLeft + 140, guiTop + 184, 15, 15, mouseX, mouseY);

        if (hover) {
            ArsMelimaDraws.drawBackArrow(graphics, TEXTURE, guiLeft, guiTop, 177, 233, 11, 11, 512, 512, true, 142, 186);
        } else {
            ArsMelimaDraws.drawDimBackArrow(graphics, TEXTURE, guiLeft, guiTop, 177, 221, 11, 11, 512, 512, 142, 186);
        }
    }

    private void renderPageArrows(GuiGraphics graphics, int mouseX, int mouseY, ArsMelimaMenu menu, Font font) {
        Chapter current = menu.getCurrentChapter();
        if (current == null) return;

        int pageCount = ArsMelimaRenderer.computePageStarts(current, font, 0.85f,
                getContentWidth(), getContentHeight()).size();

        if (pageCount > 1) {
            renderLeftArrow(graphics, mouseX, mouseY);
            renderRightArrow(graphics, mouseX, mouseY, pageCount);
        }
    }

    private void renderLeftArrow(GuiGraphics graphics, int mouseX, int mouseY) {
        int leftNavX = guiLeft + 10;
        int navY = guiTop + 184;
        boolean hoverLeft = isPointInRect(leftNavX, navY, 12, 7, mouseX, mouseY);

        if (currentPage > 0) {
            ArsMelimaDraws.drawDimBackArrow(graphics, TEXTURE, guiLeft, guiTop, 151, 229, 12, 7, 512, 512, 10, 184);
            if (hoverLeft) {
                ArsMelimaDraws.drawBackArrow(graphics, TEXTURE, guiLeft, guiTop, 151, 237, 12, 7, 512, 512, true, 10, 184);
            }
        }
    }

    private void renderRightArrow(GuiGraphics graphics, int mouseX, int mouseY, int pageCount) {
        int rightNavX = guiLeft + 276;
        int navY = guiTop + 184;
        boolean hoverRight = isPointInRect(rightNavX, navY, 12, 7, mouseX, mouseY);

        if (currentPage < pageCount - 1) {
            ArsMelimaDraws.drawDimForwardArrow(graphics, TEXTURE, guiLeft, guiTop, 164, 229, 12, 7, 512, 512, 276, 184);
            if (hoverRight) {
                ArsMelimaDraws.drawForwardArrow(graphics, TEXTURE, guiLeft, guiTop, 164, 237, 12, 7, 512, 512, true, 276, 184);
            }
        }
    }

    private void renderContent(GuiGraphics graphics, int mouseX, int mouseY, ArsMelimaMenu menu, Font font) {
        int contentLeft = guiLeft + CONTENT_X1;
        int contentTop = guiTop + CONTENT_Y1;
        int contentWidth = CONTENT_X2 - CONTENT_X1;
        int contentHeight = CONTENT_Y2 - CONTENT_Y1;

        int rightContentLeft = guiLeft + 159;
        int rightContentTop = guiTop + 20;
        int rightContentWidth = 298 - 170;
        int rightContentHeight = 174 - 34;

        if (menu.getCurrentIndex() == -1) {
            ArsMelimaRenderer.renderChapterList(graphics, mouseX, mouseY, contentLeft, contentTop,
                    contentWidth, contentHeight, font, menu, 0.85f);
        } else {
            ArsMelimaRenderer.renderChapterPage(graphics, mouseX, mouseY, menu.getCurrentChapter(),
                    currentPage, contentLeft, contentTop, contentWidth, contentHeight,
                    rightContentLeft, rightContentTop, rightContentWidth, rightContentHeight,
                    font, 0.85f, TEXTURE);
        }
    }

    private int getContentWidth() {
        return (CONTENT_X2 - CONTENT_X1) - 8; // width - padding
    }

    private int getContentHeight() {
        return (CONTENT_Y2 - CONTENT_Y1) - 8; // height - padding
    }

    private boolean isPointInRect(int rx, int ry, int rw, int rh, int px, int py) {
        return px >= rx && py >= ry && px < rx + rw && py < ry + rh;
    }

    public void setCurrentPage(int page) {
        this.currentPage = page;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getGuiLeft() {
        return guiLeft;
    }

    public int getGuiTop() {
        return guiTop;
    }
}