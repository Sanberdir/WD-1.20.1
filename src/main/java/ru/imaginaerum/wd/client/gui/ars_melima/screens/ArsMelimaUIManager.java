package ru.imaginaerum.wd.client.gui.ars_melima.screens;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import ru.imaginaerum.wd.client.gui.ars_melima.*;
import ru.imaginaerum.wd.client.gui.ars_melima.progress_tree.ProgressNode;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaRenderer.*;
import static ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaRenders.OPEN_STRIP_HEIGHT;
import static ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaRenders.TOTAL_STRIP_HEIGHT;

/**
 * Координатор UI — минимальная логика, вызывает конкретные рендереры.
 * Обновлено: синяя вкладка (index 1) — основные главы, красная (index 0) — базовые главы.
 */
public class ArsMelimaUIManager {
    private int guiLeft, guiTop;
    private ArsMelimaMenu currentMenu;

    // Состояние для основной вкладки (синяя)
    private int currentChapterPage = 0;
    private int currentTextPage = 0;
    private int currentProgressPage = 0;
    private int currentLearningPage = 0;

    // Состояние для базовых глав (красная вкладка)
    private int currentBaseChapterPage = 0;
    private int currentBaseTextPage = 0;
    private int currentBaseLearningPage = 0;

    private int currentSection = 1; // Дефолтная — синяя вкладка
    private int currentTaskPage = 0;

    // Для прогрессии
    private final NodePositionsStore nodePositionsStore = new NodePositionsStore();
    private ProgressNode currentProgressNode = null;
    private final ProgressBarRenderer progressBarRenderer = new ProgressBarRenderer();

    public void setCurrentTaskPage(int page) { this.currentTaskPage = page; }
    public int getCurrentTaskPage() { return currentTaskPage; }

    public void setCurrentMenu(ArsMelimaMenu menu) { this.currentMenu = menu; }

    // Геттеры и сеттеры для основной вкладки
    public void setCurrentProgressPage(int page) { this.currentProgressPage = page; }
    public int getCurrentProgressPage() { return currentProgressPage; }
    public void setCurrentChapterPage(int page) { this.currentChapterPage = page; }
    public int getCurrentChapterPage() { return currentChapterPage; }
    public void setCurrentTextPage(int page) { this.currentTextPage = page; }
    public int getCurrentTextPage() { return currentTextPage; }
    public void setCurrentLearningPage(int page) { this.currentLearningPage = page; }
    public int getCurrentLearningPage() { return currentLearningPage; }

    // Геттеры и сеттеры для базовых глав
    public void setCurrentBaseChapterPage(int page) { this.currentBaseChapterPage = page; }
    public int getCurrentBaseChapterPage() { return currentBaseChapterPage; }
    public void setCurrentBaseTextPage(int page) { this.currentBaseTextPage = page; }
    public int getCurrentBaseTextPage() { return currentBaseTextPage; }
    public void setCurrentBaseLearningPage(int page) { this.currentBaseLearningPage = page; }
    public int getCurrentBaseLearningPage() { return currentBaseLearningPage; }

    public void setCurrentProgressNode(ProgressNode node) { this.currentProgressNode = node; setCurrentTextPage(0); }
    public ProgressNode getCurrentProgressNode() { return currentProgressNode; }

    public NodePositionsStore getNodePositionsStore() { return nodePositionsStore; }
    public Map<String, Point> getNodePositions() { return this.nodePositionsStore.getPositions(); }

    public int getGuiLeft() { return guiLeft; }
    public int getGuiTop() { return guiTop; }
    public int getCurrentSection() { return currentSection; }

    public void setCurrentSection(int section) {
        if (section >= 0 && section <= 2) {
            this.currentSection = section;
            resetSectionState();
        }
    }

