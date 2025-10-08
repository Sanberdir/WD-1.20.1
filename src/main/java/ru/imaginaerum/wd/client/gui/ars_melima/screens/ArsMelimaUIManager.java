package ru.imaginaerum.wd.client.gui.ars_melima.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaMenu;
import ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaRenders;
import ru.imaginaerum.wd.client.gui.ars_melima.progress_tree.ProgressNode;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager.*;

import java.util.Map;

/**
 * Координатор UI — минимальная логика, вызывает конкретные рендереры.
 */
public class ArsMelimaUIManager {
    private int guiLeft, guiTop;

    private int currentChapterPage = 0;
    private int currentTextPage = 0;
    private int currentProgressPage = 0;

    private final NodePositionsStore nodePositionsStore = new NodePositionsStore();
    private ProgressNode currentProgressNode = null;

    private final ProgressBarRenderer progressBarRenderer = new ProgressBarRenderer();

    public void setCurrentProgressPage(int page) { this.currentProgressPage = page; }
    public int getCurrentProgressPage() { return currentProgressPage; }

    public void setCurrentChapterPage(int page) { this.currentChapterPage = page; }
    public int getCurrentChapterPage() { return currentChapterPage; }

    public void setCurrentTextPage(int page) { this.currentTextPage = page; }
    public int getCurrentTextPage() { return currentTextPage; }

    public void setCurrentProgressNode(ProgressNode node) { this.currentProgressNode = node; setCurrentTextPage(0); }
    public ProgressNode getCurrentProgressNode() { return currentProgressNode; }

    public NodePositionsStore getNodePositionsStore() { return nodePositionsStore; }

    /**
     * Возвращает карту позиций узлов для обработчиков кликов.
     * Сохраняет совместимость с существующим кодом, использующим uiManager.getNodePositions().
     */
    public Map<String, Point> getNodePositions() {
        return this.nodePositionsStore.getPositions();
    }

    public int getGuiLeft() { return guiLeft; }
    public int getGuiTop() { return guiTop; }

    /**
     * Основной render без ItemStack (чистая версия).
     */
    public void render(GuiGraphics graphics, int mouseX, int mouseY, int screenWidth, int screenHeight,
                       ArsMelimaMenu menu, Font font) {
        calculatePosition(screenWidth, screenHeight);

        BackgroundRenderer.renderBackground(graphics, guiLeft, guiTop);
        BookmarkRenderer.renderBookmark(graphics, guiLeft, guiTop);

        if (menu != null && menu.isProgressionOpen()) {
            ProgressBarModel model = new ProgressBarModel(ClientCookingData.clientLevel,
                    ClientCookingData.clientXp,
                    CookingXPManager.getMaxForLevel(ClientCookingData.clientLevel));
            progressBarRenderer.render(graphics, guiLeft, guiTop, model);
        }

        ContentAreasRenderer.renderContentAreas(graphics, guiLeft, guiTop);

        NavigationRenderer.renderNavigation(this, graphics, mouseX, mouseY, menu, font);

        // Контент (дерево прогресса / список глав / текст)
        renderContent(graphics, mouseX, mouseY, menu, font);
    }

    /**
     * Backwards-compatible overload: старый вызов с ItemStack остаётся работоспособным.
     * Просто перенаправляет на новую реализацию (book игнорируется, если он не нужен).
     */
    public void render(GuiGraphics graphics, int mouseX, int mouseY, int screenWidth, int screenHeight,
                       ArsMelimaMenu menu, ItemStack book, Font font) {
        render(graphics, mouseX, mouseY, screenWidth, screenHeight, menu, font);
    }

    private void calculatePosition(int screenWidth, int screenHeight) {
        guiLeft = (screenWidth - ArsMelimaConstants.FG_W) / 2;
        guiTop = (screenHeight - ArsMelimaConstants.FG_H) / 2;
    }

    private void renderContent(GuiGraphics graphics, int mouseX, int mouseY, ArsMelimaMenu menu, Font font) {
        int contentLeft = guiLeft + ArsMelimaConstants.CONTENT_X1;
        int contentTop = guiTop + ArsMelimaConstants.CONTENT_Y1;
        int contentWidth = ArsMelimaConstants.CONTENT_X2 - ArsMelimaConstants.CONTENT_X1;
        int contentHeight = ArsMelimaConstants.CONTENT_Y2 - ArsMelimaConstants.CONTENT_Y1;

        int rightContentLeft = guiLeft + ArsMelimaConstants.RIGHT_CONTENT_X1;
        int rightContentTop = guiTop + ArsMelimaConstants.RIGHT_CONTENT_Y1;
        int rightContentWidth = ArsMelimaConstants.RIGHT_CONTENT_X2 - ArsMelimaConstants.RIGHT_CONTENT_X1;
        int rightContentHeight = ArsMelimaConstants.RIGHT_CONTENT_Y2 - ArsMelimaConstants.RIGHT_CONTENT_Y1;

        if (menu.isProgressionOpen()) {
            ProgressTreeRenderer.renderProgressTree(this, graphics, mouseX, mouseY, menu, font);
            return;
        }

        if (menu.getCurrentIndex() == -1) {
            ArsMelimaRenders.renderChapterList(graphics, mouseX, mouseY,
                    contentLeft, contentTop, contentWidth, contentHeight,
                    rightContentLeft, rightContentTop, rightContentWidth, rightContentHeight,
                    font, menu, 0.85f, currentChapterPage);
        } else {
            ArsMelimaRenders.renderChapterPage(graphics, mouseX, mouseY, menu.getCurrentChapter(),
                    currentTextPage, contentLeft, contentTop, contentWidth, contentHeight,
                    rightContentLeft, rightContentTop, rightContentWidth, rightContentHeight,
                    font, 0.85f, ArsMelimaConstants.TEXTURE);
        }
    }
}
