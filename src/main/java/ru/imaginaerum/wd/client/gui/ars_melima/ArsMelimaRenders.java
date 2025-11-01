package ru.imaginaerum.wd.client.gui.ars_melima;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.Registry;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ArsMelimaDraws;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.Column;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.RenderUnit;

import java.util.ArrayList;
import java.util.List;

import static ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaRenderer.CONTENT_PADDING;
import static ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager.ArsMelimaConstants.ICONS_TEXTURE;

public class ArsMelimaRenders {
    public static final int OPEN_STRIP_HEIGHT = 23;
    private static final int CLOSED_STRIP_HEIGHT = 18;
    private static final int LINE_SPACING = 3;
    public static final int TOTAL_STRIP_HEIGHT = OPEN_STRIP_HEIGHT + LINE_SPACING;

    private static final int ITEM_ICON_SIZE = 16; // стандартный размер иконки предмета
    private static final int ITEM_ICON_LEFT_GAP = 4; // отступ от левой границы контента

    // ─────────────────────────────────────────────
    // Рендер списка глав (одной колонки)
    // ─────────────────────────────────────────────
    public static void renderChapterColumn(GuiGraphics graphics, int mouseX, int mouseY,
                                           int contentLeft, int contentTop, int contentWidth, int contentHeight,
                                           Font font, List<Chapter> chapters, int startIdx, int endIdx, float textScale) {
        int x = contentLeft + CONTENT_PADDING;
        int y = contentTop + CONTENT_PADDING;
        int availableHeight = contentHeight - CONTENT_PADDING * 2;
        int maxPossibleChapters = availableHeight / TOTAL_STRIP_HEIGHT;

        for (int i = startIdx; i < endIdx && i < startIdx + maxPossibleChapters; i++) {
            Chapter chapter = chapters.get(i);
            int stripHeight = chapter.isOpen() ? OPEN_STRIP_HEIGHT : CLOSED_STRIP_HEIGHT;

            int CLOSED_STRIP_TOP_MARGIN = 3;
            int renderY = y;
            if (!chapter.isOpen()) renderY += CLOSED_STRIP_TOP_MARGIN;

            boolean hover = isPointInRect(contentLeft + 2, renderY,
                    contentWidth - 4, stripHeight, mouseX, mouseY);

            renderChapterStrip(graphics, contentLeft, renderY, contentWidth, stripHeight, chapter.isOpen(), hover);

            // --- отрисовка иконки предмета слева по центру по Y (если задана)
            drawChapterIconIfPresent(graphics, chapter, contentLeft, renderY, stripHeight);

            if (chapter.isOpen()) {
                int textY = renderY + (stripHeight - 8) / 2;
                int textX = x + 24; // смещение текста вправо, чтобы не пересекаться с иконкой
                String title = chapter.getTitle();
                int baseColor = hover ? 0xFFE2A65D : 0xFF5D4037;

                graphics.drawString(font, title, textX, textY - 1, 0x80FFFFFF, false);
                graphics.drawString(font, title, textX - 1, textY, 0x80DBD4B8, false);
                graphics.drawString(font, title, textX + 1, textY, 0x80DBD4B8, false);
                graphics.drawString(font, title, textX, textY + 1, 0x80BFB38A, false);
                graphics.drawString(font, title, textX, textY, baseColor, false);
            }

            y += TOTAL_STRIP_HEIGHT;
        }
    }

    // ─────────────────────────────────────────────
    // Отрисовка полоски главы
    // ─────────────────────────────────────────────
    public static void renderChapterStrip(GuiGraphics graphics, int x, int y, int width, int height,
                                          boolean open, boolean hover) {
        RenderSystem.setShaderTexture(0, ICONS_TEXTURE);
        int srcX = 0;
        int srcY = open ? 18 : 42;
        int srcHeight = open ? 23 : 18;

        graphics.blit(ICONS_TEXTURE, x, y, srcX, srcY, width, srcHeight, 512, 512);

        if (hover) graphics.fill(x, y, x + width, y + srcHeight, 0x20FFFFFF);
    }

    // ─────────────────────────────────────────────
    // Проверка клика по заголовку
    // ─────────────────────────────────────────────
    public static List<Integer> computeChapterPageCount(Chapter chapter, Font font, float scale,
                                                        int colWidth, int colHeight) {
        List<RenderUnit> units = buildRenderUnits(chapter, font, scale, colWidth, colHeight);
        return computePageStartsFromUnits(units, colWidth, colHeight, scale);
    }

    /**
     * Подсчёт количества страниц при отображении списка глав.
     */
    public static int computeChapterPageCount(List<Chapter> chapters) {
        if (chapters == null || chapters.isEmpty()) {
            return 1;
        }
        return (chapters.size() + ArsMelimaRenderer.CHAPTERS_PER_PAGE - 1)
                / ArsMelimaRenderer.CHAPTERS_PER_PAGE;
    }

