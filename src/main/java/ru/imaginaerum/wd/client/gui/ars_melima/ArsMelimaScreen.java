package ru.imaginaerum.wd.client.gui.ars_melima;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Items;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ArsMelimaDraws;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Обновлённый экран книги: поддержка произвольного числа элементов в главе
 * (текстов и изображений) в любом порядке.
 */
public class ArsMelimaScreen extends Screen {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("wd", "textures/gui/ars_melima/ars_melima.png");

    public ArsMelimaScreen() {
        super(Component.translatable("screen.wd.ars_melima"));
    }

    // Размеры текстуры
    static final int xSize = 512;
    static final int ySize = 512;

    // Области текстуры
    private static final int BG_U = 4, BG_V = 273, BG_W = 305, BG_H = 184;
    private static final int FG_U = 8, FG_V = 12, FG_W = 297, FG_H = 185;

    // Закладка
    private static final int BOOKMARK_U = 208, BOOKMARK_V = 227, BOOKMARK_W = 49, BOOKMARK_H = 20;
    private static final int BOOKMARK_ANCHOR_X = BOOKMARK_W, BOOKMARK_ANCHOR_Y = 0;
    private static final int FG_ANCHOR_X = 0, FG_ANCHOR_Y = 25;

    private final ArsMelimaMenu menu = new ArsMelimaMenu();

    // Левая область
    private static final int CONTENT_X1 = 8, CONTENT_Y1 = 20, CONTENT_X2 = 137, CONTENT_Y2 = 160;
    private int contentLeft, contentTop, contentWidth, contentHeight;

    // Правая область
    private int rightContentLeft, rightContentTop, rightContentWidth, rightContentHeight;

    // Стрелка назад (верхняя)
    private static final int BACK_ARROW_U = 128, BACK_ARROW_V = 226, BACK_ARROW_W = 32, BACK_ARROW_H = 18;
    private static final int BACK_ARROW_REL_X = 102, BACK_ARROW_REL_Y = 154;
    private static final int BACK_ARROW_PADDING = 2;

    // Стрелки листания (нижние, слева/справа)
    private static final int NAV_LEFT_REL_X = 10, NAV_RIGHT_REL_X = 253, NAV_REL_Y = 154;

    private int guiLeft = 0;
    private int guiTop = 0;

    // текущая страница (страница = разворот из 2 колонок)
    private int currentPage = 0;

    // единый scale и padding — используем везде
    private static final float TEXT_SCALE = 0.85f;
    private static final int CONTENT_PADDING = 4;

