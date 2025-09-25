package ru.imaginaerum.wd.client.gui.ars_melima;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Items;

import java.util.List;

public class ArsMelimaScreen extends Screen {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("wd", "textures/gui/ars_melima/ars_melima.png");

    public ArsMelimaScreen() {
        super(Component.translatable("screen.wd.ars_melima"));
    }

    // Общий размер текстуры
    static final int xSize = 512;
    static final int ySize = 512;

    // размеры областей в текстуре
    private static final int BG_U = 4, BG_V = 273, BG_W = 305, BG_H = 184;
    private static final int FG_U = 8, FG_V = 12, FG_W = 297, FG_H = 185;

    // закладка
    private static final int BOOKMARK_U = 208;
    private static final int BOOKMARK_V = 227;
    private static final int BOOKMARK_W = 49;
    private static final int BOOKMARK_H = 20;

    private static final int BOOKMARK_ANCHOR_X = BOOKMARK_W;
    private static final int BOOKMARK_ANCHOR_Y = 0;

    private static final int FG_ANCHOR_X = 0;
    private static final int FG_ANCHOR_Y = 25;

    private final ArsMelimaMenu menu = new ArsMelimaMenu();

    // Левая область
    private static final int CONTENT_X1 = 8;
    private static final int CONTENT_Y1 = 20;
    private static final int CONTENT_X2 = 137;
    private static final int CONTENT_Y2 = 160;

    private int contentLeft, contentTop, contentWidth, contentHeight;

    // Правая область
    private int rightContentLeft, rightContentTop, rightContentWidth, rightContentHeight;

    // --- Константы для "стрелки назад" (стрелка взята из текстуры)
    // координаты внутри текстуры (U,V) — это диапазон X128..161, Y226..245
    private static final int BACK_ARROW_U = 128;
    private static final int BACK_ARROW_V = 226;
    private static final int BACK_ARROW_W = 33;  // 161 - 128
    private static final int BACK_ARROW_H = 19;  // 245 - 226

    // желаемая позиция стрелки ВНУТРИ GUI (относительно guiLeft, guiTop)
    private static final int BACK_ARROW_REL_X = 102;
    private static final int BACK_ARROW_REL_Y = 154;

    // хитбокс расширим для удобства
    private static final int BACK_ARROW_PADDING = 2;
    // --- конец констант

    // сохраняем текущее положение GUI для вычисления относительных координат при кликах
    private int guiLeft = 0;
    private int guiTop = 0;

