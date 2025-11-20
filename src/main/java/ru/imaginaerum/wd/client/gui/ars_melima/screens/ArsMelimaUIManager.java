package ru.imaginaerum.wd.client.gui.ars_melima.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
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
 * Обновлён: поддержка TreeLinks в правой колонке и постраничная логика.
 */
public class ArsMelimaUIManager {
    private int guiLeft, guiTop;
    private ArsMelimaMenu currentMenu;

    private int currentChapterPage = 0;
    private int currentTextPage = 0;
    private int currentProgressPage = 0;
    private int currentLearningPage = 0;

    private int currentTaskPage = 0;
    public void setCurrentTaskPage(int page) { this.currentTaskPage = page; }
    public int getCurrentTaskPage() { return currentTaskPage; }

    public void setCurrentMenu(ArsMelimaMenu menu) { this.currentMenu = menu; }

    private final NodePositionsStore nodePositionsStore = new NodePositionsStore();
    private ProgressNode currentProgressNode = null;

    private final ProgressBarRenderer progressBarRenderer = new ProgressBarRenderer();

    public void setCurrentProgressPage(int page) { this.currentProgressPage = page; }
    public int getCurrentProgressPage() { return currentProgressPage; }

    public void setCurrentChapterPage(int page) { this.currentChapterPage = page; }
    public int getCurrentChapterPage() { return currentChapterPage; }

    public void setCurrentTextPage(int page) { this.currentTextPage = page; }
    public int getCurrentTextPage() { return currentTextPage; }

    public void setCurrentLearningPage(int page) { this.currentLearningPage = page; }
    public int getCurrentLearningPage() { return currentLearningPage; }

    public void setCurrentProgressNode(ProgressNode node) { this.currentProgressNode = node; setCurrentTextPage(0); }
    public ProgressNode getCurrentProgressNode() { return currentProgressNode; }

    public NodePositionsStore getNodePositionsStore() { return nodePositionsStore; }

    public Map<String, Point> getNodePositions() { return this.nodePositionsStore.getPositions(); }

    public int getGuiLeft() { return guiLeft; }
    public int getGuiTop() { return guiTop; }

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

