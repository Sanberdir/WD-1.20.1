// 1. Обновленный ArsMelimaUIManager с поддержкой постраничной навигации для глав
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

public class ArsMelimaUIManager {
    private static final ResourceLocation TEXTURE = new ResourceLocation("wd", "textures/gui/ars_melima/ars_melima.png");
    public static final ResourceLocation ICONS_TEXTURE = new ResourceLocation("wd", "textures/gui/ars_melima/ars_melima_icons.png");

    private int currentChapterPage = 0; // Текущая страница в режиме списка глав
    private int currentTextPage = 0; // Текущая страница в режиме текста главы
    private int guiLeft, guiTop;

    // Константы размеров
    private static final int FG_W = 297, FG_H = 185;
    private static final int BG_W = 305, BG_H = 184;
    private static final int CONTENT_X1 = 8, CONTENT_Y1 = 20, CONTENT_X2 = 137, CONTENT_Y2 = 160;
    private static final int RIGHT_CONTENT_X1 = 159, RIGHT_CONTENT_Y1 = 20, RIGHT_CONTENT_X2 = 286, RIGHT_CONTENT_Y2 = 160;

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
        int xp = ru.imaginaerum.wd.common.items.custom.ArsMelima.getStoredXp(book);
        float progress = Math.min(1.0f, xp / (float) ru.imaginaerum.wd.common.items.custom.ArsMelima.MAX_XP);
        int fillW = Math.max(0, (int) Math.floor(120 * progress));

        if (fillW > 0) {
            graphics.blit(ICONS_TEXTURE, dstX, dstY, 216, 91, fillW, 5, 512, 512);
        }

