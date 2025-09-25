package ru.imaginaerum.wd.client.gui.ars_melima;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Items;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ArsMelimaDraws;

import java.util.List;

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

    // Стрелка назад
    private static final int BACK_ARROW_U = 128, BACK_ARROW_V = 226, BACK_ARROW_W = 32, BACK_ARROW_H = 18;
    private static final int BACK_ARROW_REL_X = 102, BACK_ARROW_REL_Y = 154;
    private static final int BACK_ARROW_PADDING = 2;

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

        // Основная стрелка назад
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

        // Потухшие стрелки
        ArsMelimaDraws.drawDimBackArrow(graphics, TEXTURE, guiLeft, guiTop, 128, 208, 32, 18, xSize, ySize, 10, 154);
        ArsMelimaDraws.drawDimForwardArrow(graphics, TEXTURE, guiLeft, guiTop, 160, 208, 32, 18, xSize, ySize, 253, 154);

        //Активные стрелки
        ArsMelimaDraws.drawForwardArrow(graphics, TEXTURE, guiLeft, guiTop, 160, 227, 32, 18, xSize, ySize, 253, 154);
        ArsMelimaDraws.drawBackArrow(graphics, TEXTURE, guiLeft, guiTop, 128, 226, 32, 18, xSize, ySize, 10, 154);

        // Список или страница главы
        if (menu.getCurrentIndex() == -1) {
            renderChapterList(graphics, mouseX, mouseY);
        } else {
            renderChapterPage(graphics, mouseX, mouseY, menu.getCurrentChapter());
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
            ArsMelimaDraws.drawScaledText(graphics, this.font, Component.translatable("screen.wd.ars_melima.no_chapters"), x, y, 0xFF222222, 0.85f);
            return;
        }
        for (int i = 0; i < chapters.size(); i++) {
            Chapter c = chapters.get(i);
            int ty = y + i * lineHeight;
            boolean hover = isPointInRect(contentLeft + 2, ty - 2, contentWidth - 4, lineHeight + 2, mouseX, mouseY);
            int color = hover ? 0xFFD4B400 : 0xFF222222;
            ArsMelimaDraws.drawScaledText(graphics, this.font, c.getTitle(), x, ty, color, 0.85f);
        }
    }

    private void renderChapterPage(GuiGraphics graphics, int mouseX, int mouseY, Chapter chapter) {
        if (chapter == null) return;

        float scale = 0.85f;
        int padding = 4;

        Column[] cols = {
                new Column(contentLeft + padding, contentTop + padding, contentWidth - padding * 2, contentHeight - padding * 2),
                new Column(rightContentLeft + padding, rightContentTop + padding, rightContentWidth - padding * 2, rightContentHeight - padding * 2)
        };

        int col = 0;
        int y = cols[col].top;

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

        int scaledW = (int)(cols[col].width / scale);
        List<FormattedCharSequence> lines = this.font.split(Component.literal(chapter.getContent()), scaledW);
        for (FormattedCharSequence fs : lines) {
            if (y > cols[col].top + cols[col].height - 10) {
                col++;
                if (col >= cols.length) break;
                y = cols[col].top;
            }
            ArsMelimaDraws.drawScaledText(graphics, this.font, fs, cols[col].left, y, 0xFF000000, scale);
            y += (int)(10 * scale);
        }
    }

    private static class Column {
        final int left, top, width, height;
        Column(int left, int top, int width, int height) { this.left = left; this.top = top; this.width = width; this.height = height; }
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
            int bx = guiLeft + BACK_ARROW_REL_X - BACK_ARROW_PADDING;
            int by = guiTop + BACK_ARROW_REL_Y - BACK_ARROW_PADDING;
            int bw = BACK_ARROW_W + BACK_ARROW_PADDING * 2;
            int bh = BACK_ARROW_H + BACK_ARROW_PADDING * 2;
            if (isPointInRectInclusive(bx, by, bw, bh, mx, my) && button == 0) {
                menu.closeChapter();
                return true;
            }
        }

        if (!isPointInRect(contentLeft, contentTop, contentWidth, contentHeight, mx, my))
            return super.mouseClicked(mouseX, mouseY, button);

        if (menu.getCurrentIndex() == -1) {
            int idx = (my - (contentTop + 4)) / 12;
            if (idx >= 0 && idx < menu.getChapters().size()) {
                menu.openChapter(idx);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
