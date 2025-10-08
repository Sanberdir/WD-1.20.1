package ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager;


import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaMenu;
import ru.imaginaerum.wd.client.gui.ars_melima.progress_tree.ProgressNode;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ArsMelimaDraws;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ArsMelimaUIManager;

public class NavigationRenderer {
    public static void renderNavigation(ArsMelimaUIManager manager, GuiGraphics graphics, int mouseX, int mouseY, ArsMelimaMenu menu, Font font) {
        if (menu.isProgressionOpen()) {
            renderBackArrow(manager.getGuiLeft(), manager.getGuiTop(), graphics, mouseX, mouseY);
            renderProgressPageArrows(manager, graphics, mouseX, mouseY, menu);
            return;
        }

        if (menu.getCurrentIndex() != -1) {
            renderBackArrow(manager.getGuiLeft(), manager.getGuiTop(), graphics, mouseX, mouseY);
            renderTextPageArrows(manager, graphics, mouseX, mouseY, menu, font);
        } else {
            renderChapterPageArrows(manager, graphics, mouseX, mouseY, menu);
        }
    }

    private static void renderBackArrow(int guiLeft, int guiTop, GuiGraphics graphics, int mouseX, int mouseY) {
        boolean hover = UIUtils.isPointInRect(guiLeft + 140, guiTop + 184, 15, 15, mouseX, mouseY);
        if (hover) {
            ArsMelimaDraws.drawBackArrow(graphics, ArsMelimaConstants.TEXTURE, guiLeft, guiTop, 177, 233, 11, 11, 512, 512, true, 142, 186);
        } else {
            ArsMelimaDraws.drawDimBackArrow(graphics, ArsMelimaConstants.TEXTURE, guiLeft, guiTop, 177, 221, 11, 11, 512, 512, 142, 186);
        }
    }

    private static void renderProgressPageArrows(ArsMelimaUIManager manager, GuiGraphics graphics, int mouseX, int mouseY, ArsMelimaMenu menu) {
        int totalPages = computeProgressPageCount(menu.getProgressNodes());
        if (totalPages > 1) {
            renderLeftArrow(manager.getGuiLeft(), manager.getGuiTop(), graphics, mouseX, mouseY, manager.getCurrentProgressPage() > 0);
            renderRightArrow(manager.getGuiLeft(), manager.getGuiTop(), graphics, mouseX, mouseY, manager.getCurrentProgressPage() < totalPages - 1);
        }
    }

    private static int computeProgressPageCount(java.util.List<ProgressNode> nodes) {
        if (nodes == null || nodes.isEmpty()) return 1;
        return (nodes.size() + ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaRenderer.CHAPTERS_PER_PAGE - 1) / ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaRenderer.CHAPTERS_PER_PAGE;
    }

    private static void renderTextPageArrows(ArsMelimaUIManager manager, GuiGraphics graphics, int mouseX, int mouseY, ArsMelimaMenu menu, Font font) {
        ru.imaginaerum.wd.client.gui.ars_melima.Chapter current = menu.getCurrentChapter();
        if (current == null) return;

        int pageCount = ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaRenders.computeChapterPageCount(current, font, 0.85f,
                getContentWidth(), getContentHeight()).size();

        if (pageCount > 1) {
            renderLeftArrow(manager.getGuiLeft(), manager.getGuiTop(), graphics, mouseX, mouseY, manager.getCurrentTextPage() > 0);
            renderRightArrow(manager.getGuiLeft(), manager.getGuiTop(), graphics, mouseX, mouseY, manager.getCurrentTextPage() < pageCount - 1);
        }
    }

    private static void renderChapterPageArrows(ArsMelimaUIManager manager, GuiGraphics graphics, int mouseX, int mouseY, ArsMelimaMenu menu) {
        int totalPages = ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaRenders.computeChapterPageCount(menu.getChapters());
        if (totalPages > 1) {
            renderLeftArrow(manager.getGuiLeft(), manager.getGuiTop(), graphics, mouseX, mouseY, manager.getCurrentChapterPage() > 0);
            renderRightArrow(manager.getGuiLeft(), manager.getGuiTop(), graphics, mouseX, mouseY, manager.getCurrentChapterPage() < totalPages - 1);
        }
    }

    private static void renderLeftArrow(int guiLeft, int guiTop, GuiGraphics graphics, int mouseX, int mouseY, boolean enabled) {
        int leftNavX = guiLeft + 10;
        int navY = guiTop + 184;
        boolean hoverLeft = UIUtils.isPointInRect(leftNavX, navY, 12, 7, mouseX, mouseY);
        if (enabled) {
            ArsMelimaDraws.drawDimBackArrow(graphics, ArsMelimaConstants.TEXTURE, guiLeft, guiTop, 151, 229, 12, 7, 512, 512, 10, 184);
            if (hoverLeft) {
                ArsMelimaDraws.drawBackArrow(graphics, ArsMelimaConstants.TEXTURE, guiLeft, guiTop, 151, 237, 12, 7, 512, 512, true, 10, 184);
            }
        }
    }

    private static void renderRightArrow(int guiLeft, int guiTop, GuiGraphics graphics, int mouseX, int mouseY, boolean enabled) {
        int rightNavX = guiLeft + 276;
        int navY = guiTop + 184;
        boolean hoverRight = UIUtils.isPointInRect(rightNavX, navY, 12, 7, mouseX, mouseY);
        if (enabled) {
            ArsMelimaDraws.drawDimForwardArrow(graphics, ArsMelimaConstants.TEXTURE, guiLeft, guiTop, 164, 229, 12, 7, 512, 512, 276, 184);
            if (hoverRight) {
                ArsMelimaDraws.drawForwardArrow(graphics, ArsMelimaConstants.TEXTURE, guiLeft, guiTop, 164, 237, 12, 7, 512, 512, true, 276, 184);
            }
        }
    }

    private static int getContentWidth() { return (ArsMelimaConstants.CONTENT_X2 - ArsMelimaConstants.CONTENT_X1) - 8; }
    private static int getContentHeight() { return (ArsMelimaConstants.CONTENT_Y2 - ArsMelimaConstants.CONTENT_Y1) - 8; }
}
