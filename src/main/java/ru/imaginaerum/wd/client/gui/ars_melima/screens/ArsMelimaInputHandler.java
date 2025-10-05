package ru.imaginaerum.wd.client.gui.ars_melima.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaMenu;

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
            if (handleBackArrowClick(mx, my, button, guiLeft, guiTop, menu, uiManager)) {
                return true;
            }
            if (handlePageArrowClick(mx, my, button, guiLeft, guiTop, menu, uiManager)) {
                return true;
            }
        } else {
            return handleChapterListClick(mx, my, button, guiLeft, guiTop, menu, uiManager);
        }

        return false;
    }

    private boolean handleBackArrowClick(int mx, int my, int button, int guiLeft, int guiTop,
                                         ArsMelimaMenu menu, ArsMelimaUIManager uiManager) {
        if (button == 0 && isPointInRect(guiLeft + 140, guiTop + 184, 15, 15, mx, my)) {
            menu.closeChapter();
            uiManager.setCurrentPage(0);
            playPageTurnSound();
            return true;
        }
        return false;
    }

    private boolean handlePageArrowClick(int mx, int my, int button, int guiLeft, int guiTop,
                                         ArsMelimaMenu menu, ArsMelimaUIManager uiManager) {
        if (button == 0) {
            // Левая стрелка
            if (isPointInRect(guiLeft + NAV_LEFT_REL_X, guiTop + NAV_REL_Y, 12, 7, mx, my)) {
                if (uiManager.getCurrentPage() > 0) {
                    uiManager.setCurrentPage(uiManager.getCurrentPage() - 1);
                    playPageTurnSound();
                    return true;
                }
            }
            // Правая стрелка
            if (isPointInRect(guiLeft + NAV_RIGHT_REL_X, guiTop + NAV_REL_Y, 12, 7, mx, my)) {
                // Здесь нужно вычислить pageCount, но для простоты оставим так
                uiManager.setCurrentPage(uiManager.getCurrentPage() + 1);
                playPageTurnSound();
                return true;
            }
        }
        return false;
    }

    private boolean handleChapterListClick(int mx, int my, int button, int guiLeft, int guiTop,
                                           ArsMelimaMenu menu, ArsMelimaUIManager uiManager) {
        int contentLeft = guiLeft + 8;
        int contentTop = guiTop + 20;
        int contentWidth = 137 - 8;
        int contentHeight = 160 - 20;

        if (button == 0 && isPointInRect(contentLeft, contentTop, contentWidth, contentHeight, mx, my)) {
            int idx = (my - (contentTop + 4)) / 12;
            if (idx >= 0 && idx < menu.getChapters().size()) {
                menu.openChapter(idx);
                uiManager.setCurrentPage(0);
                playPageTurnSound();
                return true;
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