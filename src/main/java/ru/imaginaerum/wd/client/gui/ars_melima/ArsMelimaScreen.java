package ru.imaginaerum.wd.client.gui.ars_melima;


import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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

    // Стрелка назад (верхняя)
    private static final int BACK_ARROW_W = 32, BACK_ARROW_H = 18;
    private static final int BACK_ARROW_REL_X = 132, BACK_ARROW_REL_Y = 174;
    private static final int BACK_ARROW_PADDING = 2;
    // Добавьте эти константы для размеров стрелок листания
    private static final int NAV_ARROW_W = 12;
    private static final int NAV_ARROW_H = 7;
    // Стрелки листания (нижние, слева/справа)
    private static final int NAV_LEFT_REL_X = 10, NAV_RIGHT_REL_X = 276, NAV_REL_Y = 184;

    private int guiLeft = 0;
    private int guiTop = 0;

    // текущая страница (страница = разворот из 2 колонок)
    private int currentPage = 0;

    // единый scale и padding — используем везде
    private static final float TEXT_SCALE = 0.85f;
    static final int CONTENT_PADDING = 4;

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
        // делегируем всю логику рендеринга UI в тот же метод (с сохранением сигнатуры)
        // но поскольку некоторые поля в экране используются/обновляются — часть логики остаётся здесь:

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
                    guiLeft + 142 - 2,
                    guiTop + 186 - 2,
                    11 + 4,
                    11 + 4,
                    mouseX, mouseY
            );

            if (hover) {
                ArsMelimaDraws.drawBackArrow(graphics, TEXTURE, guiLeft, guiTop,
                        177, 233, 11, 11,
                        xSize, ySize, true, 142, 186);
            } else {
                ArsMelimaDraws.drawDimBackArrow(graphics, TEXTURE, guiLeft, guiTop,
                        177, 221, 11, 11,
                        xSize, ySize, 142, 186);
            }
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

            boolean hoverLeft = isPointInRectInclusive(leftNavX, navY, NAV_ARROW_W, NAV_ARROW_H, mouseX, mouseY);
            boolean hoverRight = isPointInRectInclusive(rightNavX, navY, NAV_ARROW_W, NAV_ARROW_H, mouseX, mouseY);

            if (currentPage > 0) {
                ArsMelimaDraws.drawDimBackArrow(graphics, TEXTURE, guiLeft, guiTop, 151, 229, 12, 7, xSize, ySize, NAV_LEFT_REL_X, NAV_REL_Y);
                if (hoverLeft)
                    ArsMelimaDraws.drawBackArrow(graphics, TEXTURE, guiLeft, guiTop, 151, 237, 12, 7, xSize, ySize, true, NAV_LEFT_REL_X, NAV_REL_Y);
            }

            if (currentPage < pageCount - 1) {
                ArsMelimaDraws.drawDimForwardArrow(graphics, TEXTURE, guiLeft, guiTop, 164, 229, 12, 7, xSize, ySize, NAV_RIGHT_REL_X, NAV_REL_Y);
                if (hoverRight)
                    ArsMelimaDraws.drawForwardArrow(graphics, TEXTURE, guiLeft, guiTop, 164, 237, 12, 7, xSize, ySize, true, NAV_RIGHT_REL_X, NAV_REL_Y);
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
        ArsMelimaRenderer.renderChapterList(graphics, mouseX, mouseY,
                contentLeft, contentTop, contentWidth, contentHeight,
                this.font, this.menu, TEXT_SCALE);
    }

    private void renderChapterPage(GuiGraphics graphics, int mouseX, int mouseY, Chapter chapter, int page) {
        ArsMelimaRenderer.renderChapterPage(graphics, mouseX, mouseY, chapter, page,
                contentLeft, contentTop, contentWidth, contentHeight,
                rightContentLeft, rightContentTop, rightContentWidth, rightContentHeight,
                this.font, TEXT_SCALE, TEXTURE);
    }

    private List<RenderUnit> buildRenderUnits(Chapter chapter, Font font, float scale, int colWidth, int colHeight) {
        return ArsMelimaRenderer.buildRenderUnits(chapter, font, scale, colWidth, colHeight);
    }

    private List<Integer> computePageStartsFromUnits(List<RenderUnit> units, int colWidth, int colHeight, float scale) {
        return ArsMelimaRenderer.computePageStartsFromUnits(units, colWidth, colHeight, scale);
    }

    private List<Integer> computePageStarts(Chapter chapter, Font font, float scale, int colWidth, int colHeight) {
        return ArsMelimaRenderer.computePageStarts(chapter, font, scale, colWidth, colHeight);
    }

    // Вспомогательные структуры — сделали package-private, чтобы ArsMelimaRenderer мог их использовать
    static class Column {
        final int left, top, width, height;
        Column(int left, int top, int width, int height) { this.left = left; this.top = top; this.width = width; this.height = height; }
    }

    static class RenderUnit {
        enum Type { TEXT, IMAGE }
        Type type;
        FormattedCharSequence line;
        String imageResource;
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
            int bx = guiLeft + BACK_ARROW_REL_X - BACK_ARROW_PADDING;
            int by = guiTop + BACK_ARROW_REL_Y - BACK_ARROW_PADDING;
            int bw = BACK_ARROW_W + BACK_ARROW_PADDING * 2;
            int bh = BACK_ARROW_H + BACK_ARROW_PADDING * 2;
            if (isPointInRectInclusive(bx, by, bw, bh, mx, my) && button == 0) {
                menu.closeChapter();
                currentPage = 0;
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.playSound(
                            net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN,
                            1.0F,
                            1.0F
                    );
                }
                return true;
            }

            Chapter current = menu.getCurrentChapter();
            int colWidth = Math.max(1, contentWidth - CONTENT_PADDING * 2);
            int colHeight = Math.max(1, contentHeight - CONTENT_PADDING * 2);
            int pageCount = computePageStarts(current, this.font, TEXT_SCALE, colWidth, colHeight).size();

            int leftNavX = guiLeft + NAV_LEFT_REL_X;
            int rightNavX = guiLeft + NAV_RIGHT_REL_X;
            int navY = guiTop + NAV_REL_Y;
            //Размер стрелок листания
            int arrowWidth = 12;
            int arrowHeight = 7;
            if (isPointInRectInclusive(leftNavX, navY, arrowWidth, arrowHeight, mx, my) && button == 0) {
                if (currentPage > 0) {
                    currentPage--;
                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.playSound(
                                net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN,
                                1.0F,
                                1.0F
                        );
                    }
                    return true;
                }
            }
            if (isPointInRectInclusive(rightNavX, navY, arrowWidth, arrowHeight, mx, my) && button == 0) {
                if (currentPage < pageCount - 1) {
                    currentPage++;
                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.playSound(
                                net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN,
                                1.0F,
                                1.0F
                        );
                    }
                    return true;
                }
            }
        }

        if (menu.getCurrentIndex() == -1) {
            if (isPointInRect(contentLeft, contentTop, contentWidth, contentHeight, mx, my)) {
                int idx = (my - (contentTop + 4)) / 12;
                if (idx >= 0 && idx < menu.getChapters().size()) {
                    menu.openChapter(idx);
                    currentPage = 0;
                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.playSound(
                                net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN,
                                1.0F,
                                1.0F
                        );
                    }
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