    /**
     * Прокси-метод для обратной совместимости вызова renderChapterList
     */
    public static void renderChapterList(GuiGraphics graphics, int mouseX, int mouseY,
                                         int leftContentLeft, int leftContentTop, int leftContentWidth, int leftContentHeight,
                                         int rightContentLeft, int rightContentTop, int rightContentWidth, int rightContentHeight,
                                         Font font, ArsMelimaMenu menu, float textScale, int currentPage) {
        ArsMelimaRenderer.renderChapterList(graphics, mouseX, mouseY,
                leftContentLeft, leftContentTop, leftContentWidth, leftContentHeight,
                rightContentLeft, rightContentTop, rightContentWidth, rightContentHeight,
                font, menu, textScale, currentPage);
    }
    // ─────────────────────────────────────────────
    // Рендер содержимого страницы главы
    // ─────────────────────────────────────────────
    public static void renderChapterPage(GuiGraphics graphics, int mouseX, int mouseY,
                                         Chapter chapter, int page,
                                         int contentLeft, int contentTop, int contentWidth, int contentHeight,
                                         int rightContentLeft, int rightContentTop, int rightContentWidth, int rightContentHeight,
                                         Font font, float scale, ResourceLocation texture) {
        if (chapter == null) return;

        Column[] cols = {
                new Column(contentLeft + CONTENT_PADDING, contentTop + CONTENT_PADDING,
                        contentWidth - CONTENT_PADDING * 2, contentHeight - CONTENT_PADDING * 2),
                new Column(rightContentLeft + CONTENT_PADDING, rightContentTop + CONTENT_PADDING,
                        rightContentWidth - CONTENT_PADDING * 2, rightContentHeight - CONTENT_PADDING * 2)
        };

        List<RenderUnit> units = buildRenderUnits(chapter, font, scale, cols[0].width, cols[0].height);
        List<Integer> pageStarts = computePageStartsFromUnits(units, cols[0].width, cols[0].height, scale);

        if (page < 0) page = 0;
        if (page >= pageStarts.size()) page = pageStarts.size() - 1;

        int unitIdx = pageStarts.get(page);
        int linePix = Math.max(1, (int) (10 * scale));

        for (int col = 0; col < cols.length; col++) {
            renderColumnContent(graphics, cols[col], units, unitIdx, linePix, font, scale, texture);
            unitIdx = getNextUnitIndex(units, unitIdx, cols[col].height, linePix);
        }
    }

    private static void renderColumnContent(GuiGraphics graphics, Column column,
                                            List<RenderUnit> units, int startUnitIdx,
                                            int linePix, Font font, float scale, ResourceLocation texture) {
        int cx = column.left;
        int cy = column.top;
        int availableHeight = column.height;
        int cap = Math.max(0, availableHeight / linePix);
        int used = 0;
        int unitIdx = startUnitIdx;

        while (used < cap && unitIdx < units.size()) {
            RenderUnit unit = units.get(unitIdx);

            if (unit.type == RenderUnit.Type.TEXT) {
                ArsMelimaDraws.drawScaledText(graphics, font, unit.line, cx, cy, 0xFF000000, scale);
                cy += linePix;
                used++;
                unitIdx++;
            } else {
                int imgW = Math.min(110, column.width);
                int desiredImgH = Math.min(70, column.height / 2);
                int imgH = Math.max(1, desiredImgH);
                int rowsNeeded = Math.max(1, (int) Math.ceil((double) imgH / linePix));
                if (rowsNeeded > (cap - used)) break;

                renderImageElement(graphics, unit, cx, cy, column.width, imgW, imgH, texture);
                cy += rowsNeeded * linePix;
                used += rowsNeeded;
                unitIdx++;
            }
        }
    }

    private static void renderImageElement(GuiGraphics graphics, RenderUnit unit,
                                           int cx, int cy, int colWidth, int imgW, int imgH,
                                           ResourceLocation texture) {
        int dx = cx + (colWidth - imgW) / 2;
        graphics.fill(dx, cy, dx + imgW, cy + imgH, 0xFFDDDDDD);
        try {
            RenderSystem.setShaderTexture(0, new ResourceLocation(unit.imageResource));
            graphics.blit(new ResourceLocation(unit.imageResource), dx, cy, 0, 0, imgW, imgH, imgW, imgH);
            RenderSystem.setShaderTexture(0, texture);
        } catch (Exception e) {
            RenderSystem.setShaderTexture(0, texture);
        }
    }

    private static int getNextUnitIndex(List<RenderUnit> units, int startIdx, int colHeight, int linePix) {
        int cap = Math.max(0, colHeight / linePix);
        int used = 0;
        int unitIdx = startIdx;
        while (used < cap && unitIdx < units.size()) {
            RenderUnit unit = units.get(unitIdx);
            int rowsNeeded = (unit.type == RenderUnit.Type.TEXT) ? 1 : Math.max(1, unit.rows);
            if (rowsNeeded > (cap - used)) break;
            used += rowsNeeded;
            unitIdx++;
        }
        return unitIdx;
    }

