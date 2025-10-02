package ru.imaginaerum.wd.client.gui.ars_melima;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ArsMelimaDraws;

import java.util.ArrayList;
import java.util.List;

/**
 * Новый вспомогательный класс — вынесена логика рендеринга страниц/списка глав и вспомогательные
 * вычисления размеров/юнитов. Все методы статические, ArsMelimaScreen делегирует им вызовы, при этом
 * сигнатуры методов в ArsMelimaScreen не изменены.
 */
public class ArsMelimaRenderer {

    public static void renderChapterList(GuiGraphics graphics, int mouseX, int mouseY,
                                         int contentLeft, int contentTop, int contentWidth, int contentHeight,
                                         Font font, ArsMelimaMenu menu, float textScale) {
        int x = contentLeft + 4;
        int y = contentTop + 4;
        int lineHeight = 12;
        List<Chapter> chapters = menu.getChapters();
        if (chapters.isEmpty()) {
            ArsMelimaDraws.drawScaledText(graphics, font, Component.translatable("screen.wd.ars_melima.no_chapters"), x, y, 0xFF222222, textScale);
            return;
        }
        for (int i = 0; i < chapters.size(); i++) {
            Chapter c = chapters.get(i);
            int ty = y + i * lineHeight;
            boolean hover = isPointInRect(contentLeft + 2, ty - 2, contentWidth - 4, lineHeight + 2, mouseX, mouseY);
            int color = hover ? 0xFFD4B400 : 0xFF222222;
            ArsMelimaDraws.drawScaledText(graphics, font, c.getTitle(), x, ty, color, textScale);
        }
    }

    public static void renderChapterPage(GuiGraphics graphics, int mouseX, int mouseY,
                                         Chapter chapter, int page,
                                         int contentLeft, int contentTop, int contentWidth, int contentHeight,
                                         int rightContentLeft, int rightContentTop, int rightContentWidth, int rightContentHeight,
                                         Font font, float scale, ResourceLocation TEXTURE) {
        if (chapter == null) return;

        int padding = ArsMelimaScreen.CONTENT_PADDING;

        ArsMelimaScreen.Column[] cols = {
                new ArsMelimaScreen.Column(contentLeft + padding, contentTop + padding, contentWidth - padding * 2, contentHeight - padding * 2),
                new ArsMelimaScreen.Column(rightContentLeft + padding, rightContentTop + padding, rightContentWidth - padding * 2, rightContentHeight - padding * 2)
        };

        // Собираем "юниты"
        List<ArsMelimaScreen.RenderUnit> units = buildRenderUnits(chapter, font, scale, cols[0].width, cols[0].height);

        // Получаем старты страниц (индексы юнитов)
        List<Integer> pageStarts = computePageStartsFromUnits(units, cols[0].width, cols[0].height, scale);

        // Корректируем текущую страницу
        if (page < 0) page = 0;
        if (page >= pageStarts.size()) page = pageStarts.size() - 1;

        int unitIdx = pageStarts.get(page);

        int linePix = Math.max(1, (int) (10 * scale));

        // Рисуем по колонкам
        for (int col = 0; col < cols.length; col++) {
            int cx = cols[col].left;
            int cy = cols[col].top;
            int availableHeight = cols[col].height;
            int cap = Math.max(0, availableHeight / linePix);
            int used = 0;

            while (used < cap && unitIdx < units.size()) {
                ArsMelimaScreen.RenderUnit u = units.get(unitIdx);

                if (u.type == ArsMelimaScreen.RenderUnit.Type.TEXT) {
                    ArsMelimaDraws.drawScaledText(graphics, font, u.line, cx, cy, 0xFF000000, scale);
                    cy += linePix;
                    used++;
                    unitIdx++;
                } else { // IMAGE
                    int imgW = Math.min(110, cols[col].width);
                    int desiredImgH = Math.min(70, cols[col].height / 2);
                    int imgH = Math.max(1, desiredImgH);
                    int dx = cx + (cols[col].width - imgW) / 2;
                    int rowsNeeded = Math.max(1, (int) Math.ceil((double) imgH / linePix));
                    if (rowsNeeded > (cap - used)) {
                        break;
                    }

                    graphics.fill(dx, cy, dx + imgW, cy + imgH, 0xFFDDDDDD);
                    try {
                        RenderSystem.setShaderTexture(0, new ResourceLocation(u.imageResource));
                        graphics.blit(new ResourceLocation(u.imageResource), dx, cy, 0, 0, imgW, imgH, imgW, imgH);
                        RenderSystem.setShaderTexture(0, TEXTURE);
                    } catch (Exception e) {
                        RenderSystem.setShaderTexture(0, TEXTURE);
                    }

                    cy += rowsNeeded * linePix;
                    used += rowsNeeded;
                    unitIdx++;
                }
            }
        }
    }

