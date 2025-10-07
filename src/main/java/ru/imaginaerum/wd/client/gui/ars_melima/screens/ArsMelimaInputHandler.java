package ru.imaginaerum.wd.client.gui.ars_melima.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaMenu;
import ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaRenders;
import ru.imaginaerum.wd.client.gui.ars_melima.progress_tree.ProgressNode;

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

        if (menu.isProgressionOpen()) {
            // Режим дерева прогресса
            if (handleBackArrowClick(mx, my, button, guiLeft, guiTop, menu, uiManager)) return true;
            if (handleProgressPageArrowClick(mx, my, button, guiLeft, guiTop, menu, uiManager)) return true;
            return handleProgressNodesClick(mx, my, button, guiLeft, guiTop, menu, uiManager);
        }

        if (menu.getCurrentIndex() != -1) {
            // Режим просмотра текста главы
            if (handleBackArrowClick(mx, my, button, guiLeft, guiTop, menu, uiManager)) return true;
            if (handleTextPageArrowClick(mx, my, button, guiLeft, guiTop, menu, uiManager)) return true;
        } else {
            // Режим списка глав
            if (handleChapterPageArrowClick(mx, my, button, guiLeft, guiTop, menu, uiManager)) return true;
            return handleChapterListClick(mx, my, button, guiLeft, guiTop, menu, uiManager);
        }

        return false;
    }

    // --- общий back (назад в списки) ---
    private boolean handleBackArrowClick(int mx, int my, int button, int guiLeft, int guiTop,
                                         ArsMelimaMenu menu, ArsMelimaUIManager uiManager) {
        if (button == 0 && isPointInRect(guiLeft + 140, guiTop + 184, 15, 15, mx, my)) {
            if (menu.isProgressionOpen()) {
                menu.closeProgression();
            } else {
                menu.closeChapter();
            }
            uiManager.setCurrentTextPage(0);
            playPageTurnSound();
            return true;
        }
        return false;
    }

    // --- навигация по страницам текста главы ---
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
                // вычисление pageCount производится в UIManager при рендере; здесь позволим инкремент, UI проверит границы
                uiManager.setCurrentTextPage(uiManager.getCurrentTextPage() + 1);
                playPageTurnSound();
                return true;
            }
        }
        return false;
    }

    // --- навигация по страницам списка глав ---
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

    // --- клик по списку глав (две колонки) ---
    // --- клик по списку глав (две колонки) ---
    private boolean handleChapterListClick(int mx, int my, int button, int guiLeft, int guiTop,
                                           ArsMelimaMenu menu, ArsMelimaUIManager uiManager) {
        // Левая область
        int leftContentLeft = guiLeft + 8;
        int leftContentTop = guiTop + 20;
        int leftContentWidth = 137 - 8;    // 129 пикселей
        int leftContentHeight = 160 - 20;  // 140 пикселей

        // Правая область
        int rightContentLeft = guiLeft + 159;
        int rightContentTop = guiTop + 20;
        int rightContentWidth = leftContentWidth;
        int rightContentHeight = leftContentHeight;

        if (button == 0) {
            int currentPage = uiManager.getCurrentChapterPage();
            int startIdx = currentPage * CHAPTERS_PER_PAGE;

            // Левая колонка
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

                    if (isPointInRect(stripX, stripY, stripWidth, stripHeight, mx, my)) {
                        // ВАЖНО: вместо открытия главы — открываем дерево прогресса
                        menu.openProgression();
                        uiManager.setCurrentProgressPage(0);
                        playPageTurnSound();
                        return true;
                    }
                }
            }

            // Правая колонка
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
                        // ВАЖНО: вместо открытия главы — открываем дерево прогресса
                        menu.openProgression();
                        uiManager.setCurrentProgressPage(0);
                        playPageTurnSound();
                        return true;
                    }
                }
            }
        }
        return false;
    }


    // --- прогресс: стрелки страниц ---
    private boolean handleProgressPageArrowClick(int mx, int my, int button, int guiLeft, int guiTop,
                                                 ArsMelimaMenu menu, ArsMelimaUIManager uiManager) {
        if (button == 0) {
            int totalPages = computeProgressPageCount(menu.getProgressNodes());

            if (isPointInRect(guiLeft + NAV_LEFT_REL_X, guiTop + NAV_REL_Y, 12, 7, mx, my)) {
                if (uiManager.getCurrentProgressPage() > 0) {
                    uiManager.setCurrentProgressPage(uiManager.getCurrentProgressPage() - 1);
                    playPageTurnSound();
                    return true;
                }
            }
            if (isPointInRect(guiLeft + NAV_RIGHT_REL_X, guiTop + NAV_REL_Y, 12, 7, mx, my)) {
                if (uiManager.getCurrentProgressPage() < totalPages - 1) {
                    uiManager.setCurrentProgressPage(uiManager.getCurrentProgressPage() + 1);
                    playPageTurnSound();
                    return true;
                }
            }
        }
        return false;
    }

    private int computeProgressPageCount(java.util.List<ProgressNode> nodes) {
        if (nodes == null || nodes.isEmpty()) return 1;
        return (nodes.size() + CHAPTERS_PER_PAGE - 1) / CHAPTERS_PER_PAGE;
    }

    // --- прогресс: клик по нодам (иконка 20x20 начиная в guiLeft+30, guiTop+35, строчные ряды) ---
    private boolean handleProgressNodesClick(int mx, int my, int button, int guiLeft, int guiTop,
                                             ArsMelimaMenu menu, ArsMelimaUIManager uiManager) {
        if (button != 0) return false;
        int start = uiManager.getCurrentProgressPage() * CHAPTERS_PER_PAGE;
        int end = Math.min(start + CHAPTERS_PER_PAGE, menu.getProgressNodes().size());
        int x = guiLeft + 30;
        int y = guiTop + 35;
        int rowHeight = 24;
        for (int i = start, row = 0; i < end; i++, row++) {
            int rx = x;
            int ry = y + row * rowHeight;
            if (isPointInRect(rx, ry, 20, 20, mx, my)) {
                // Здесь можно открыть подробности ноды. Пока — звук и true.
                playPageTurnSound();
                return true;
            }
        }
        return false;
    }

    // --- утилиты ---
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
