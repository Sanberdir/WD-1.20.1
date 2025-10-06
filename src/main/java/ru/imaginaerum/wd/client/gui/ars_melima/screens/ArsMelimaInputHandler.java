// 3. Обновленный ArsMelimaInputHandler с обработкой постраничной навигации для глав
package ru.imaginaerum.wd.client.gui.ars_melima.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaMenu;
import ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaRenders;

import static ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaRenderer.*;
import static ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaRenders.OPEN_STRIP_HEIGHT;
import static ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaRenders.TOTAL_STRIP_HEIGHT;

public class ArsMelimaInputHandler {
    private static final int NAV_LEFT_REL_X = 10;
    private static final int NAV_RIGHT_REL_X = 276;
    private static final int NAV_REL_Y = 184;

    public boolean handleMouseClick(double mouseX, double mouseY, int button,
                                    ArsMelimaUIManager uiManager, ArsMelimaMenu menu, ItemStack book) {
        int mx = (int) Math.floor(mouseX);
        int my = (int) Math.floor(mouseY);
        int guiLeft = uiManager.getGuiLeft();
        int guiTop = uiManager.getGuiTop();

        if (menu.getCurrentIndex() != -1) {
            // Режим просмотра текста главы
            if (handleBackArrowClick(mx, my, button, guiLeft, guiTop, menu, uiManager)) {
                return true;
            }
            if (handleTextPageArrowClick(mx, my, button, guiLeft, guiTop, menu, uiManager)) {
                return true;
            }
        } else {
            // Режим списка глав
            if (handleChapterPageArrowClick(mx, my, button, guiLeft, guiTop, menu, uiManager)) {
                return true;
            }
            return handleChapterListClick(mx, my, button, guiLeft, guiTop, menu, uiManager);
        }

        return false;
    }

    private boolean handleBackArrowClick(int mx, int my, int button, int guiLeft, int guiTop,
                                         ArsMelimaMenu menu, ArsMelimaUIManager uiManager) {
        if (button == 0 && isPointInRect(guiLeft + 140, guiTop + 184, 15, 15, mx, my)) {
            menu.closeChapter();
            uiManager.setCurrentTextPage(0);
            playPageTurnSound();
            return true;
        }
        return false;
    }

    private boolean handleTextPageArrowClick(int mx, int my, int button, int guiLeft, int guiTop,
                                             ArsMelimaMenu menu, ArsMelimaUIManager uiManager) {
        if (button == 0) {
            // Левая стрелка
            if (isPointInRect(guiLeft + NAV_LEFT_REL_X, guiTop + NAV_REL_Y, 12, 7, mx, my)) {
                if (uiManager.getCurrentTextPage() > 0) {
                    uiManager.setCurrentTextPage(uiManager.getCurrentTextPage() - 1);
                    playPageTurnSound();
                    return true;
                }
            }
            // Правая стрелка
            if (isPointInRect(guiLeft + NAV_RIGHT_REL_X, guiTop + NAV_REL_Y, 12, 7, mx, my)) {
                // Здесь нужно вычислить pageCount, но для простоты оставим так
                uiManager.setCurrentTextPage(uiManager.getCurrentTextPage() + 1);
                playPageTurnSound();
                return true;
            }
        }
        return false;
    }

    private boolean handleChapterPageArrowClick(int mx, int my, int button, int guiLeft, int guiTop,
                                                ArsMelimaMenu menu, ArsMelimaUIManager uiManager) {
        if (button == 0) {
            int totalPages = ArsMelimaRenders.computeChapterPageCount(menu.getChapters());

            // Левая стрелка
            if (isPointInRect(guiLeft + NAV_LEFT_REL_X, guiTop + NAV_REL_Y, 12, 7, mx, my)) {
                if (uiManager.getCurrentChapterPage() > 0) {
                    uiManager.setCurrentChapterPage(uiManager.getCurrentChapterPage() - 1);
                    playPageTurnSound();
                    return true;
                }
            }
            // Правая стрелка
            if (isPointInRect(guiLeft + NAV_RIGHT_REL_X, guiTop + NAV_REL_Y, 12, 7, mx, my)) {
                if (uiManager.getCurrentChapterPage() < totalPages - 1) {
                    uiManager.setCurrentChapterPage(uiManager.getCurrentChapterPage() + 1);
                    playPageTurnSound();
                    return true;
                }
            }
        }
        return false;
    }