    @Override
    protected void init() {
        super.init();
        List<Chapter> chapters = ChapterLoader.loadChapters();
        menu.setChapters(chapters);
        menu.setCurrentIndex(-1);
        currentPage = 0;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        // Центрируем GUI
        guiLeft = (this.width - FG_W) / 2;
        guiTop = (this.height - FG_H) / 2;

        // Левая колонка
        contentLeft = guiLeft + CONTENT_X1;
        contentTop = guiTop + CONTENT_Y1;
        contentWidth = CONTENT_X2 - CONTENT_X1;
        contentHeight = CONTENT_Y2 - CONTENT_Y1;

        // Правая колонка
        rightContentLeft = guiLeft + 159;
        rightContentTop = guiTop + 20;
        rightContentWidth = 298 - 170;
        rightContentHeight = 174 - 34;

        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Фон и передний план
        int bgLeft = guiLeft + (FG_W - BG_W) / 2;
        int bgTop = guiTop + (FG_H - BG_H) / 2;
        graphics.blit(TEXTURE, bgLeft, bgTop, BG_U, BG_V, BG_W, BG_H, xSize, ySize);
        graphics.blit(TEXTURE, guiLeft, guiTop, FG_U, FG_V, FG_W, FG_H, xSize, ySize);

        // Закладка
        int bookmarkLeft = guiLeft + FG_ANCHOR_X - BOOKMARK_ANCHOR_X;
        int bookmarkTop = guiTop + FG_ANCHOR_Y - BOOKMARK_ANCHOR_Y;
        RenderSystem.setShaderColor(1.0F, 0.0F, 0.0F, 1.0F);
        graphics.blit(TEXTURE, bookmarkLeft, bookmarkTop, BOOKMARK_U, BOOKMARK_V, BOOKMARK_W, BOOKMARK_H, xSize, ySize);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        ArsMelimaDraws.renderItem(graphics, Items.APPLE.getDefaultInstance(), bookmarkLeft + 4, bookmarkTop);

        // Фон областей
        ArsMelimaDraws.drawAreaBackground(graphics, contentLeft, contentTop, contentWidth, contentHeight);
        ArsMelimaDraws.drawAreaBackground(graphics, rightContentLeft, rightContentTop, rightContentWidth, rightContentHeight);

        // Основная стрелка назад (верхняя) — закрывает главу
        if (menu.getCurrentIndex() != -1) {
            boolean hover = isPointInRectInclusive(
                    guiLeft + BACK_ARROW_REL_X - BACK_ARROW_PADDING,
                    guiTop + BACK_ARROW_REL_Y - BACK_ARROW_PADDING,
                    BACK_ARROW_W + BACK_ARROW_PADDING * 2,
                    BACK_ARROW_H + BACK_ARROW_PADDING * 2,
                    mouseX, mouseY
            );
            ArsMelimaDraws.drawBackChaptersArrow(graphics, TEXTURE, guiLeft, guiTop,
                    BACK_ARROW_U, BACK_ARROW_V, BACK_ARROW_W, BACK_ARROW_H,
                    xSize, ySize, hover, BACK_ARROW_REL_X, BACK_ARROW_REL_Y);
        }

        // --------- Вычисляем количество страниц корректно, используя TEXT_SCALE и CONTENT_PADDING
        Chapter current = menu.getCurrentChapter();
        int pageCount = 0;
        if (current != null) {
            int colWidth = Math.max(1, contentWidth - CONTENT_PADDING * 2);
            int colHeight = Math.max(1, contentHeight - CONTENT_PADDING * 2);
            List<Integer> starts = computePageStarts(current, this.font, TEXT_SCALE, colWidth, colHeight);
            pageCount = Math.max(1, starts.size());
            if (currentPage >= pageCount) currentPage = pageCount - 1;
        } else {
            currentPage = 0;
        }

        // --------- Навигационные стрелки (нижние)
        if (menu.getCurrentIndex() != -1 && pageCount > 1) {
            int leftNavX = guiLeft + NAV_LEFT_REL_X;
            int rightNavX = guiLeft + NAV_RIGHT_REL_X;
            int navY = guiTop + NAV_REL_Y;

            boolean hoverLeft = isPointInRectInclusive(leftNavX, navY, BACK_ARROW_W, BACK_ARROW_H, mouseX, mouseY);
            boolean hoverRight = isPointInRectInclusive(rightNavX, navY, BACK_ARROW_W, BACK_ARROW_H, mouseX, mouseY);

            // Левая стрелка — рисуем только если можно листать назад
            if (currentPage > 0) {
                ArsMelimaDraws.drawDimBackArrow(graphics, TEXTURE, guiLeft, guiTop, 128, 208, 32, 18, xSize, ySize, NAV_LEFT_REL_X, NAV_REL_Y);
                if (hoverLeft)
                    ArsMelimaDraws.drawBackArrow(graphics, TEXTURE, guiLeft, guiTop, 128, 226, 32, 18, xSize, ySize, true, NAV_LEFT_REL_X, NAV_REL_Y);
            }

            // Правая стрелка — рисуем только если можно листать вперед
            if (currentPage < pageCount - 1) {
                ArsMelimaDraws.drawDimForwardArrow(graphics, TEXTURE, guiLeft, guiTop, 160, 208, 32, 18, xSize, ySize, NAV_RIGHT_REL_X, NAV_REL_Y);
                if (hoverRight)
                    ArsMelimaDraws.drawForwardArrow(graphics, TEXTURE, guiLeft, guiTop, 160, 227, 32, 18, xSize, ySize, true, NAV_RIGHT_REL_X, NAV_REL_Y);
            }
        }

        // --------- Список или страница главы
        if (menu.getCurrentIndex() == -1) {
            renderChapterList(graphics, mouseX, mouseY);
        } else {
            renderChapterPage(graphics, mouseX, mouseY, current, currentPage);
        }

        RenderSystem.disableBlend();
        super.render(graphics, mouseX, mouseY, partialTick);
    }


    private void renderChapterList(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = contentLeft + 4;
        int y = contentTop + 4;
        int lineHeight = 12;
        List<Chapter> chapters = menu.getChapters();
        if (chapters.isEmpty()) {
            ArsMelimaDraws.drawScaledText(graphics, this.font, Component.translatable("screen.wd.ars_melima.no_chapters"), x, y, 0xFF222222, TEXT_SCALE);
            return;
        }
        for (int i = 0; i < chapters.size(); i++) {
            Chapter c = chapters.get(i);
            int ty = y + i * lineHeight;
            boolean hover = isPointInRect(contentLeft + 2, ty - 2, contentWidth - 4, lineHeight + 2, mouseX, mouseY);
            int color = hover ? 0xFFD4B400 : 0xFF222222;
            ArsMelimaDraws.drawScaledText(graphics, this.font, c.getTitle(), x, ty, color, TEXT_SCALE);
        }
    }