    // ─────────────────────────────────────────────
    // Разбиение текста и изображений на RenderUnit
    // ─────────────────────────────────────────────
    public static List<RenderUnit> buildRenderUnits(Chapter chapter, Font font, float scale, int colWidth, int colHeight) {
        List<RenderUnit> units = new ArrayList<>();
        if (chapter == null) return units;

        int linePix = Math.max(1, (int) (10 * scale));
        int normalCap = Math.max(1, colHeight / linePix);

        for (ChapterElement el : chapter.getElements()) {
            if (el == null) continue;
            switch (el.getType()) {
                case TEXT -> addTextUnits(units, el.getData(), font, scale, colWidth);
                case IMAGE -> addImageUnit(units, el.getData(), colHeight, linePix, normalCap);
            }
        }

        if (units.isEmpty()) units.add(createEmptyTextUnit());
        return units;
    }

    private static void addTextUnits(List<RenderUnit> units, String text, Font font, float scale, int colWidth) {
        String txt = text != null ? text : "";
        List<FormattedCharSequence> lines = font.split(Component.literal(txt), (int) (colWidth / scale));
        for (FormattedCharSequence line : lines) {
            RenderUnit unit = new RenderUnit();
            unit.type = RenderUnit.Type.TEXT;
            unit.line = line;
            unit.rows = 1;
            units.add(unit);
        }
    }

    private static void addImageUnit(List<RenderUnit> units, String resource, int colHeight, int linePix, int normalCap) {
        int imgH = Math.min(60, colHeight / 2);
        int rows = Math.max(1, (int) Math.ceil((double) imgH / linePix));
        rows = Math.min(rows, normalCap);

        RenderUnit unit = new RenderUnit();
        unit.type = RenderUnit.Type.IMAGE;
        unit.imageResource = resource != null ? resource : "";
        unit.rows = rows;
        units.add(unit);
    }

    private static RenderUnit createEmptyTextUnit() {
        RenderUnit unit = new RenderUnit();
        unit.type = RenderUnit.Type.TEXT;
        unit.line = Component.literal("").getVisualOrderText();
        unit.rows = 1;
        return unit;
    }

    public static List<Integer> computePageStartsFromUnits(List<RenderUnit> units, int colWidth, int colHeight, float scale) {
        List<Integer> starts = new ArrayList<>();
        if (units == null || units.isEmpty()) {
            starts.add(0);
            return starts;
        }

        int linePix = Math.max(1, (int) (10 * scale));
        int normalCap = Math.max(1, colHeight / linePix);
        int idx = 0;
        while (idx < units.size()) {
            starts.add(idx);
            idx = advanceOverTwoColumns(units, idx, normalCap);
        }
        return starts;
    }

    private static int advanceOverTwoColumns(List<RenderUnit> units, int startIdx, int normalCap) {
        int idx = startIdx;
        idx = advanceOverColumn(units, idx, normalCap);
        idx = advanceOverColumn(units, idx, normalCap);
        return idx;
    }

    private static int advanceOverColumn(List<RenderUnit> units, int startIdx, int normalCap) {
        int remaining = normalCap;
        int idx = startIdx;
        while (remaining > 0 && idx < units.size()) {
            RenderUnit unit = units.get(idx);
            int need = (unit.type == RenderUnit.Type.TEXT) ? 1 : Math.max(1, unit.rows);
            if (need > remaining) break;
            remaining -= need;
            idx++;
        }
        return idx;
    }

    private static boolean isPointInRect(int rx, int ry, int rw, int rh, int px, int py) {
        return px >= rx && py >= ry && px < rx + rw && py < ry + rh;
    }

    // -------------------------------------------------
    // Вспомогательная логика: рендер иконки предмета
    // -------------------------------------------------
    private static void drawChapterIconIfPresent(GuiGraphics graphics, Chapter chapter, int contentLeft, int renderY, int stripHeight) {
        if (chapter == null) return;
        String iconRes = chapter.getIcon();
        if (iconRes == null || iconRes.isEmpty()) return;

        try {
            ResourceLocation itemRL = new ResourceLocation(iconRes);
            Item item = BuiltInRegistries.ITEM.getOptional(itemRL).orElse(Items.AIR);
            if (item == Items.AIR) return;

            ItemStack stack = new ItemStack(item, 1);

            int iconX = contentLeft + CONTENT_PADDING + ITEM_ICON_LEFT_GAP - 5;
            int iconY = renderY + (stripHeight - ITEM_ICON_SIZE) / 2;

            // Рисуем иконку предмета через GuiGraphics (правильный путь в 1.20+)
            graphics.renderItem(stack, iconX, iconY);

            // Если нужно отрисовать количество/декорации (число, зачарование), использовать:
            // graphics.renderItemDecorations(Minecraft.getInstance().font, stack, iconX, iconY);

        } catch (Exception e) {
            // молча пропускаем, чтобы GUI не крашнулся
        }
    }
}