    @Override
    protected void init() {
        super.init();
        List<Chapter> chapters = ChapterLoader.loadChapters();
        menu.setChapters(chapters);
        menu.setCurrentIndex(-1);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        // top-left GUI (центрируем как обычно)
        int left = (this.width - FG_W) / 2;
        int top = (this.height - FG_H) / 2;

        // сохраняем для mouseClicked
        this.guiLeft = left;
        this.guiTop = top;

        // Левая колонка
        contentLeft = left + CONTENT_X1;
        contentTop = top + CONTENT_Y1;
        contentWidth = CONTENT_X2 - CONTENT_X1;
        contentHeight = CONTENT_Y2 - CONTENT_Y1;

        // Правая колонка
        rightContentLeft = left + 159;
        rightContentTop = top + 20;
        rightContentWidth = 298 - 170;
        rightContentHeight = 174 - 34;

        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // фон
        int bgLeft = left + (FG_W - BG_W) / 2;
        int bgTop = top + (FG_H - BG_H) / 2;
        graphics.blit(TEXTURE, bgLeft, bgTop, BG_U, BG_V, BG_W, BG_H, xSize, ySize);

        // передний план
        graphics.blit(TEXTURE, left, top, FG_U, FG_V, FG_W, FG_H, xSize, ySize);

        // закладка (как было)
        int fgAnchorX = left + FG_ANCHOR_X;
        int fgAnchorY = top + FG_ANCHOR_Y;
        int bookmarkLeft = fgAnchorX - BOOKMARK_ANCHOR_X;
        int bookmarkTop = fgAnchorY - BOOKMARK_ANCHOR_Y;

        RenderSystem.setShaderColor(1.0F, 0.0F, 0.0F, 1.0F);
        graphics.blit(TEXTURE, bookmarkLeft, bookmarkTop,
                BOOKMARK_U, BOOKMARK_V, BOOKMARK_W, BOOKMARK_H, xSize, ySize);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        graphics.renderItem(Items.APPLE.getDefaultInstance(), bookmarkLeft + 4, bookmarkTop);

        // рамки и фон для колонок
        drawAreaBackground(graphics, contentLeft, contentTop, contentWidth, contentHeight);
        drawAreaBackground(graphics, rightContentLeft, rightContentTop, rightContentWidth, rightContentHeight);

        // Отрисовка стрелки назад (только если открыта глава)
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        if (menu.getCurrentIndex() != -1) {
            int arrowX = left + BACK_ARROW_REL_X;
            int arrowY = top + BACK_ARROW_REL_Y;
            int bx = arrowX - BACK_ARROW_PADDING;
            int by = arrowY - BACK_ARROW_PADDING;
            int bw = BACK_ARROW_W + BACK_ARROW_PADDING * 2;
            int bh = BACK_ARROW_H + BACK_ARROW_PADDING * 2;
            // mouseX/mouseY в render — int, поэтому используем их напрямую
            boolean hover = isPointInRectInclusive(bx, by, bw, bh, mouseX, mouseY);

            drawBackArrow(graphics, left, top, hover);
        }

        if (menu.getCurrentIndex() == -1) {
            renderChapterList(graphics, mouseX, mouseY);
        } else {
            renderChapterPage(graphics, mouseX, mouseY, menu.getCurrentChapter());
        }

        RenderSystem.disableBlend();
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    // рисуем стрелку из текстуры в позицию (guiLeft + BACK_ARROW_REL_X, guiTop + BACK_ARROW_REL_Y)
    // alpha: 0.7 по умолчанию, 1.0 при hover
    private void drawBackArrow(GuiGraphics graphics, int guiLeft, int guiTop, boolean hover) {
        int destX = guiLeft + BACK_ARROW_REL_X;
        int destY = guiTop + BACK_ARROW_REL_Y;

        // убедимся, что блендинг включён и используется стандартная функция
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        float alpha = hover ? 1.0f : 0.7f;
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);

        // shader texture уже установлен в render перед вызовом drawBackArrow
        graphics.blit(TEXTURE,
                destX, destY,
                BACK_ARROW_U, BACK_ARROW_V,
                BACK_ARROW_W, BACK_ARROW_H,
                xSize, ySize);

        // Сброс цвета и blend (чтобы не повлиять на остальную отрисовку)
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        // Не отключаем blend полностью — остальная часть UI может полагаться на него,
        // но если хочешь, можно вызвать RenderSystem.disableBlend() здесь.
    }

    private void drawAreaBackground(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
        g.fill(x, y, x + w, y + h, 0xFFEEEEDD);
    }

    private void renderChapterList(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = contentLeft + 4;
        int y = contentTop + 4;
        int lineHeight = 12;
        List<Chapter> chapters = menu.getChapters();

        if (chapters.isEmpty()) {
            drawScaledText(graphics, "Нет глав (положите json в lang/ars_melima)",
                    x, y, 0xFF222222, 0.85f);
            return;
        }

        for (int i = 0; i < chapters.size(); i++) {
            Chapter c = chapters.get(i);
            String title = c.getTitle();
            int ty = y + i * lineHeight;
            boolean hover = isPointInRect(contentLeft + 2, ty - 2, contentWidth - 4, lineHeight + 2, mouseX, mouseY);
            int color = hover ? 0xFFD4B400 : 0xFF222222;
            drawScaledText(graphics, title, x, ty, color, 0.85f);
        }
    }

