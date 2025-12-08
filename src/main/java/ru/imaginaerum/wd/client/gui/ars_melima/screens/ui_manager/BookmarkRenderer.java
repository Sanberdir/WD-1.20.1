package ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ArsMelimaDraws;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ArsMelimaUIManager;

public class BookmarkRenderer {
    public static final int RED_BOOKMARK = 0;
    public static final int BLUE_BOOKMARK = 1;
    public static final int GREEN_BOOKMARK = 2;

    // ИЗМЕНЕНИЕ: синяя вкладка выбрана по умолчанию
    private static int currentBookmark = BLUE_BOOKMARK;

    // Координаты текстуры для фона (берём красную для всех)
    private static final int[] BACKGROUND_TEXTURE = {208, 227};

    // Цвет каждой закладки (R,G,B,A)
    private static final float[][] BOOKMARK_COLORS = {
            {1.0F, 0.0F, 0.0F, 1.0F}, // Красная
            {0.0F, 0.4F, 1.0F, 1.0F}, // Синяя
            {0.0F, 1.0F, 0.0F, 1.0F}  // Зелёная
    };

    // Иконки для закладок
    private static final ItemStack[] BOOKMARK_ICONS = {
            new ItemStack(Items.APPLE),
            new ItemStack(Items.EMERALD),
            new ItemStack(Items.NETHER_STAR)
    };

    static final int BOOKMARK_WIDTH = 49;
    static final int BOOKMARK_HEIGHT = 20;

    private static final int BOOKMARK_SPACING = 20;

    // Смещение для выбранной вкладки
    private static final int SELECTED_OFFSET_X = -6;
    private static final int ICON_OFFSET_Y = 1;

    // Базовые отступы от guiTop
    private static final int BASE_TOP_OFFSET = 15;

    public static void renderBookmarks(GuiGraphics graphics, int guiLeft, int guiTop) {
        int baseLeft = guiLeft - 50 + 8;
        int baseTop = guiTop + BASE_TOP_OFFSET;

        for (int i = 0; i < 3; i++) {
            int x = baseLeft;
            int y = baseTop + i * BOOKMARK_SPACING;

            // Дополнительные вертикальные смещения для лучшего визуала
            if (i == BLUE_BOOKMARK) y += 2;
            if (i == GREEN_BOOKMARK) y += 4;

            renderSingleBookmark(graphics, x, y, i, i == currentBookmark);
        }
    }

    private static void renderSingleBookmark(GuiGraphics graphics, int x, int y, int type, boolean isSelected) {
        // Смещение влево только для выбранной вкладки
        int offsetX = isSelected ? SELECTED_OFFSET_X : 0;

        // --- фон (одинаковый для всех) ---
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(ArsMelimaConstants.TEXTURE, x + offsetX, y,
                BACKGROUND_TEXTURE[0], BACKGROUND_TEXTURE[1],
                BOOKMARK_WIDTH, BOOKMARK_HEIGHT,
                512, 512);

        // --- цвет закладки (только фон) ---
        float[] color = BOOKMARK_COLORS[type];
        RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
        graphics.blit(ArsMelimaConstants.TEXTURE, x + offsetX, y,
                BACKGROUND_TEXTURE[0], BACKGROUND_TEXTURE[1],
                BOOKMARK_WIDTH, BOOKMARK_HEIGHT,
                512, 512);

        // --- восстановление цвета перед отрисовкой предмета ---
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // --- иконка (без подсветки, чуть ниже) ---
        ArsMelimaDraws.renderItem(graphics, BOOKMARK_ICONS[type],
                x + 4 + offsetX, y + ICON_OFFSET_Y);
    }

    public static boolean handleBookmarkClick(double mx, double my, int guiLeft, int guiTop, ArsMelimaUIManager uiManager) {
        int baseLeft = guiLeft - 50 + 8;
        int baseTop = guiTop + BASE_TOP_OFFSET;

        // Проверяем каждую закладку по очереди
        for (int i = 0; i < 3; i++) {
            int x = baseLeft;
            int y = baseTop + i * BOOKMARK_SPACING;

            // Учитываем дополнительные смещения (как в renderBookmarks)
            if (i == BLUE_BOOKMARK) y += 2;
            if (i == GREEN_BOOKMARK) y += 4;

            // Получаем координаты конкретной закладки
            int bookmarkX = x;
            int bookmarkY = y;

            // Если эта закладка выбрана, учитываем смещение влево
            if (i == currentBookmark) {
                bookmarkX += SELECTED_OFFSET_X;
            }

            // Проверяем, находится ли курсор в пределах закладки
            if (mx >= bookmarkX && mx <= bookmarkX + BOOKMARK_WIDTH &&
                    my >= bookmarkY && my <= bookmarkY + BOOKMARK_HEIGHT) {
                // Устанавливаем новую выбранную вкладку в обоих местах
                currentBookmark = i;
                if (uiManager != null) {
                    uiManager.setCurrentSection(i);
                }
                return true;
            }
        }

        return false;
    }


    public static int getCurrentBookmark() {
        return currentBookmark;
    }

    // Новый метод для синхронизации с UI Manager
    public static void syncWithUIManager(ArsMelimaUIManager uiManager) {
        if (uiManager != null) {
            currentBookmark = uiManager.getCurrentSection();
        }
    }

}