    private void resetSectionState() {
        if (currentMenu != null) {
            // Закрываем все открытые окна при смене раздела
            currentMenu.closeChapter();
            currentMenu.closeLearningChapters();
            currentMenu.closeTasks();
            currentMenu.closeProgression();
        }
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, int screenWidth, int screenHeight,
                       ArsMelimaMenu menu, Font font) {
        calculatePosition(screenWidth, screenHeight);
        currentMenu = menu;

        BookmarkRenderer.syncWithUIManager(this);

        // 1. Фон (самый нижний)
        BackgroundRenderer.renderBackground(graphics, guiLeft, guiTop);

        // 2. Закладки поверх фона
        BookmarkRenderer.renderBookmarks(graphics, guiLeft, guiTop);

        // 3. Белые страницы поверх закладок
        PagesRenderer.renderPages(graphics, guiLeft, guiTop);

        // 4. Чёрная рамка
//        ContentAreasRenderer.renderContentAreas(graphics, guiLeft, guiTop);

        // Прогресс-бар только в основной вкладке при открытой прогрессии
        if (currentSection == 1 && menu != null && menu.isProgressionOpen()) {
            ProgressBarModel model = new ProgressBarModel(ClientCookingData.clientLevel,
                    ClientCookingData.clientXp,
                    CookingXPManager.getMaxForLevel(ClientCookingData.clientLevel));
            progressBarRenderer.render(graphics, guiLeft, guiTop, model);
        }

        // Навигация только в основной и базовой вкладках
        if (currentSection == 0 || currentSection == 1) {
            NavigationRenderer.renderNavigation(this, graphics, mouseX, mouseY, menu, font);
        }

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
        switch (currentSection) {
            case 0: // Красная вкладка - базовые главы
                renderBaseSection(graphics, mouseX, mouseY, menu, font);
                break;
            case 1: // Синяя вкладка - основные главы
                renderMainSection(graphics, mouseX, mouseY, menu, font);
                break;
            default: // Другие вкладки
                renderNonMainSection(graphics, mouseX, mouseY, menu, font);
                break;
        }
    }

    // ===================== КРАСНАЯ ВКЛАДКА - БАЗОВЫЕ ГЛАВЫ =====================

    private void renderBaseSection(GuiGraphics graphics, int mouseX, int mouseY, ArsMelimaMenu menu, Font font) {
        int contentLeft = guiLeft + ArsMelimaConstants.CONTENT_X1;
        int contentTop = guiTop + ArsMelimaConstants.CONTENT_Y1;
        int contentWidth = ArsMelimaConstants.CONTENT_X2 - ArsMelimaConstants.CONTENT_X1;
        int contentHeight = ArsMelimaConstants.CONTENT_Y2 - ArsMelimaConstants.CONTENT_Y1;

        int rightContentLeft = guiLeft + ArsMelimaConstants.RIGHT_CONTENT_X1;
        int rightContentTop = guiTop + ArsMelimaConstants.RIGHT_CONTENT_Y1;
        int rightContentWidth = ArsMelimaConstants.RIGHT_CONTENT_X2 - ArsMelimaConstants.RIGHT_CONTENT_X1;
        int rightContentHeight = ArsMelimaConstants.RIGHT_CONTENT_Y2 - ArsMelimaConstants.RIGHT_CONTENT_Y1;

        // Проверяем, открыта ли базовая глава
        if (menu.isBaseChapterOpen()) {
            // РЕНДЕРИМ СОДЕРЖИМОЕ БАЗОВОЙ ГЛАВЫ
            renderBaseChapterContentPage(graphics, mouseX, mouseY, menu, font);
        } else {
            // Рендерим список БАЗОВЫХ глав (menu.getBaseChapters())
            List<Chapter> baseChapters = menu.getBaseChapters();

            if (baseChapters == null || baseChapters.isEmpty()) {
                // Если нет базовых глав, показываем сообщение
                renderStyledText(graphics, font, "Нет базовых глав",
                        contentLeft + (contentWidth - font.width("Нет базовых глав")) / 2,
                        contentTop + (contentHeight - font.lineHeight) / 2, 1.0f);
            } else {
                // Создаем временный объект меню для рендеринга только базовых глав
                ArsMelimaMenu baseMenuWrapper = new ArsMelimaMenu() {
                    @Override
                    public List<Chapter> getChapters() {
                        return baseChapters; // Возвращаем только базовые главы
                    }

                    @Override
                    public int getCurrentIndex() {
                        return -1; // Ничего не открыто
                    }

                    @Override
                    public boolean isProgressionOpen() {
                        return false;
                    }

                    @Override
                    public boolean isTasksOpen() {
                        return false;
                    }

                    @Override
                    public boolean isLearningChaptersOpen() {
                        return false;
                    }

                    @Override
                    public boolean isBaseChapterOpen() {
                        return false; // Список открыт, не глава
                    }
                };

                // ОТРИСОВЫВАЕМ ЗАГОЛОВОК НАД КОНТЕНТОМ (не сдвигая его)
                renderSectionHeader(graphics, font, contentLeft, contentTop, contentWidth,
                        "wd.section.base_chapters", 0xFFA42E2E);

                // Рендерим список базовых глав (координаты БЕЗ изменений)
                ArsMelimaRenders.renderChapterList(graphics, mouseX, mouseY,
                        contentLeft, contentTop, contentWidth, contentHeight,
                        rightContentLeft, rightContentTop, rightContentWidth, rightContentHeight,
                        font, baseMenuWrapper, 0.85f, currentBaseChapterPage);
            }
        }
    }