    public static List<ArsMelimaScreen.RenderUnit> buildRenderUnits(Chapter chapter, Font font, float scale, int colWidth, int colHeight) {
        List<ArsMelimaScreen.RenderUnit> units = new ArrayList<>();
        if (chapter == null) return units;

        int linePix = Math.max(1, (int) (10 * scale));
        int normalCap = Math.max(1, colHeight / linePix);

        for (ChapterElement el : chapter.getElements()) {
            if (el == null) continue;
            if (el.getType() == ChapterElement.Type.TEXT) {
                String txt = el.getData() == null ? "" : el.getData();
                List<FormattedCharSequence> lines = font.split(Component.literal(txt), (int) (colWidth / scale));
                for (FormattedCharSequence l : lines) {
                    ArsMelimaScreen.RenderUnit ru = new ArsMelimaScreen.RenderUnit();
                    ru.type = ArsMelimaScreen.RenderUnit.Type.TEXT;
                    ru.line = l;
                    ru.rows = 1;
                    units.add(ru);
                }
            } else if (el.getType() == ChapterElement.Type.IMAGE) {
                String res = el.getData();
                int imgH = Math.min(60, colHeight / 2);
                int rows = Math.max(1, (int) Math.ceil((double) imgH / linePix));
                rows = Math.min(rows, normalCap);
                ArsMelimaScreen.RenderUnit ru = new ArsMelimaScreen.RenderUnit();
                ru.type = ArsMelimaScreen.RenderUnit.Type.IMAGE;
                ru.imageResource = res == null ? "" : res;
                ru.rows = rows;
                units.add(ru);
            }
        }

        if (units.isEmpty()) {
            ArsMelimaScreen.RenderUnit ru = new ArsMelimaScreen.RenderUnit();
            ru.type = ArsMelimaScreen.RenderUnit.Type.TEXT;
            ru.line = Component.literal("").getVisualOrderText();
            ru.rows = 1;
            units.add(ru);
        }

        return units;
    }

    public static List<Integer> computePageStartsFromUnits(List<ArsMelimaScreen.RenderUnit> units, int colWidth, int colHeight, float scale) {
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

            int remaining = normalCap;
            while (remaining > 0 && idx < units.size()) {
                ArsMelimaScreen.RenderUnit u = units.get(idx);
                if (u.type == ArsMelimaScreen.RenderUnit.Type.TEXT) {
                    remaining -= 1;
                    idx += 1;
                } else {
                    int need = Math.max(1, u.rows);
                    if (need > remaining) break;
                    remaining -= need;
                    idx += 1;
                }
            }

            remaining = normalCap;
            while (remaining > 0 && idx < units.size()) {
                ArsMelimaScreen.RenderUnit u = units.get(idx);
                if (u.type == ArsMelimaScreen.RenderUnit.Type.TEXT) {
                    remaining -= 1;
                    idx += 1;
                } else {
                    int need = Math.max(1, u.rows);
                    if (need > remaining) break;
                    remaining -= need;
                    idx += 1;
                }
            }
        }

        return starts;
    }

    public static List<Integer> computePageStarts(Chapter chapter, Font font, float scale, int colWidth, int colHeight) {
        List<ArsMelimaScreen.RenderUnit> units = buildRenderUnits(chapter, font, scale, colWidth, colHeight);
        return computePageStartsFromUnits(units, colWidth, colHeight, scale);
    }

    // Вспомогательное для hover в списке
    private static boolean isPointInRect(int rx, int ry, int rw, int rh, int px, int py) {
        return px >= rx && py >= ry && px < rx + rw && py < ry + rh;
    }
}