        RenderSystem.setShaderTexture(0, TEXTURE);
    }

    private void renderContentAreas(GuiGraphics graphics) {
        // Левая область (без изменений)
        int contentLeft = guiLeft + CONTENT_X1;
        int contentTop = guiTop + CONTENT_Y1;
        int contentWidth = CONTENT_X2 - CONTENT_X1;
        int contentHeight = CONTENT_Y2 - CONTENT_Y1;

        // Правая область - ИСПРАВЛЕННЫЕ КООРДИНАТЫ
        int rightContentLeft = guiLeft + RIGHT_CONTENT_X1;
        int rightContentTop = guiTop + RIGHT_CONTENT_Y1;  // ТАКАЯ ЖЕ КАК У ЛЕВОЙ
        int rightContentWidth = RIGHT_CONTENT_X2 - RIGHT_CONTENT_X1;
        int rightContentHeight = RIGHT_CONTENT_Y2 - RIGHT_CONTENT_Y1; // ТАКАЯ ЖЕ КАК У ЛЕВОЙ

        ArsMelimaDraws.drawAreaBackground(graphics, contentLeft, contentTop, contentWidth, contentHeight);
        ArsMelimaDraws.drawAreaBackground(graphics, rightContentLeft, rightContentTop, rightContentWidth, rightContentHeight);
    }


    private void renderNavigation(GuiGraphics graphics, int mouseX, int mouseY, ArsMelimaMenu menu, Font font) {
        if (menu.getCurrentIndex() != -1) {
            // Режим просмотра текста главы
            renderBackArrow(graphics, mouseX, mouseY);
            renderTextPageArrows(graphics, mouseX, mouseY, menu, font);
        } else {
            // Режим списка глав
            renderChapterPageArrows(graphics, mouseX, mouseY, menu);
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

    private void renderTextPageArrows(GuiGraphics graphics, int mouseX, int mouseY, ArsMelimaMenu menu, Font font) {
        Chapter current = menu.getCurrentChapter();
        if (current == null) return;

        int pageCount = ArsMelimaRenderer.computePageStarts(current, font, 0.85f,
                getContentWidth(), getContentHeight()).size();

        if (pageCount > 1) {
            renderLeftArrow(graphics, mouseX, mouseY, currentTextPage > 0);
            renderRightArrow(graphics, mouseX, mouseY, currentTextPage < pageCount - 1);
        }
    }

    private void renderChapterPageArrows(GuiGraphics graphics, int mouseX, int mouseY, ArsMelimaMenu menu) {
        int totalPages = ArsMelimaRenderer.computeChapterPageCount(menu.getChapters());

        if (totalPages > 1) {
            renderLeftArrow(graphics, mouseX, mouseY, currentChapterPage > 0);
            renderRightArrow(graphics, mouseX, mouseY, currentChapterPage < totalPages - 1);
        }
    }

    private void renderLeftArrow(GuiGraphics graphics, int mouseX, int mouseY, boolean enabled) {
        int leftNavX = guiLeft + 10;
        int navY = guiTop + 184;
        boolean hoverLeft = isPointInRect(leftNavX, navY, 12, 7, mouseX, mouseY);

        if (enabled) {
            ArsMelimaDraws.drawDimBackArrow(graphics, TEXTURE, guiLeft, guiTop, 151, 229, 12, 7, 512, 512, 10, 184);
            if (hoverLeft) {
                ArsMelimaDraws.drawBackArrow(graphics, TEXTURE, guiLeft, guiTop, 151, 237, 12, 7, 512, 512, true, 10, 184);
            }
        }
    }

    private void renderRightArrow(GuiGraphics graphics, int mouseX, int mouseY, boolean enabled) {
        int rightNavX = guiLeft + 276;
        int navY = guiTop + 184;
        boolean hoverRight = isPointInRect(rightNavX, navY, 12, 7, mouseX, mouseY);

        if (enabled) {
            ArsMelimaDraws.drawDimForwardArrow(graphics, TEXTURE, guiLeft, guiTop, 164, 229, 12, 7, 512, 512, 276, 184);
            if (hoverRight) {
                ArsMelimaDraws.drawForwardArrow(graphics, TEXTURE, guiLeft, guiTop, 164, 237, 12, 7, 512, 512, true, 276, 184);
            }
        }
    }

    private void renderContent(GuiGraphics graphics, int mouseX, int mouseY, ArsMelimaMenu menu, Font font) {
        // Левая область
        int contentLeft = guiLeft + CONTENT_X1;
        int contentTop = guiTop + CONTENT_Y1;
        int contentWidth = CONTENT_X2 - CONTENT_X1;
        int contentHeight = CONTENT_Y2 - CONTENT_Y1;

        // Правая область - ИСПРАВЛЕННЫЕ КООРДИНАТЫ
        int rightContentLeft = guiLeft + RIGHT_CONTENT_X1;
        int rightContentTop = guiTop + RIGHT_CONTENT_Y1;  // ТАКАЯ ЖЕ КАК У ЛЕВОЙ
        int rightContentWidth = RIGHT_CONTENT_X2 - RIGHT_CONTENT_X1;
        int rightContentHeight = RIGHT_CONTENT_Y2 - RIGHT_CONTENT_Y1; // ТАКАЯ ЖЕ КАК У ЛЕВОЙ

        if (menu.getCurrentIndex() == -1) {
            // Режим списка глав - используем обе колонки с ОДИНАКОВОЙ высотой
            ArsMelimaRenderer.renderChapterList(graphics, mouseX, mouseY,
                    contentLeft, contentTop, contentWidth, contentHeight,
                    rightContentLeft, rightContentTop, rightContentWidth, rightContentHeight,
                    font, menu, 0.85f, currentChapterPage);
        } else {
            // Режим просмотра текста главы
            ArsMelimaRenderer.renderChapterPage(graphics, mouseX, mouseY, menu.getCurrentChapter(),
                    currentTextPage, contentLeft, contentTop, contentWidth, contentHeight,
                    rightContentLeft, rightContentTop, rightContentWidth, rightContentHeight,
                    font, 0.85f, TEXTURE);
        }
    }


    private int getContentWidth() {
        return (CONTENT_X2 - CONTENT_X1) - 8;
    }

    private int getContentHeight() {
        return (CONTENT_Y2 - CONTENT_Y1) - 8;
    }

    private boolean isPointInRect(int rx, int ry, int rw, int rh, int px, int py) {
        return px >= rx && py >= ry && px < rx + rw && py < ry + rh;
    }

    public void setCurrentChapterPage(int page) {
        this.currentChapterPage = page;
    }

    public int getCurrentChapterPage() {
        return currentChapterPage;
    }

    public void setCurrentTextPage(int page) {
        this.currentTextPage = page;
    }

    public int getCurrentTextPage() {
        return currentTextPage;
    }

    public int getGuiLeft() {
        return guiLeft;
    }

    public int getGuiTop() {
        return guiTop;
    }
}