    // НОВЫЙ МЕТОД: рендеринг содержимого базовой главы
    private void renderBaseChapterContentPage(GuiGraphics graphics, int mouseX, int mouseY,
                                              ArsMelimaMenu menu, Font font) {
        Chapter chapter = menu.getCurrentBaseChapter();
        if (chapter == null) return;

        int contentLeft = guiLeft + ArsMelimaConstants.CONTENT_X1;
        int contentTop = guiTop + ArsMelimaConstants.CONTENT_Y1;
        int contentWidth = ArsMelimaConstants.CONTENT_X2 - ArsMelimaConstants.CONTENT_X1;
        int contentHeight = ArsMelimaConstants.CONTENT_Y2 - ArsMelimaConstants.CONTENT_Y1;

        int rightContentLeft = guiLeft + ArsMelimaConstants.RIGHT_CONTENT_X1;
        int rightContentTop = guiTop + ArsMelimaConstants.RIGHT_CONTENT_Y1;
        int rightContentWidth = ArsMelimaConstants.RIGHT_CONTENT_X2 - ArsMelimaConstants.RIGHT_CONTENT_X1;
        int rightContentHeight = ArsMelimaConstants.RIGHT_CONTENT_Y2 - ArsMelimaConstants.RIGHT_CONTENT_Y1;

        // Если первая страница - показываем LearningChapters и TreeLinks
        if (currentBaseTextPage == 0) {
            // Слева: LearningChapters для базовых глав
            List<LearningChapter> learningChapters = menu.getLearningChapters(chapter.getId());
            renderLearningChaptersList(graphics, mouseX, mouseY,
                    contentLeft, contentTop, contentWidth, contentHeight,
                    rightContentLeft, rightContentTop, rightContentWidth, rightContentHeight,
                    font, learningChapters, 0.85f, currentBaseLearningPage);

            // Справа: TreeLinks для базовых глав
            List<TreeLink> treeLinks = menu.getBaseTreeLinks(chapter.getId());
            renderTreeLinksList(graphics, mouseX, mouseY,
                    contentLeft, contentTop, contentWidth, contentHeight,
                    rightContentLeft, rightContentTop, rightContentWidth, rightContentHeight,
                    font, treeLinks, 0.85f, 0);
        } else {
            // Последующие страницы: только TreeLinks
            List<TreeLink> treeLinks = menu.getBaseTreeLinks(chapter.getId());
            renderTreeLinksList(graphics, mouseX, mouseY,
                    contentLeft, contentTop, contentWidth, contentHeight,
                    rightContentLeft, rightContentTop, rightContentWidth, rightContentHeight,
                    font, treeLinks, 0.85f, currentBaseTextPage);
        }

        // ДОБАВЛЕНО: Рендерим содержимое главы (текст/изображения) сверху
        renderBaseChapterTextContent(graphics, mouseX, mouseY, chapter, font);
    }