    /**
     * Рендерит страницу главы, где каждая глава — список ChapterElement (TEXT / IMAGE).
     * Каждый текст разбивается на строки, каждая картинка занимает несколько "строк" по высоте.
     */
    private void renderChapterPage(GuiGraphics graphics, int mouseX, int mouseY, Chapter chapter, int page) {
        if (chapter == null) return;

        float scale = TEXT_SCALE;
        int padding = CONTENT_PADDING;

        Column[] cols = {
                new Column(contentLeft + padding, contentTop + padding, contentWidth - padding * 2, contentHeight - padding * 2),
                new Column(rightContentLeft + padding, rightContentTop + padding, rightContentWidth - padding * 2, rightContentHeight - padding * 2)
        };

        // Собираем "юниты" (каждая текстовая строка — отдельный юнит; каждая картинка — юнит с несколькими строками)
        List<RenderUnit> units = buildRenderUnits(chapter, this.font, scale, cols[0].width, cols[0].height);

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
                RenderUnit u = units.get(unitIdx);

                if (u.type == RenderUnit.Type.TEXT) {
                    // рисуем одну строку
                    ArsMelimaDraws.drawScaledText(graphics, this.font, u.line, cx, cy, 0xFF000000, scale);
                    cy += linePix;
                    used++;
                    unitIdx++;
                } else { // IMAGE
                    // вычисляем реальную высоту картинки в пикселях
                    int imgW = Math.min(100, cols[col].width);
                    int desiredImgH = Math.min(60, cols[col].height / 2);
                    int imgH = Math.max(1, desiredImgH); // px
                    // центрируем картинку по горизонтали внутри колонки
                    int dx = cx + (cols[col].width - imgW) / 2;
                    // если картинка не помещается в оставшееся пространство по высоте — перейдём на следующую колонку
                    int rowsNeeded = Math.max(1, (int) Math.ceil((double) imgH / linePix));
                    if (rowsNeeded > (cap - used)) {
                        // переложим картинку в следующую колонку/страницу
                        break;
                    }

                    // рисуем фон (placeholder)
                    graphics.fill(dx, cy, dx + imgW, cy + imgH, 0xFFDDDDDD);
                    // рисуем саму картинку
                    try {
                        RenderSystem.setShaderTexture(0, new ResourceLocation(u.imageResource));
                        graphics.blit(new ResourceLocation(u.imageResource), dx, cy, 0, 0, imgW, imgH, imgW, imgH);
                        RenderSystem.setShaderTexture(0, TEXTURE);
                    } catch (Exception e) {
                        // если текстура не найдена — оставим только фон
                        RenderSystem.setShaderTexture(0, TEXTURE);
                    }

                    cy += rowsNeeded * linePix;
                    used += rowsNeeded;
                    unitIdx++;
                }
            }
        }
    }

    /**
     * Построение списка render-юнитов из Chapter: каждая текстовая строка — отдельный юнит;
     * каждое изображение — один юнит с запасом rows (высота в строках).
     */
    private List<RenderUnit> buildRenderUnits(Chapter chapter, Font font, float scale, int colWidth, int colHeight) {
        List<RenderUnit> units = new ArrayList<>();
        if (chapter == null) return units;

        int linePix = Math.max(1, (int) (10 * scale));
        int normalCap = Math.max(1, colHeight / linePix);

        for (ChapterElement el : chapter.getElements()) {
            if (el == null) continue;
            if (el.getType() == ChapterElement.Type.TEXT) {
                String txt = el.getData() == null ? "" : el.getData();
                List<FormattedCharSequence> lines = font.split(Component.literal(txt), (int) (colWidth / scale));
                for (FormattedCharSequence l : lines) {
                    RenderUnit ru = new RenderUnit();
                    ru.type = RenderUnit.Type.TEXT;
                    ru.line = l;
                    ru.rows = 1;
                    units.add(ru);
                }
            } else if (el.getType() == ChapterElement.Type.IMAGE) {
                String res = el.getData();
                // высота картинки в пикселях — используем разумные ограничения (как ранее)
                int imgH = Math.min(60, colHeight / 2);
                int rows = Math.max(1, (int) Math.ceil((double) imgH / linePix));
                // не даём картинке быть больше колонки
                rows = Math.min(rows, normalCap);
                RenderUnit ru = new RenderUnit();
                ru.type = RenderUnit.Type.IMAGE;
                ru.imageResource = res == null ? "" : res;
                ru.rows = rows;
                units.add(ru);
            }
        }

        // Если вообще нет юнитов — добавим пустой текстовый юнит, чтобы страница была
        if (units.isEmpty()) {
            RenderUnit ru = new RenderUnit();
            ru.type = RenderUnit.Type.TEXT;
            ru.line = Component.literal("").getString() == null ? null : Component.literal("").getVisualOrderText(); // пустая строка
            // but safer:
            ru.line = Component.literal("").getVisualOrderText();
            ru.rows = 1;
            units.add(ru);
        }

        return units;
    }

    /**
     * На основе списка юнитов вычисляем стартовые индексы для каждой страницы (каждая страница = 2 колонки).
     * Правила:
     *  - текстовые юниты занимают 1 строки
     *  - image-юниты занимают ru.rows строк и НЕ дробятся: если не помещаются в остатке колонки — переносим на следующую колонку
     */
    private List<Integer> computePageStartsFromUnits(List<RenderUnit> units, int colWidth, int colHeight, float scale) {
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

            // первая колонка (для первой колонки страницы — никаких особых ограничений у нас больше нет)
            int remaining = normalCap;
            while (remaining > 0 && idx < units.size()) {
                RenderUnit u = units.get(idx);
                if (u.type == RenderUnit.Type.TEXT) {
                    // занимает 1 строку
                    remaining -= 1;
                    idx += 1;
                } else {
                    // image: если не помещается — переносим на следующую колонку
                    int need = Math.max(1, u.rows);
                    if (need > remaining) break;
                    remaining -= need;
                    idx += 1;
                }
            }

            // вторая колонка
            remaining = normalCap;
            while (remaining > 0 && idx < units.size()) {
                RenderUnit u = units.get(idx);
                if (u.type == RenderUnit.Type.TEXT) {
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

    /**
     * Старый computePageStarts — теперь просто адаптируем вызовы к новым методам.
     * Оставлен для совместимости вызовов в коде (mouseClicked и т.д.)
     */
    private List<Integer> computePageStarts(Chapter chapter, Font font, float scale, int colWidth, int colHeight) {
        List<RenderUnit> units = buildRenderUnits(chapter, font, scale, colWidth, colHeight);
        return computePageStartsFromUnits(units, colWidth, colHeight, scale);
    }

    // Вспомогательные структуры
    private static class Column {
        final int left, top, width, height;
        Column(int left, int top, int width, int height) { this.left = left; this.top = top; this.width = width; this.height = height; }
    }

    private static class RenderUnit {
        enum Type { TEXT, IMAGE }
        Type type;
        // если TEXT
        FormattedCharSequence line;
        // если IMAGE
        String imageResource;
        // сколько "строк" занимает (1 строка = linePix пикселей)
        int rows;
    }

    private boolean isPointInRect(int rx, int ry, int rw, int rh, int px, int py) {
        return px >= rx && py >= ry && px < rx + rw && py < ry + rh;
    }

    private boolean isPointInRectInclusive(int rx, int ry, int rw, int rh, int px, int py) {
        return px >= rx && py >= ry && px <= rx + rw - 1 && py <= ry + rh - 1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) Math.floor(mouseX);
        int my = (int) Math.floor(mouseY);

        if (menu.getCurrentIndex() != -1) {
            // Верхняя стрелка "назад"
            int bx = guiLeft + BACK_ARROW_REL_X - BACK_ARROW_PADDING;
            int by = guiTop + BACK_ARROW_REL_Y - BACK_ARROW_PADDING;
            int bw = BACK_ARROW_W + BACK_ARROW_PADDING * 2;
            int bh = BACK_ARROW_H + BACK_ARROW_PADDING * 2;
            if (isPointInRectInclusive(bx, by, bw, bh, mx, my) && button == 0) {
                menu.closeChapter();
                currentPage = 0;
                return true;
            }

            // Навигация по страницам
            Chapter current = menu.getCurrentChapter();
            int colWidth = Math.max(1, contentWidth - CONTENT_PADDING * 2);
            int colHeight = Math.max(1, contentHeight - CONTENT_PADDING * 2);
            int pageCount = computePageStarts(current, this.font, TEXT_SCALE, colWidth, colHeight).size();

            int leftNavX = guiLeft + NAV_LEFT_REL_X;
            int rightNavX = guiLeft + NAV_RIGHT_REL_X;
            int navY = guiTop + NAV_REL_Y;

            if (isPointInRectInclusive(leftNavX, navY, BACK_ARROW_W, BACK_ARROW_H, mx, my) && button == 0) {
                if (currentPage > 0) {
                    currentPage--;
                    return true;
                }
            }
            if (isPointInRectInclusive(rightNavX, navY, BACK_ARROW_W, BACK_ARROW_H, mx, my) && button == 0) {
                if (currentPage < pageCount - 1) {
                    currentPage++;
                    return true;
                }
            }
        }

        // Если в списке глав
        if (menu.getCurrentIndex() == -1) {
            if (isPointInRect(contentLeft, contentTop, contentWidth, contentHeight, mx, my)) {
                int idx = (my - (contentTop + 4)) / 12;
                if (idx >= 0 && idx < menu.getChapters().size()) {
                    menu.openChapter(idx);
                    currentPage = 0;
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