        renderContent(graphics, mouseX, mouseY, menu, font);
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, int screenWidth, int screenHeight,
                       ArsMelimaMenu menu, ItemStack book, Font font) {
        render(graphics, mouseX, mouseY, screenWidth, screenHeight, menu, font);
    }

    private void calculatePosition(int screenWidth, int screenHeight) {
        guiLeft = (screenWidth - ArsMelimaConstants.FG_W) / 2;
        guiTop = (screenHeight - ArsMelimaConstants.FG_H) / 2;
    }

    private void renderContent(GuiGraphics graphics, int mouseX, int mouseY, ArsMelimaMenu menu, Font font) {
        // ДОБАВЛЕНО: отладочный вывод
        System.out.println("[RENDER] Current index: " + menu.getCurrentIndex());
        Chapter chapter = menu.getCurrentChapter();
        System.out.println("[RENDER] Current chapter: " + (chapter != null ? chapter.getId() : "NULL"));
        if (chapter != null) {
            System.out.println("[RENDER] Chapter elements: " + chapter.getElements().size());
            System.out.println("[RENDER] Is dynamic: " + menu.isDynamicChapterOpen());
        }

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
            renderLearningChapters(graphics, mouseX, mouseY, menu, font);
            return;
        }

        if (menu.getCurrentIndex() == -1) {
            ArsMelimaRenders.renderChapterList(graphics, mouseX, mouseY,
                    contentLeft, contentTop, contentWidth, contentHeight,
                    rightContentLeft, rightContentTop, rightContentWidth, rightContentHeight,
                    font, menu, 0.85f, currentChapterPage);

        } else {
            // УБРАНО: повторное объявление Chapter chapter
            int page = currentTextPage;

            // ДОБАВЛЕНО: проверка на динамическую главу
            if (menu.isDynamicChapterOpen()) {
                // Для динамических глав рендерим только содержимое
                System.out.println("[RENDER] Rendering dynamic chapter content");
                ArsMelimaRenders.renderChapterPage(graphics, mouseX, mouseY,
                        chapter, page,
                        contentLeft, contentTop, contentWidth, contentHeight,
                        rightContentLeft, rightContentTop, rightContentWidth, rightContentHeight,
                        font, 0.85f, ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager.ArsMelimaConstants.ICONS_TEXTURE);

            } else if (page == 0) {
                // Левая колонка — learning chapters (только для обычных глав)
                List<LearningChapter> learningChapters = menu.getLearningChapters(chapter.getId());
                renderLearningChaptersList(graphics, mouseX, mouseY,
                        contentLeft, contentTop, contentWidth, contentHeight,
                        rightContentLeft, rightContentTop, rightContentWidth, rightContentHeight,
                        font, learningChapters, 0.85f, currentLearningPage);

                // Правая колонка — tree links (только для обычных глав)
                List<TreeLink> treeLinks = menu.getTreeLinks(chapter.getId());
                renderTreeLinksList(graphics, mouseX, mouseY,
                        contentLeft, contentTop, contentWidth, contentHeight,
                        rightContentLeft, rightContentTop, rightContentWidth, rightContentHeight,
                        font, treeLinks, 0.85f, 0);

            } else {
                // Страницы TreeLinks (только для обычных глав)
                List<TreeLink> treeLinks = menu.getTreeLinks(chapter.getId());
                renderTreeLinksList(graphics, mouseX, mouseY,
                        contentLeft, contentTop, contentWidth, contentHeight,
                        rightContentLeft, rightContentTop, rightContentWidth, rightContentHeight,
                        font, treeLinks, 0.85f, page);
            }
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
            if (y + TOTAL_STRIP_HEIGHT > contentTop + contentHeight) break;
        }
    }

    private void renderTaskStrip(GuiGraphics graphics, Task task, String chapterId,
                                 int x, int y, int width, Font font) {
        int progress = ClientTaskData.getTaskProgress(chapterId, task.getId());
        int required = task.getRequiredCount();
        int displayProgress = Math.min(progress, required);
        boolean completed = displayProgress >= required;

        ArsMelimaRenders.renderChapterStrip(graphics, x, y, width, OPEN_STRIP_HEIGHT, true, false);
        drawTaskItemIcon(graphics, task, x + CONTENT_PADDING, y + (OPEN_STRIP_HEIGHT - 16) / 2);

        int textY = y + (OPEN_STRIP_HEIGHT - 4) / 2;
        int textX = x + CONTENT_PADDING + 24;

        String itemName = getItemDisplayName(task.getItem());
        String progressText = displayProgress + "/" + required;
        int textColor = completed ? 0xFF888888 : 0xFF5D4037;
        int progressColor = completed ? 0xFF666666 : 0xFF8B4513;

        graphics.pose().pushPose();
        graphics.pose().translate(textX, textY, 0);
        graphics.pose().scale(0.7f, 0.7f, 1.0f);
        graphics.drawString(font, itemName, 0, 0, textColor, false);
        graphics.pose().popPose();

        graphics.pose().pushPose();
        int progressWidth = (int)(font.width(progressText) * 0.5f);
        int progressX = x + width - CONTENT_PADDING - progressWidth;
        graphics.pose().translate(progressX, textY, 0);
        graphics.pose().scale(0.5f, 0.5f, 1.0f);
        graphics.drawString(font, progressText, 0, 0, progressColor, false);
        graphics.pose().popPose();
    }

    private void drawTaskItemIcon(GuiGraphics graphics, Task task, int x, int y) {
        Item item = task.getItem();
        if (item != null) graphics.renderItem(new ItemStack(item), x, y);
    }

    private String getItemDisplayName(Item item) {
        if (item == null) return "Unknown Item";
        return new ItemStack(item).getHoverName().getString();
    }

    // --------------------------
    // Рендер TreeLinks (list)
    // --------------------------
    private void renderTreeLinksList(GuiGraphics graphics, int mouseX, int mouseY,
                                     int leftContentLeft, int leftContentTop, int leftContentWidth, int leftContentHeight,
                                     int rightContentLeft, int rightContentTop, int rightContentWidth, int rightContentHeight,
                                     Font font, List<TreeLink> treeLinks, float scale, int currentPage) {

        if (treeLinks == null || treeLinks.isEmpty()) {
            return;
        }

        if (currentPage <= 0) {
            int startIdx = 0;
            int endIdx = Math.min(treeLinks.size(), CHAPTERS_PER_COLUMN);

            for (int i = startIdx; i < endIdx; i++) {
                TreeLink tl = treeLinks.get(i);
                int columnIndex = i - startIdx;
                int stripY = rightContentTop + CONTENT_PADDING + columnIndex * TOTAL_STRIP_HEIGHT;
                renderTreeLinkStrip(graphics, tl, rightContentLeft, stripY, rightContentWidth, OPEN_STRIP_HEIGHT, font, scale, mouseX, mouseY);
            }
            return;
        }

        int perPage = CHAPTERS_PER_PAGE;
        int startIdx = (currentPage - 1) * perPage;
        if (startIdx < 0) startIdx = 0;
        if (startIdx >= treeLinks.size()) return;
        int endIdx = Math.min(startIdx + perPage, treeLinks.size());

        // Левая колонка: startIdx .. startIdx + CHAPTERS_PER_COLUMN - 1
        for (int i = startIdx; i < Math.min(startIdx + CHAPTERS_PER_COLUMN, endIdx); i++) {
            TreeLink tl = treeLinks.get(i);
            int stripY = leftContentTop + CONTENT_PADDING + (i - startIdx) * TOTAL_STRIP_HEIGHT;
            renderTreeLinkStrip(graphics, tl, leftContentLeft, stripY, leftContentWidth, OPEN_STRIP_HEIGHT, font, scale, mouseX, mouseY);
        }

        // Правая колонка: startIdx + CHAPTERS_PER_COLUMN .. endIdx - 1
        for (int i = startIdx + CHAPTERS_PER_COLUMN; i < endIdx; i++) {
            TreeLink tl = treeLinks.get(i);
            int columnIndex = i - (startIdx + CHAPTERS_PER_COLUMN);
            int stripY = rightContentTop + CONTENT_PADDING + columnIndex * TOTAL_STRIP_HEIGHT;
            renderTreeLinkStrip(graphics, tl, rightContentLeft, stripY, rightContentWidth, OPEN_STRIP_HEIGHT, font, scale, mouseX, mouseY);
        }
    }

    private void renderTreeLinkStrip(GuiGraphics graphics, TreeLink tl,
                                     int x, int y, int width, int height,
                                     Font font, float scale,
                                     int mouseX, int mouseY) {

        if (tl == null) return;

        boolean hover = isPointInRect(x, y, width, height, mouseX, mouseY);
        ArsMelimaRenders.renderChapterStrip(graphics, x, y, width, height, true, hover);

        // --- ЛОКАЛИЗАЦИЯ ---
        String keyById = "wd.tree_links." + tl.getId();
        String localized = Component.translatable(keyById).getString();

        String title;

        if (localized == null || localized.isEmpty() || localized.equals(keyById)) {
            // Пробуем использовать title как ключ
            String titleKey = tl.getTitle();
            if (titleKey != null && !titleKey.isBlank()) {
                String loc2 = Component.translatable(titleKey).getString();
                title = (loc2 == null || loc2.isBlank() || loc2.equals(titleKey))
                        ? tl.getId()    // fallback
                        : loc2;
            } else {
                title = tl.getId();
            }
        } else {
            title = localized;
        }

        // --- ВЕРТИКАЛЬНОЕ ВЫРАВНИВАНИЕ ---
        int stripHeight = height;
        int textY = y + (stripHeight - (int)(font.lineHeight * scale)) / 2;
        int textX = x + CONTENT_PADDING + 24;

        int baseColor = hover ? 0xFFE2A65D : 0xFF5D4037;

        graphics.pose().pushPose();
        graphics.pose().translate(textX, textY, 0);
        graphics.pose().scale(scale, scale, 1.0f);

        // тень и световой обвод
        graphics.drawString(font, title, 0, -1, 0x80FFFFFF, false);
        graphics.drawString(font, title, -1, 0, 0x80DBD4B8, false);
        graphics.drawString(font, title, 1, 0, 0x80DBD4B8, false);
        graphics.drawString(font, title, 0, 1, 0x80BFB38A, false);

        // центральный текст
        graphics.drawString(font, title, 0, 0, baseColor, false);

        graphics.pose().popPose();
    }


    // ----- Learning chapters rendering -----
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

        renderLearningChaptersList(graphics, mouseX, mouseY,
                contentLeft, contentTop, contentWidth, contentHeight,
                rightContentLeft, rightContentTop, rightContentWidth, rightContentHeight,
                font, learningChapters, 0.85f, currentLearningPage);
    }

    private void renderLearningChaptersList(GuiGraphics graphics, int mouseX, int mouseY,
                                            int leftContentLeft, int leftContentTop, int leftContentWidth, int leftContentHeight,
                                            int rightContentLeft, int rightContentTop, int rightContentWidth, int rightContentHeight,
                                            Font font, List<LearningChapter> learningChapters, float scale, int currentPage) {

        if (learningChapters == null || learningChapters.isEmpty()) return;

        int chaptersPerPage = CHAPTERS_PER_PAGE;
        int startIdx = currentPage * chaptersPerPage;
        int endIdx = Math.min(startIdx + chaptersPerPage, learningChapters.size());

        checkAndUnlockChapters(learningChapters);

        // Левая колонка - начинаем с CONTENT_PADDING БЕЗ дополнительных отступов
        for (int i = startIdx; i < startIdx + CHAPTERS_PER_COLUMN && i < endIdx; i++) {
            LearningChapter lc = learningChapters.get(i);
            int columnIndex = i - startIdx;
            int stripY = leftContentTop + CONTENT_PADDING + columnIndex * TOTAL_STRIP_HEIGHT;
            renderLearningChapterStrip(graphics, lc, leftContentLeft, stripY, leftContentWidth, OPEN_STRIP_HEIGHT, font, scale, mouseX, mouseY);
        }

        // Правая колонка
        for (int i = startIdx + CHAPTERS_PER_COLUMN; i < endIdx; i++) {
            LearningChapter lc = learningChapters.get(i);
            int columnIndex = i - (startIdx + CHAPTERS_PER_COLUMN);
            int stripY = rightContentTop + CONTENT_PADDING + columnIndex * TOTAL_STRIP_HEIGHT;
            renderLearningChapterStrip(graphics, lc, rightContentLeft, stripY, rightContentWidth, OPEN_STRIP_HEIGHT, font, scale, mouseX, mouseY);
        }
    }

    private void checkAndUnlockChapters(List<LearningChapter> learningChapters) {
        if (learningChapters == null) return;

        for (LearningChapter lc : learningChapters) {
            if (lc != null && lc.isLocked()) {
                boolean progressionUnlocked = ClientCookingData.isProgressUnlocked(lc.getId());
                if (progressionUnlocked) {
                    lc.unlock();
                }

                String parentChapterId = lc.getParent();
                if (parentChapterId != null && !parentChapterId.isEmpty()) {
                    if (isLearningChapterCompleted(parentChapterId)) {
                        lc.unlock();
                    }
                }
            }
        }
    }

    private boolean isLearningChapterCompleted(String chapterId) {
        if (chapterId == null || chapterId.isEmpty()) return false;
        List<Task> tasks = TaskLoader.loadTasks(chapterId);
        if (tasks == null || tasks.isEmpty()) return true;
        for (Task t : tasks) {
            int progress = ClientTaskData.getTaskProgress(chapterId, t.getId());
            if (progress < t.getRequiredCount()) return false;
        }
        return true;
    }

    private boolean isLearningChapterEffectivelyUnlocked(LearningChapter lc) {
        if (lc == null) return false;
        if (lc.isUnlocked()) return true;

        String chapterId = lc.getId();
        if (chapterId == null || chapterId.isEmpty()) return false;

        List<Task> tasks = TaskLoader.loadTasks(chapterId);
        if (tasks == null || tasks.isEmpty()) return false;

        for (Task t : tasks) {
            int progress = ClientTaskData.getTaskProgress(chapterId, t.getId());
            if (progress < t.getRequiredCount()) return false;
        }

        lc.unlock();
        return true;
    }

    private void renderLearningChapterStrip(GuiGraphics graphics, LearningChapter lc,
                                            int x, int y, int width, int height,
                                            Font font, float scale,
                                            int mouseX, int mouseY) {

        float textScale = 0.7f; // масштаб текста

        boolean isUnlocked = isLearningChapterEffectivelyUnlocked(lc);
        boolean hover = isPointInRect(x, y, width, height, mouseX, mouseY); // <-- use full height

        ArsMelimaRenders.renderChapterStrip(graphics, x, y, width, height, isUnlocked, hover && isUnlocked);

        if (!isUnlocked) return;

        int textY = y + (height - (int)(font.lineHeight * textScale)) / 2;
        int textX = x + CONTENT_PADDING + 24;

        String title = lc.getTitle() == null || lc.getTitle().isEmpty() ? lc.getId() : lc.getTitle();
        int baseColor = hover ? 0xFFE2A65D : 0xFF5D4037;

        graphics.pose().pushPose();
        graphics.pose().translate(textX, textY, 0);
        graphics.pose().scale(textScale, textScale, 1.0f);

        // тень и световой обвод
        graphics.drawString(font, title, 0, -1, 0x80FFFFFF, false);
        graphics.drawString(font, title, -1, 0, 0x80DBD4B8, false);
        graphics.drawString(font, title, 1, 0, 0x80DBD4B8, false);
        graphics.drawString(font, title, 0, 1, 0x80BFB38A, false);

        // центральный текст
        graphics.drawString(font, title, 0, 0, baseColor, false);

        graphics.pose().popPose();
    }



    private boolean isPointInRect(int rx, int ry, int rw, int rh, int px, int py) {
        return px >= rx && py >= ry && px < rx + rw && py < ry + rh;
    }
}