    // НОВЫЙ МЕТОД: рендеринг текстового содержимого базовой главы
    private void renderBaseChapterTextContent(GuiGraphics graphics, int mouseX, int mouseY,
                                              Chapter chapter, Font font) {
        if (chapter == null || chapter.getElements() == null) return;

        int contentLeft = guiLeft + ArsMelimaConstants.CONTENT_X1;
        int contentTop = guiTop + ArsMelimaConstants.CONTENT_Y1;
        int contentWidth = ArsMelimaConstants.CONTENT_X2 - ArsMelimaConstants.CONTENT_X1;
        int contentHeight = ArsMelimaConstants.CONTENT_Y2 - ArsMelimaConstants.CONTENT_Y1;

        int yOffset = 0;

        // Отображаем заголовок на первой странице
        if (currentBaseTextPage == 0) {
            String chapterId = chapter.getId();
            if (chapterId != null && !chapterId.isEmpty()) {
                // Пробуем локализованный заголовок
                String titleText = chapter.getTitle();
                if (titleText != null && !titleText.isEmpty()) {
                    Component titleComponent = Component.translatable(titleText);
                    String localizedTitle = titleComponent.getString();

                    // Если не нашли локализацию, используем как есть
                    if (localizedTitle.equals(titleText)) {
                        // Пробуем по ID
                        titleComponent = Component.translatable("wd.chapter." + chapterId);
                        localizedTitle = titleComponent.getString();
                    }

                    int titleWidth = font.width(localizedTitle);
                    int titleX = contentLeft + (contentWidth - titleWidth) / 2;
                    int titleY = contentTop + CONTENT_PADDING - 14;

                    renderStyledText(graphics, font, localizedTitle, titleX, titleY, 1.0f);

                    int lineY = titleY + font.lineHeight;
                    renderTitleLineUnderText(graphics, contentLeft, lineY, contentWidth);
                    yOffset = font.lineHeight - 2;
                }
            }
        }

        // === ОБЛАСТЬ ДЛЯ КОЛОНОК ===
        int columnsTop = contentTop + CONTENT_PADDING + yOffset;
        int columnsHeight = contentHeight - CONTENT_PADDING - yOffset;
        if (columnsHeight <= 0) return;

        // === РЕНДЕР КОНТЕНТА ЧЕРЕЗ СИСТЕМУ СТРАНИЦ ===
        // Используем существующий метод ArsMelimaRenders.renderChapterPage
        ArsMelimaRenders.renderChapterPage(
                graphics, mouseX, mouseY,
                chapter,
                currentBaseTextPage, // Используем страницу для базовых глав
                contentLeft, columnsTop,
                contentWidth, columnsHeight,
                // Координаты правой колонки — как у обычных глав
                guiLeft + ArsMelimaConstants.RIGHT_CONTENT_X1,
                guiTop + ArsMelimaConstants.RIGHT_CONTENT_Y1,
                ArsMelimaConstants.RIGHT_CONTENT_X2 - ArsMelimaConstants.RIGHT_CONTENT_X1,
                ArsMelimaConstants.RIGHT_CONTENT_Y2 - ArsMelimaConstants.RIGHT_CONTENT_Y1,
                font,
                0.85f,
                ArsMelimaConstants.ICONS_TEXTURE
        );
    }


    // ===================== СИНЯЯ ВКЛАДКА - ОСНОВНЫЕ ГЛАВЫ =====================

