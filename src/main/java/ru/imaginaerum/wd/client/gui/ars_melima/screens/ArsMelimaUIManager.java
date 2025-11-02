package ru.imaginaerum.wd.client.gui.ars_melima.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import ru.imaginaerum.wd.client.gui.ars_melima.*;
import ru.imaginaerum.wd.client.gui.ars_melima.progress_tree.ProgressNode;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager.*;

import java.util.List;
import java.util.Map;

import static ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaRenderer.*;
import static ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaRenders.OPEN_STRIP_HEIGHT;
import static ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaRenders.TOTAL_STRIP_HEIGHT;

/**
 * Координатор UI — минимальная логика, вызывает конкретные рендереры.
 */
public class ArsMelimaUIManager {
    private int guiLeft, guiTop;

    private int currentChapterPage = 0;
    private int currentTextPage = 0;
    private int currentProgressPage = 0;
    private int currentLearningPage = 0; // НОВОЕ: страница для learning chapters

    private int currentTaskPage = 0; // ДОБАВИТЬ: страница для задач
    public void setCurrentTaskPage(int page) {
        this.currentTaskPage = page;
    }

    public int getCurrentTaskPage() {
        return currentTaskPage;
    }
    private final NodePositionsStore nodePositionsStore = new NodePositionsStore();
    private ProgressNode currentProgressNode = null;

    private final ProgressBarRenderer progressBarRenderer = new ProgressBarRenderer();

    public void setCurrentProgressPage(int page) { this.currentProgressPage = page; }
    public int getCurrentProgressPage() { return currentProgressPage; }

    public void setCurrentChapterPage(int page) { this.currentChapterPage = page; }
    public int getCurrentChapterPage() { return currentChapterPage; }

    public void setCurrentTextPage(int page) { this.currentTextPage = page; }
    public int getCurrentTextPage() { return currentTextPage; }

    public void setCurrentLearningPage(int page) { this.currentLearningPage = page; } // НОВОЕ
    public int getCurrentLearningPage() { return currentLearningPage; } // НОВОЕ

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