    private void renderChapterPage(GuiGraphics graphics, int mouseX, int mouseY, Chapter chapter) {
        if (chapter == null) return;

        float scale = 0.85f;
        int padding = 4;

        Column[] cols = {
                new Column(contentLeft + padding, contentTop + padding,
                        contentWidth - padding * 2, contentHeight - padding * 2),
                new Column(rightContentLeft + padding, rightContentTop + padding,
                        rightContentWidth - padding * 2, rightContentHeight - padding * 2)
        };

        int col = 0;
        int y = cols[col].top;

        // картинка
        if (chapter.getImageResource() != null && !chapter.getImageResource().isEmpty()) {
            ResourceLocation rl = new ResourceLocation(chapter.getImageResource());
            int imgW = Math.min(100, cols[col].width);
            int imgH = Math.min(60, cols[col].height / 2);

            if (y + imgH > cols[col].top + cols[col].height) {
                col++;
                if (col >= cols.length) return;
                y = cols[col].top;
            }

            int cx = cols[col].left + (cols[col].width - imgW) / 2;
            graphics.fill(cx, y, cx + imgW, y + imgH, 0xFFDDDDDD);
            RenderSystem.setShaderTexture(0, rl);
            graphics.blit(rl, cx, y, 0, 0, imgW, imgH, imgW, imgH);

            y += imgH + 8;
        }

        // текст
        int scaledW = (int)(cols[col].width / scale);
        List<FormattedCharSequence> lines =
                this.font.split(Component.literal(chapter.getContent()), scaledW);

        for (FormattedCharSequence fs : lines) {
            if (y > cols[col].top + cols[col].height - 10) {
                col++;
                if (col >= cols.length) break;
                y = cols[col].top;
            }
            drawScaledText(graphics, fs, cols[col].left, y, 0xFF000000, scale);
            y += (int)(10 * scale);
        }
    }

    // вспомогательный контейнер для колонок
    private static class Column {
        final int left, top, width, height;
        Column(int left, int top, int width, int height) {
            this.left = left; this.top = top; this.width = width; this.height = height;
        }
    }

    private void drawScaledText(GuiGraphics graphics, String text, int x, int y, int color, float scale) {
        drawScaledText(graphics, Component.literal(text), x, y, color, scale);
    }

    private void drawScaledText(GuiGraphics graphics, Component text, int x, int y, int color, float scale) {
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(x, y, 0);
        poseStack.scale(scale, scale, 1.0f);
        graphics.drawString(this.font, text, 0, 0, color, false);
        poseStack.popPose();
    }

    private void drawScaledText(GuiGraphics graphics, FormattedCharSequence text, int x, int y, int color, float scale) {
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(x, y, 0);
        poseStack.scale(scale, scale, 1.0f);
        graphics.drawString(this.font, text, 0, 0, color, false);
        poseStack.popPose();
    }

    // обычная проверка (левая-верхняя включительно, правая/нижняя — исключаются)
    private boolean isPointInRect(int rx, int ry, int rw, int rh, int px, int py) {
        return px >= rx && py >= ry && px < rx + rw && py < ry + rh;
    }

    // включающая проверка (включая правую/нижнюю границу) — удобно для хитбокса
    private boolean isPointInRectInclusive(int rx, int ry, int rw, int rh, int px, int py) {
        return px >= rx && py >= ry && px <= rx + rw - 1 && py <= ry + rh - 1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) Math.floor(mouseX);
        int my = (int) Math.floor(mouseY);

        // 1) Обрабатываем стрелку назад первой (вся прямоугольная область + padding)
        if (menu.getCurrentIndex() != -1) {
            int arrowX = guiLeft + BACK_ARROW_REL_X;
            int arrowY = guiTop + BACK_ARROW_REL_Y;
            int bx = arrowX - BACK_ARROW_PADDING;
            int by = arrowY - BACK_ARROW_PADDING;
            int bw = BACK_ARROW_W + BACK_ARROW_PADDING * 2;
            int bh = BACK_ARROW_H + BACK_ARROW_PADDING * 2;

            if (isPointInRectInclusive(bx, by, bw, bh, mx, my)) {
                // только левый клик закрывает главу (возврат к списку)
                if (button == 0) {
                    menu.closeChapter();
                    return true;
                } else {
                    return false;
                }
            }
        }

        // 2) Остальная логика — клики по списку и т.д.
        if (!isPointInRect(contentLeft, contentTop, contentWidth, contentHeight, mx, my)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (menu.getCurrentIndex() == -1) {
            int relY = my - (contentTop + 4);
            int lineHeight = 12;
            int idx = relY / lineHeight;
            if (idx >= 0 && idx < menu.getChapters().size()) {
                menu.openChapter(idx);
                return true;
            }
        } else {
            // ПКМ больше не закрывает страницу — закрытие только через стрелку
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