    private void renderMainSection(GuiGraphics graphics, int mouseX, int mouseY, ArsMelimaMenu menu, Font font) {
        Chapter chapter = menu.getCurrentChapter();

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

// Показываем прогресс-бар в cooking_school главе
        if (chapter != null && "cooking_school".equals(chapter.getId()) && !menu.isProgressionOpen()) {
            ProgressBarModel model = new ProgressBarModel(ClientCookingData.clientLevel,
                    ClientCookingData.clientXp,
                    CookingXPManager.getMaxForLevel(ClientCookingData.clientLevel));
            progressBarRenderer.render(graphics, guiLeft, guiTop, model);
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
            // ОТРИСОВЫВАЕМ ЗАГОЛОВОК НАД КОНТЕНТОМ (не сдвигая его)
            renderSectionHeader(graphics, font, contentLeft, contentTop, contentWidth,
                    "wd.section.main_chapters", 0xFF2E6DA4);

            // Рендерим список ОСНОВНЫХ глав (координаты БЕЗ изменений)
            ArsMelimaRenders.renderChapterList(graphics, mouseX, mouseY,
                    contentLeft, contentTop, contentWidth, contentHeight,
                    rightContentLeft, rightContentTop, rightContentWidth, rightContentHeight,
                    font, menu, 0.85f, currentChapterPage);

        } else {
            int page = currentTextPage;

            if (menu.isDynamicChapterOpen()) {
                renderDynamicChapterContent(graphics, mouseX, mouseY, chapter, font);

            } else if (page == 0) {
                List<LearningChapter> learningChapters = menu.getLearningChapters(chapter.getId());
                renderLearningChaptersList(graphics, mouseX, mouseY,
                        contentLeft, contentTop, contentWidth, contentHeight,
                        rightContentLeft, rightContentTop, rightContentWidth, rightContentHeight,
                        font, learningChapters, 0.85f, currentLearningPage);

                List<TreeLink> treeLinks = menu.getTreeLinks(chapter.getId());
                renderTreeLinksList(graphics, mouseX, mouseY,
                        contentLeft, contentTop, contentWidth, contentHeight,
                        rightContentLeft, rightContentTop, rightContentWidth, rightContentHeight,
                        font, treeLinks, 0.85f, 0);

            } else {
                List<TreeLink> treeLinks = menu.getTreeLinks(chapter.getId());
                renderTreeLinksList(graphics, mouseX, mouseY,
                        contentLeft, contentTop, contentWidth, contentHeight,
                        rightContentLeft, rightContentTop, rightContentWidth, rightContentHeight,
                        font, treeLinks, 0.85f, page);
            }
        }
    }
    private void renderSectionHeader(GuiGraphics graphics, Font font,
                                     int contentLeft, int contentTop, int contentWidth,
                                     String localizationKey, int titleColor) {
        // Получаем локализованный текст
        Component titleComponent = Component.translatable(localizationKey);
        String titleText = titleComponent.getString();

        // Fallback если локализация не найдена
        if (titleText.equals(localizationKey)) {
            switch(localizationKey) {
                case "wd.section.base_chapters":
                    titleText = "Базовые главы";
                    break;
                case "wd.section.main_chapters":
                    titleText = "Основные главы";
                    break;
                default:
                    titleText = "Оглавление";
            }
        }

        // Рассчитываем позицию заголовка (над контентом)
        int titleWidth = font.width(titleText);
        int titleX = contentLeft + (contentWidth - titleWidth) / 2;
        int titleY = contentTop - 10; // Рисуем НАД контентом

        // Рисуем заголовок с тем же стилем, что и в renderStyledText
        renderStyledText(graphics, font, titleText, titleX, titleY, 1.0f);

        // Рисуем декоративную линию под заголовком (над контентом)
        int lineY = titleY + font.lineHeight;
        renderTitleLineUnderText(graphics, contentLeft, lineY, contentWidth);
    }

    // ===================== ДРУГИЕ ВКЛАДКИ =====================

    private void renderNonMainSection(GuiGraphics graphics, int mouseX, int mouseY, ArsMelimaMenu menu, Font font) {
        int contentLeft = guiLeft + ArsMelimaConstants.CONTENT_X1;
        int contentTop = guiTop + ArsMelimaConstants.CONTENT_Y1;
        int contentWidth = ArsMelimaConstants.CONTENT_X2 - ArsMelimaConstants.CONTENT_X1;
        int contentHeight = ArsMelimaConstants.CONTENT_Y2 - ArsMelimaConstants.CONTENT_Y1;

        String message = "";
        switch (currentSection) {
            case 2: // Зелёная закладка
                message = "WIP";
                break;
            default:
                message = "Раздел в разработке";
        }

        int textWidth = font.width(message);
        int textX = contentLeft + (contentWidth - textWidth) / 2;
        int textY = contentTop + (contentHeight - font.lineHeight) / 2;

        renderStyledText(graphics, font, message, textX, textY, 1.0f);
    }

    // ===================== ОБЩИЕ МЕТОДЫ РЕНДЕРИНГА =====================