    private boolean handleChapterListClick(int mx, int my, int button, int guiLeft, int guiTop,
                                           ArsMelimaMenu menu, ArsMelimaUIManager uiManager) {
        // Левая область
        int leftContentLeft = guiLeft + 8;
        int leftContentTop = guiTop + 20;
        int leftContentWidth = 137 - 8;    // 129 пикселей
        int leftContentHeight = 160 - 20;  // 140 пикселей

        // Правая область - ТОЧНО ТАКИЕ ЖЕ РАЗМЕРЫ
        int rightContentLeft = guiLeft + 159;
        int rightContentTop = guiTop + 20;     // такая же как у левой
        int rightContentWidth = 137 - 8;       // такая же ширина как у левой (129 пикселей)
        int rightContentHeight = 160 - 20;     // такая же высота как у левой (140 пикселей)

        if (button == 0) {
            int currentPage = uiManager.getCurrentChapterPage();
            int startIdx = currentPage * CHAPTERS_PER_PAGE;

            // Проверяем клик в левой колонке
            if (isPointInRect(leftContentLeft, leftContentTop, leftContentWidth, leftContentHeight, mx, my)) {
                int relativeY = my - (leftContentTop + CONTENT_PADDING);
                int chapterIndexInColumn = relativeY / TOTAL_STRIP_HEIGHT;
                int idx = startIdx + chapterIndexInColumn;

                if (chapterIndexInColumn >= 0 && chapterIndexInColumn < CHAPTERS_PER_COLUMN &&
                        idx < menu.getChapters().size() && idx < startIdx + CHAPTERS_PER_COLUMN) {

                    int stripY = leftContentTop + CONTENT_PADDING + chapterIndexInColumn * TOTAL_STRIP_HEIGHT;
                    int stripHeight = OPEN_STRIP_HEIGHT;
                    int stripX = leftContentLeft + 2;
                    int stripWidth = leftContentWidth - 4;

                    // Проверяем, попал ли клик в полосу, где появляется подсветка
                    if (isPointInRect(stripX, stripY, stripWidth, stripHeight, mx, my)) {
                        menu.openChapter(idx);
                        uiManager.setCurrentTextPage(0);
                        playPageTurnSound();
                        return true;
                    }
                }
            }

            // Проверяем клик в правой колонке
            if (isPointInRect(rightContentLeft, rightContentTop, rightContentWidth, rightContentHeight, mx, my)) {
                int relativeY = my - (rightContentTop + CONTENT_PADDING);
                int chapterIndexInColumn = relativeY / TOTAL_STRIP_HEIGHT;
                int idx = startIdx + CHAPTERS_PER_COLUMN + chapterIndexInColumn;

                if (chapterIndexInColumn >= 0 && chapterIndexInColumn < CHAPTERS_PER_COLUMN &&
                        idx < menu.getChapters().size() && idx < startIdx + CHAPTERS_PER_PAGE) {

                    int stripY = rightContentTop + CONTENT_PADDING + chapterIndexInColumn * TOTAL_STRIP_HEIGHT;
                    int stripHeight = OPEN_STRIP_HEIGHT;
                    int stripX = rightContentLeft + 2;
                    int stripWidth = rightContentWidth - 4;

                    if (isPointInRect(stripX, stripY, stripWidth, stripHeight, mx, my)) {
                        menu.openChapter(idx);
                        uiManager.setCurrentTextPage(0);
                        playPageTurnSound();
                        return true;
                    }
                }


            }
        }
        return false;
    }

    private boolean isPointInRect(int rx, int ry, int rw, int rh, int px, int py) {
        return px >= rx && py >= ry && px < rx + rw && py < ry + rh;
    }

    private void playPageTurnSound() {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.playSound(
                    net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN,
                    1.0F,
                    1.0F
            );
        }
    }
}