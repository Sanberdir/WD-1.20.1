package ru.imaginaerum.wd.client.gui.ars_melima;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ArsMelimaDraws;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.Column;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.RenderUnit;

import java.util.ArrayList;
import java.util.List;

public class ArsMelimaRenderer {
    public static final int CONTENT_PADDING = 4;

    public static void renderChapterList(GuiGraphics graphics, int mouseX, int mouseY,
                                         int contentLeft, int contentTop, int contentWidth, int contentHeight,
                                         Font font, ArsMelimaMenu menu, float textScale) {
        int x = contentLeft + CONTENT_PADDING;
        int y = contentTop + CONTENT_PADDING;
        int lineHeight = 12;
        List<Chapter> chapters = menu.getChapters();

        if (chapters.isEmpty()) {
            ArsMelimaDraws.drawScaledText(graphics, font,
                    Component.translatable("screen.wd.ars_melima.no_chapters"),
                    x, y, 0xFF222222, textScale);
            return;
        }

        for (int i = 0; i < chapters.size(); i++) {
            Chapter c = chapters.get(i);
            int ty = y + i * lineHeight;
            boolean hover = isPointInRect(contentLeft + 2, ty - 2,
                    contentWidth - 4, lineHeight + 2, mouseX, mouseY);
            int color = hover ? 0xFFD4B400 : 0xFF222222;
            ArsMelimaDraws.drawScaledText(graphics, font, c.getTitle(), x, ty, color, textScale);
        }
    }

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
            // Обновляем unitIdx для следующей колонки
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
            } else { // IMAGE
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

        // Фон для изображения
        graphics.fill(dx, cy, dx + imgW, cy + imgH, 0xFFDDDDDD);

        try {
            RenderSystem.setShaderTexture(0, new ResourceLocation(unit.imageResource));
            graphics.blit(new ResourceLocation(unit.imageResource), dx, cy, 0, 0, imgW, imgH, imgW, imgH);
            RenderSystem.setShaderTexture(0, texture);
        } catch (Exception e) {
            RenderSystem.setShaderTexture(0, texture);
            // Можно добавить fallback отрисовку
        }
    }

    private static int getNextUnitIndex(List<RenderUnit> units, int startIdx, int colHeight, int linePix) {
        int cap = Math.max(0, colHeight / linePix);
        int used = 0;
        int unitIdx = startIdx;

        while (used < cap && unitIdx < units.size()) {
            RenderUnit unit = units.get(unitIdx);
            if (unit.type == RenderUnit.Type.TEXT) {
                used++;
                unitIdx++;
            } else {
                int rowsNeeded = Math.max(1, unit.rows);
                if (rowsNeeded > (cap - used)) break;
                used += rowsNeeded;
                unitIdx++;
            }
        }

        return unitIdx;
    }

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

        if (units.isEmpty()) {
            units.add(createEmptyTextUnit());
        }

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

        // Первая колонка
        idx = advanceOverColumn(units, idx, normalCap);
        // Вторая колонка
        idx = advanceOverColumn(units, idx, normalCap);

        return idx;
    }

    private static int advanceOverColumn(List<RenderUnit> units, int startIdx, int normalCap) {
        int remaining = normalCap;
        int idx = startIdx;

        while (remaining > 0 && idx < units.size()) {
            RenderUnit unit = units.get(idx);
            if (unit.type == RenderUnit.Type.TEXT) {
                remaining -= 1;
                idx += 1;
            } else {
                int need = Math.max(1, unit.rows);
                if (need > remaining) break;
                remaining -= need;
                idx += 1;
            }
        }

        return idx;
    }

    public static List<Integer> computePageStarts(Chapter chapter, Font font, float scale, int colWidth, int colHeight) {
        List<RenderUnit> units = buildRenderUnits(chapter, font, scale, colWidth, colHeight);
        return computePageStartsFromUnits(units, colWidth, colHeight, scale);
    }

    private static boolean isPointInRect(int rx, int ry, int rw, int rh, int px, int py) {
        return px >= rx && py >= ry && px < rx + rw && py < ry + rh;
    }
}