    private void renderDynamicChapterContent(GuiGraphics graphics, int mouseX, int mouseY,
                                             Chapter chapter, Font font) {
        if (chapter == null || chapter.getElements() == null) return;

        int contentLeft = guiLeft + ArsMelimaConstants.CONTENT_X1;
        int contentTop = guiTop + ArsMelimaConstants.CONTENT_Y1;
        int contentWidth = ArsMelimaConstants.CONTENT_X2 - ArsMelimaConstants.CONTENT_X1;
        int contentHeight = ArsMelimaConstants.CONTENT_Y2 - ArsMelimaConstants.CONTENT_Y1;

        int yOffset = 0;

        if (currentTextPage == 0) {
            String chapterId = chapter.getId();
            if (chapterId != null && !chapterId.isEmpty()) {
                Component titleComponent = Component.translatable("wd.chapter." + chapterId);
                String titleText = titleComponent.getString();
                int titleWidth = font.width(titleText);
                int titleX = contentLeft + (contentWidth - titleWidth) / 2;
                int titleY = contentTop + CONTENT_PADDING - 14;

                renderStyledText(graphics, font, titleText, titleX, titleY, 1.0f);

                int lineY = titleY + font.lineHeight;
                renderTitleLineUnderText(graphics, contentLeft, lineY, contentWidth);
                yOffset = font.lineHeight - 2;
            }
        }

        int columnsTop = contentTop + CONTENT_PADDING + yOffset;
        int columnsHeight = contentHeight - CONTENT_PADDING - yOffset;
        if (columnsHeight <= 0) return;

        ArsMelimaRenders.renderChapterPage(
                graphics, mouseX, mouseY,
                chapter,
                currentTextPage,
                contentLeft, columnsTop,
                contentWidth, columnsHeight,
                guiLeft + ArsMelimaConstants.RIGHT_CONTENT_X1,
                guiTop + ArsMelimaConstants.RIGHT_CONTENT_Y1,
                ArsMelimaConstants.RIGHT_CONTENT_X2 - ArsMelimaConstants.RIGHT_CONTENT_X1,
                ArsMelimaConstants.RIGHT_CONTENT_Y2 - ArsMelimaConstants.RIGHT_CONTENT_Y1,
                font,
                0.85f,
                ArsMelimaConstants.ICONS_TEXTURE
        );
    }

    private void renderTitleLineUnderText(GuiGraphics graphics, int x, int y, int width) {
        int textureX = 0;
        int textureY = 82;
        int textureWidth = 107;
        int textureHeight = 5;

        int lineX = x + (width - textureWidth) / 2;
        int lineY = y;

        graphics.blit(
                ArsMelimaConstants.ICONS_TEXTURE,
                lineX, lineY,
                textureX, textureY,
                textureWidth, textureHeight,
                512, 512
        );
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
        drawTaskItemIcon(graphics, task, x + CONTENT_PADDING - 1, y + (OPEN_STRIP_HEIGHT - 16) / 2 + 1);

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

        // Левая колонка
        for (int i = startIdx; i < Math.min(startIdx + CHAPTERS_PER_COLUMN, endIdx); i++) {
            TreeLink tl = treeLinks.get(i);
            int stripY = leftContentTop + CONTENT_PADDING + (i - startIdx) * TOTAL_STRIP_HEIGHT;
            renderTreeLinkStrip(graphics, tl, leftContentLeft, stripY, leftContentWidth, OPEN_STRIP_HEIGHT, font, scale, mouseX, mouseY);
        }

        // Правая колонка
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

        String keyById = "wd.tree_links." + tl.getId();
        String localized = Component.translatable(keyById).getString();

        String title;

        if (localized == null || localized.isEmpty() || localized.equals(keyById)) {
            String titleKey = tl.getTitle();
            if (titleKey != null && !titleKey.isBlank()) {
                String loc2 = Component.translatable(titleKey).getString();
                title = (loc2 == null || loc2.isBlank() || loc2.equals(titleKey))
                        ? tl.getId()
                        : loc2;
            } else {
                title = tl.getId();
            }
        } else {
            title = localized;
        }

        int stripHeight = height;
        int textY = y + (stripHeight - (int)(font.lineHeight * scale)) / 2;
        int textX = x + CONTENT_PADDING + 24;

        int baseColor = hover ? 0xFFE2A65D : 0xFF5D4037;

        graphics.pose().pushPose();
        graphics.pose().translate(textX, textY, 0);
        graphics.pose().scale(scale, scale, 1.0f);

        graphics.drawString(font, title, 0, -1, 0x80FFFFFF, false);
        graphics.drawString(font, title, -1, 0, 0x80DBD4B8, false);
        graphics.drawString(font, title, 1, 0, 0x80DBD4B8, false);
        graphics.drawString(font, title, 0, 1, 0x80BFB38A, false);

        graphics.drawString(font, title, 0, 0, baseColor, false);

        graphics.pose().popPose();
    }

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

