// 1. Обновленный ArsMelimaUIManager с поддержкой постраничной навигации для глав
package ru.imaginaerum.wd.client.gui.ars_melima.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaMenu;
import ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaRenderer;
import ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaRenders;
import ru.imaginaerum.wd.client.gui.ars_melima.Chapter;
import ru.imaginaerum.wd.client.gui.ars_melima.progress_tree.ProgressNode;

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
    // константы (положи рядом с другими)
    private static final int COOK_BAR_DST_X = 21; // положение в интерфейсе (относительно guiLeft)
    private static final int COOK_BAR_DST_Y = 4;  // положение в интерфейсе (относительно guiTop)
    private static final int COOK_BAR_WIDTH = 183;
    private static final int COOK_BAR_HEIGHT = 5;

    // В начале класса (поля)
    private int currentProgressPage = 0; // текущая страница в режиме дерева прогресса

    // Геттеры/сеттеры
    public void setCurrentProgressPage(int page) { this.currentProgressPage = page; }
    public int getCurrentProgressPage() { return currentProgressPage; }
    public void render(GuiGraphics graphics, int mouseX, int mouseY, int screenWidth, int screenHeight,
                       ArsMelimaMenu menu, ItemStack book, Font font) {
        calculatePosition(screenWidth, screenHeight);

        renderBackground(graphics);
        renderBookmark(graphics);

        // --- показываем полоску опыта ТОЛЬКО на странице дерева прогресса ---
        if (menu != null && menu.isProgressionOpen()) {
            renderProgressBar(graphics, book);
        }

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
        // Вернём шейдер на TEXTURE (если дальше требуется)
        RenderSystem.setShaderTexture(0, TEXTURE);
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
        int dstX = guiLeft + COOK_BAR_DST_X;
        int dstY = guiTop + COOK_BAR_DST_Y;

        int cookLevel = ClientCookingData.clientLevel;
        int cookXp = ClientCookingData.clientXp;
        int maxForLevel = CookingXPManager.getMaxForLevel(cookLevel);

        float progress = maxForLevel > 0 ? Math.min(1.0f, cookXp / (float) maxForLevel) : 0f;
        int fillW = Math.max(0, (int) Math.floor(COOK_BAR_WIDTH * progress));

        // --- Фон полоски ---
        RenderSystem.setShaderTexture(0, ICONS_TEXTURE);
        graphics.blit(ICONS_TEXTURE, dstX + 36, dstY - 12, 0, 87, COOK_BAR_WIDTH, COOK_BAR_HEIGHT, 512, 512);

        // --- Заполнение полоски ---
        if (fillW > 0) {
            graphics.blit(ICONS_TEXTURE, dstX + 36, dstY - 12, 0, 92, fillW, COOK_BAR_HEIGHT, 512, 512);
        }

        // --- Отрисовка уровня ---
        String levelText = Integer.toString(cookLevel);
        Font mcFont = Minecraft.getInstance().font;
        int textW = mcFont.width(levelText);
        int levelTextX = dstX + 36 + (COOK_BAR_WIDTH / 2) - (textW / 2);
        int levelTextY = dstY - 20;

        // === Цвета ===
        int outlineColor = 0xFF2B1A0F; // тёмный контур
        int mainColor    = 0xFFFFB74D; // яркая медь (основной текст)
        int glowBase     = 0xFF7C4A1E; // мягкий внутренний жар
        int glowPeak     = 0xFFFFE0B2; // вспышка света

        // Пульсация свечения
        double time = (System.currentTimeMillis() % 2000L) / 2000.0;
        float pulse = (float) ((Math.sin(time * Math.PI * 2) + 1) / 2.0);
        int glowColor = interpolateColor(glowBase, glowPeak, pulse);

        // --- Рисуем контур ---
        graphics.drawString(mcFont, levelText, levelTextX - 1, levelTextY, outlineColor, false);
        graphics.drawString(mcFont, levelText, levelTextX + 1, levelTextY, outlineColor, false);
        graphics.drawString(mcFont, levelText, levelTextX, levelTextY - 1, outlineColor, false);
        graphics.drawString(mcFont, levelText, levelTextX, levelTextY + 1, outlineColor, false);

        // --- Внутреннее свечение (пульсация) ---
        graphics.drawString(mcFont, levelText, levelTextX, levelTextY - 1, glowColor, false);

        // --- Основной слой ---
        graphics.drawString(mcFont, levelText, levelTextX, levelTextY, mainColor, false);
    }

    // --- Интерполяция цветов ---
    private static int interpolateColor(int c1, int c2, float t) {
        int r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
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
    private void renderProgressPageArrows(GuiGraphics graphics, int mouseX, int mouseY, ArsMelimaMenu menu) {
        int totalPages = computeProgressPageCount(menu.getProgressNodes());

        if (totalPages > 1) {
            renderLeftArrow(graphics, mouseX, mouseY, currentProgressPage > 0);
            renderRightArrow(graphics, mouseX, mouseY, currentProgressPage < totalPages - 1);
        }
    }

    private int computeProgressPageCount(java.util.List<ProgressNode> nodes) {
        if (nodes == null || nodes.isEmpty()) return 1;
        return (nodes.size() + ArsMelimaRenderer.CHAPTERS_PER_PAGE - 1) / ArsMelimaRenderer.CHAPTERS_PER_PAGE;
    }

    private void renderNavigation(GuiGraphics graphics, int mouseX, int mouseY, ArsMelimaMenu menu, Font font) {
        if (menu.isProgressionOpen()) {
            renderBackArrow(graphics, mouseX, mouseY);
            renderProgressPageArrows(graphics, mouseX, mouseY, menu);
            return;
        }

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

        int pageCount = ArsMelimaRenders.computeChapterPageCount(current, font, 0.85f,
                getContentWidth(), getContentHeight()).size();

        if (pageCount > 1) {
            renderLeftArrow(graphics, mouseX, mouseY, currentTextPage > 0);
            renderRightArrow(graphics, mouseX, mouseY, currentTextPage < pageCount - 1);
        }
    }

    private void renderChapterPageArrows(GuiGraphics graphics, int mouseX, int mouseY, ArsMelimaMenu menu) {
        int totalPages = ArsMelimaRenders.computeChapterPageCount(menu.getChapters());

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
        if (menu.isProgressionOpen()) {
            renderProgressTree(graphics, mouseX, mouseY, menu, font);
            return;
        }
        if (menu.getCurrentIndex() == -1) {
            // Режим списка глав - используем обе колонки с ОДИНАКОВОЙ высотой
            ArsMelimaRenders.renderChapterList(graphics, mouseX, mouseY,
                    contentLeft, contentTop, contentWidth, contentHeight,
                    rightContentLeft, rightContentTop, rightContentWidth, rightContentHeight,
                    font, menu, 0.85f, currentChapterPage);
        } else {
            // Режим просмотра текста главы
            ArsMelimaRenders.renderChapterPage(graphics, mouseX, mouseY, menu.getCurrentChapter(),
                    currentTextPage, contentLeft, contentTop, contentWidth, contentHeight,
                    rightContentLeft, rightContentTop, rightContentWidth, rightContentHeight,
                    font, 0.85f, TEXTURE);
        }
    }
    private void renderProgressTree(GuiGraphics graphics, int mouseX, int mouseY, ArsMelimaMenu menu, Font font) {
        int x = guiLeft + 15;
        int y = guiTop + 25;

        // отрисуем n-ю ноду в колонке (показ списка по странице)
        java.util.List<ProgressNode> nodes = menu.getProgressNodes();
        if (nodes == null || nodes.isEmpty()) {
            graphics.blit(ArsMelimaUIManager.ICONS_TEXTURE, x, y, 0, 61, 20, 20, 512, 512);

            return;
        }

        int start = currentProgressPage * ArsMelimaRenderer.CHAPTERS_PER_PAGE;
        int end = Math.min(start + ArsMelimaRenderer.CHAPTERS_PER_PAGE, nodes.size());
        int row = 0;
        int rowHeight = 24;

        for (int i = start; i < end; i++, row++) {
            ProgressNode node = nodes.get(i);

            int sx = x;
            int sy = y + row * rowHeight;

            // Рисуем фон/рамку и кусок ICONS_TEXTURE: src (0,61), size 20x20
            RenderSystem.setShaderTexture(0, ICONS_TEXTURE);
            graphics.blit(ICONS_TEXTURE, sx, sy, 0, 61, 20, 20, 512, 512);
            RenderSystem.setShaderTexture(0, TEXTURE);

            // Рендер предмета по resource-name
            ItemStack stack = createItemStackFromResource(node.getItemResource());
            // центрируем предмет в 20x20 (предмет 16x16) => offset +2
            if (!stack.isEmpty()) {
                ArsMelimaDraws.renderItem(graphics, stack, sx + 2, sy + 2);
            }

            // Отрисуем название справа от иконки


            // Hover tooltip
            if (isPointInRect(sx, sy, 20, 20, mouseX, mouseY)) {
                if (node.getDescription() != null && !node.getDescription().isEmpty()) {
                    graphics.renderTooltip(font, Component.literal(node.getDescription()), mouseX, mouseY);
                }
            }
        }
    }



    private ItemStack createItemStackFromResource(String res) {
        if (res == null || res.isEmpty()) return ItemStack.EMPTY;
        try {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(res));
            if (item != null) return new ItemStack(item);
        } catch (Exception ignored) {}
        return ItemStack.EMPTY; // вместо BARRIER
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