        // Контент (дерево прогресса / список глав / текст / learning chapters)
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
        if (menu.isTasksOpen()) {
            renderTasks(graphics, mouseX, mouseY, menu, font);
            return;
        }
        if (menu.isLearningChaptersOpen()) {
            // НОВОЕ: рендерим learning chapters
            renderLearningChapters(graphics, mouseX, mouseY, menu, font);
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
    private void renderTasks(GuiGraphics graphics, int mouseX, int mouseY, ArsMelimaMenu menu, Font font) {
        int contentLeft = guiLeft + ArsMelimaConstants.CONTENT_X1;
        int contentTop = guiTop + ArsMelimaConstants.CONTENT_Y1;
        int contentWidth = ArsMelimaConstants.CONTENT_X2 - ArsMelimaConstants.CONTENT_X1;
        int contentHeight = ArsMelimaConstants.CONTENT_Y2 - ArsMelimaConstants.CONTENT_Y1;

        List<Task> tasks = menu.getCurrentTasks();
        String chapterId = menu.getCurrentTaskChapterId();

        int y = contentTop + CONTENT_PADDING;

        for (Task task : tasks) {
            renderTaskStrip(graphics, task, chapterId, contentLeft, y, contentWidth, font);
            y += TOTAL_STRIP_HEIGHT;

            // Если не помещается - прерываем
            if (y + TOTAL_STRIP_HEIGHT > contentTop + contentHeight) break;
        }
    }

    private void renderTaskStrip(GuiGraphics graphics, Task task, String chapterId,
                                 int x, int y, int width, Font font) {
        // Получаем прогресс
        int progress = ClientTaskData.getTaskProgress(chapterId, task.getId());
        int required = task.getRequiredCount();

        // Ограничиваем отображаемый прогресс
        int displayProgress = Math.min(progress, required);
        boolean completed = displayProgress >= required;

        // Рисуем фон полоски
        ArsMelimaRenders.renderChapterStrip(graphics, x, y, width, OPEN_STRIP_HEIGHT, true, false);

        // Рисуем иконку предмета
        drawTaskItemIcon(graphics, task, x + CONTENT_PADDING, y + (OPEN_STRIP_HEIGHT - 16) / 2);

        // Текст задачи
        int textY = y + (OPEN_STRIP_HEIGHT - 8) / 2;
        int textX = x + CONTENT_PADDING + 24;

        String itemName = getItemDisplayName(task.getItem());
        String progressText = displayProgress + "/" + required; // Используем ограниченное значение

        // Цвет текста: серый если выполнено, обычный если нет
        int textColor = completed ? 0xFF888888 : 0xFF5D4037;
        int progressColor = completed ? 0xFF666666 : 0xFF8B4513;

        // Название предмета
        graphics.drawString(font, itemName, textX, textY, textColor, false);

        // Прогресс справа
        int progressWidth = font.width(progressText);
        graphics.drawString(font, progressText, x + width - CONTENT_PADDING - progressWidth, textY, progressColor, false);
    }

    private void drawTaskItemIcon(GuiGraphics graphics, Task task, int x, int y) {
        Item item = task.getItem();
        if (item != null) {
            graphics.renderItem(new ItemStack(item), x, y);
        }
    }

    private String getItemDisplayName(Item item) {
        if (item == null) return "Unknown Item";
        return new ItemStack(item).getHoverName().getString();
    }
    // НОВЫЙ МЕТОД: рендеринг learning chapters
    // НОВЫЙ МЕТОД: рендеринг learning chapters как обычных глав
    private void renderLearningChapters(GuiGraphics graphics, int mouseX, int mouseY, ArsMelimaMenu menu, Font font) {
        int contentLeft = guiLeft + ArsMelimaConstants.CONTENT_X1;
        int contentTop = guiTop + ArsMelimaConstants.CONTENT_Y1;
        int contentWidth = ArsMelimaConstants.CONTENT_X2 - ArsMelimaConstants.CONTENT_X1;
        int contentHeight = ArsMelimaConstants.CONTENT_Y2 - ArsMelimaConstants.CONTENT_Y1;

        int rightContentLeft = guiLeft + ArsMelimaConstants.RIGHT_CONTENT_X1;
        int rightContentTop = guiTop + ArsMelimaConstants.RIGHT_CONTENT_Y1;
        int rightContentWidth = ArsMelimaConstants.RIGHT_CONTENT_X2 - ArsMelimaConstants.RIGHT_CONTENT_X1;
        int rightContentHeight = ArsMelimaConstants.RIGHT_CONTENT_Y2 - ArsMelimaConstants.RIGHT_CONTENT_Y1;

        List<LearningChapter> learningChapters = menu.getCurrentLearningChapters();

        // Используем существующий рендерер для списка глав, но с learning chapters
        renderLearningChaptersList(graphics, mouseX, mouseY,
                contentLeft, contentTop, contentWidth, contentHeight,
                rightContentLeft, rightContentTop, rightContentWidth, rightContentHeight,
                font, learningChapters, 0.85f, currentLearningPage);
    }

    // Метод для рендеринга списка learning chapters (аналогично renderChapterList)
    // В методе renderLearningChaptersList изменим координаты рендеринга полосок:
    private void renderLearningChaptersList(GuiGraphics graphics, int mouseX, int mouseY,
                                            int leftContentLeft, int leftContentTop, int leftContentWidth, int leftContentHeight,
                                            int rightContentLeft, int rightContentTop, int rightContentWidth, int rightContentHeight,
                                            Font font, List<LearningChapter> learningChapters, float scale, int currentPage) {

        int chaptersPerPage = CHAPTERS_PER_PAGE;
        int startIdx = currentPage * chaptersPerPage;
        int endIdx = Math.min(startIdx + chaptersPerPage, learningChapters.size());

        // Левая колонка - полоски у левой границы
        for (int i = startIdx; i < startIdx + CHAPTERS_PER_COLUMN && i < endIdx; i++) {
            LearningChapter lc = learningChapters.get(i);
            int stripY = leftContentTop + CONTENT_PADDING + (i - startIdx) * TOTAL_STRIP_HEIGHT;

            // Полоска начинается строго от левой границы контента
            renderLearningChapterStrip(graphics, lc, leftContentLeft, stripY,
                    leftContentWidth, OPEN_STRIP_HEIGHT, font, scale,
                    mouseX, mouseY);
        }

        // Правая колонка - полоски у левой границы правой колонки
        for (int i = startIdx + CHAPTERS_PER_COLUMN; i < endIdx; i++) {
            LearningChapter lc = learningChapters.get(i);
            int columnIndex = i - (startIdx + CHAPTERS_PER_COLUMN);
            int stripY = rightContentTop + CONTENT_PADDING + columnIndex * TOTAL_STRIP_HEIGHT;

            // Полоска начинается строго от левой границы правой контентной области
            renderLearningChapterStrip(graphics, lc, rightContentLeft, stripY,
                    rightContentWidth, OPEN_STRIP_HEIGHT, font, scale,
                    mouseX, mouseY);
        }
    }

    // Обновленный метод renderLearningChapterStrip:
    private void renderLearningChapterStrip(GuiGraphics graphics, LearningChapter lc,
                                            int x, int y, int width, int height,
                                            Font font, float scale,
                                            int mouseX, int mouseY) {

        boolean isUnlocked = lc.isUnlocked();

        // Hitbox также смещаем к левой границе
        boolean hover = isPointInRect(x, y, width, height, mouseX, mouseY);

        // Рисуем фон полоски от левой границы (x) на всю ширину (width)
        ArsMelimaRenders.renderChapterStrip(graphics, x, y, width, height,
                /* openLikeChapter = */ isUnlocked,
                /* hoverLikeChapter = */ hover && isUnlocked);

        // Для locked learning — текст не рисуем
        if (!isUnlocked) return;

        // Текст выравниваем относительно левой границы полоски
        int stripHeight = height;
        int textY = y + (stripHeight - 8) / 2;
        int textX = x + CONTENT_PADDING + 24; // Отступ от левой границы полоски

        String title = lc.getTitle() == null || lc.getTitle().isEmpty() ? lc.getId() : lc.getTitle();
        int baseColor = hover ? 0xFFE2A65D : 0xFF5D4037;

        graphics.drawString(font, title, textX,     textY - 1, 0x80FFFFFF, false);
        graphics.drawString(font, title, textX - 1, textY,     0x80DBD4B8, false);
        graphics.drawString(font, title, textX + 1, textY,     0x80DBD4B8, false);
        graphics.drawString(font, title, textX,     textY + 1, 0x80BFB38A, false);
        graphics.drawString(font, title, textX,     textY,     baseColor,  false);
    }

    private boolean isPointInRect(int rx, int ry, int rw, int rh, int px, int py) {
        return px >= rx && py >= ry && px < rx + rw && py < ry + rh;
    }
}