        {
            Component titleComponent = Component.translatable("wd.learning_chapters");
            String titleText = titleComponent.getString();
            int titleWidth = font.width(titleText);

            int titleX = leftContentLeft + (leftContentWidth - titleWidth) / 2;
            int titleY = leftContentTop + CONTENT_PADDING - 14;

            renderStyledText(graphics, font, titleText, titleX, titleY, 1.0f);

            int lineY = titleY + font.lineHeight;
            renderLearningTitleLine(graphics, leftContentLeft, lineY, leftContentWidth);

            leftContentTop = lineY + 4;
        }

        // Левая колонка
        for (int i = startIdx; i < startIdx + CHAPTERS_PER_COLUMN && i < endIdx; i++) {
            LearningChapter lc = learningChapters.get(i);
            int columnIndex = i - startIdx;
            int stripY = leftContentTop + columnIndex * TOTAL_STRIP_HEIGHT;

            renderLearningChapterStrip(graphics, lc,
                    leftContentLeft, stripY,
                    leftContentWidth, OPEN_STRIP_HEIGHT,
                    font, scale, mouseX, mouseY
            );
        }

        // Правая колонка
        for (int i = startIdx + CHAPTERS_PER_COLUMN; i < endIdx; i++) {
            LearningChapter lc = learningChapters.get(i);
            int columnIndex = i - (startIdx + CHAPTERS_PER_COLUMN);
            int stripY = rightContentTop + CONTENT_PADDING + columnIndex * TOTAL_STRIP_HEIGHT;

            renderLearningChapterStrip(graphics, lc,
                    rightContentLeft, stripY,
                    rightContentWidth, OPEN_STRIP_HEIGHT,
                    font, scale, mouseX, mouseY
            );
        }
    }

    private void renderLearningTitleLine(GuiGraphics graphics, int x, int y, int width) {
        int textureX = 0;
        int textureY = 82;
        int textureWidth = 107;
        int textureHeight = 5;

        int lineX = x + (width - textureWidth) / 2;
        int lineY = y;

        graphics.blit(
                ArsMelimaConstants.ICONS_TEXTURE,
                lineX, lineY,
                textureX, textureY,
                textureWidth, textureHeight,
                512, 512
        );
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

        float textScale = 0.7f;

        boolean isUnlocked = isLearningChapterEffectivelyUnlocked(lc);
        boolean hover = isPointInRect(x, y, width, height, mouseX, mouseY);

        ArsMelimaRenders.renderChapterStrip(graphics, x, y, width, height, isUnlocked, hover && isUnlocked);

        if (!isUnlocked) return;

        int textY = y + (height - (int)(font.lineHeight * textScale)) / 2;
        int textX = x + CONTENT_PADDING + 24;

        String title = lc.getTitle() == null || lc.getTitle().isEmpty() ? lc.getId() : lc.getTitle();
        int baseColor = hover ? 0xFFE2A65D : 0xFF5D4037;

        graphics.pose().pushPose();
        graphics.pose().translate(textX, textY, 0);
        graphics.pose().scale(textScale, textScale, 1.0f);

        graphics.drawString(font, title, 0, -1, 0x80FFFFFF, false);
        graphics.drawString(font, title, -1, 0, 0x80DBD4B8, false);
        graphics.drawString(font, title, 1, 0, 0x80DBD4B8, false);
        graphics.drawString(font, title, 0, 1, 0x80BFB38A, false);

        graphics.drawString(font, title, 0, 0, baseColor, false);

        graphics.pose().popPose();
    }

    private void renderStyledText(GuiGraphics graphics, Font font, String text,
                                  int x, int y, float scale, boolean hover) {
        if (text == null || text.isEmpty()) return;

        int baseColor = hover ? 0xFFE2A65D : 0xFF5D4037;

        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1.0f);

        graphics.drawString(font, text, 0, -1, 0x80FFFFFF, false);
        graphics.drawString(font, text, -1, 0, 0x80DBD4B8, false);
        graphics.drawString(font, text, 1, 0, 0x80DBD4B8, false);
        graphics.drawString(font, text, 0, 1, 0x80BFB38A, false);

        graphics.drawString(font, text, 0, 0, baseColor, false);

        graphics.pose().popPose();
    }

    private void renderStyledText(GuiGraphics graphics, Font font, String text,
                                  int x, int y, float scale) {
        renderStyledText(graphics, font, text, x, y, scale, false);
    }

    private boolean isPointInRect(int rx, int ry, int rw, int rh, int px, int py) {
        return px >= rx && py >= ry && px < rx + rw && py < ry + rh;
    }
}