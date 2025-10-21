package ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaMenu;
import ru.imaginaerum.wd.client.gui.ars_melima.progress_tree.ProgressNode;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ArsMelimaDraws;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ArsMelimaUIManager;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ClientCookingData;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager.tree_progress.DrawNodesLinks;

import java.util.*;

import static ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager.ArsMelimaConstants.ICONS_TEXTURE;

public class ProgressTreeRenderer {
    // Логический размер ноды (фиксированный)
    public static final int NODE_SIZE = 20;
    // Длина видимой линии между отступами
    public static final int LINE_LENGTH = 9;
    // gap (отступ между визуальной рамкой и линией) для состояний
    public static final int LINE_OFFSET_UNLOCKED = 2;
    public static final int LINE_OFFSET_LOCKED = 3;
    // реальные размеры фреймов (текстур)
    public static final int UNLOCKED_SIZE = 22;
    public static final int LOCKED_SIZE = 20;
    // размер углового квадратика (2x2)
    public static final int CORNER_SIZE = 2;

    public static void renderProgressTree(ArsMelimaUIManager manager, GuiGraphics graphics,
                                          int mouseX, int mouseY,
                                          ArsMelimaMenu menu,
                                          Font font) {
        List<ProgressNode> nodes = menu.getProgressNodes();
        if (nodes == null || nodes.isEmpty()) return;

        int startX = manager.getGuiLeft() + 7;
        int startY = manager.getGuiTop() + 25;

        Map<String, Point> positions = DrawNodesLinks.computePositions(nodes, startX, startY);
        manager.getNodePositionsStore().setPositions(positions);

        drawNodes(manager, graphics, positions, nodes, mouseX, mouseY, font);
        DrawNodesLinks.drawLinks(graphics, positions, nodes);
    }

    private static void drawNodes(ArsMelimaUIManager manager, GuiGraphics graphics,
                                  Map<String, Point> computed, List<ProgressNode> nodes,
                                  int mouseX, int mouseY, Font font) {
        final int TEX_W = 512, TEX_H = 512;
        final int UNLOCKED_U = 136, UNLOCKED_V = 0;
        final int LOCKED_U = 0, LOCKED_V = 61;

        for (ProgressNode node : nodes) {
            if (node == null) continue;
            Point pos = computed.get(node.getId());
            if (pos == null) continue;

            boolean unlocked = !node.isLocked() || ClientCookingData.isProgressUnlocked(node.getId());
            int frameSize = unlocked ? UNLOCKED_SIZE : LOCKED_SIZE;
            int frameU = unlocked ? UNLOCKED_U : LOCKED_U;
            int frameV = unlocked ? UNLOCKED_V : LOCKED_V;

            int delta = (frameSize - NODE_SIZE) / 2;
            int drawX = pos.x - delta;
            int drawY = pos.y - delta;

            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.setShaderTexture(0, ICONS_TEXTURE);
            graphics.blit(ICONS_TEXTURE, drawX, drawY, frameU, frameV, frameSize, frameSize, TEX_W, TEX_H);

            ItemStack stack = createItemStackFromResource(node.getItemResource());
            int itemX = drawX + (frameSize - 16) / 2;
            int itemY = drawY + (frameSize - 16) / 2;
            if (!stack.isEmpty()) {
                ArsMelimaDraws.renderItem(graphics, stack, itemX, itemY);
            }

            if (UIUtils.isPointInRect(drawX, drawY, frameSize, frameSize, mouseX, mouseY)) {
                graphics.fill(drawX, drawY, drawX + frameSize, drawY + frameSize, 0x80FFFFFF);
                Component tooltip = Component.literal(node.getDescription() != null ? node.getDescription() : "");
                graphics.renderTooltip(font, tooltip, mouseX, mouseY);
            }
        }
    }
    // Новая вертикальная линия (4x17), TEXTURE region X4 Y0
    public static void drawLineVertical17(GuiGraphics g, int x, int y) {
        RenderSystem.setShaderTexture(0, ICONS_TEXTURE);
        g.blit(ICONS_TEXTURE, x, y, 4, 17, 4, 0, 4, 17, 512, 512);
    }

    // Новая горизонтальная линия (17x4), TEXTURE region X10 Y0
    public static void drawLineHorizontal17(GuiGraphics g, int x, int y) {
        RenderSystem.setShaderTexture(0, ICONS_TEXTURE);
        g.blit(ICONS_TEXTURE, x, y, 17, 4, 10, 0, 17, 4, 512, 512);
    }
    // Угол новый вниз вправо
    public static void drawCornerDownRight(GuiGraphics g, int x, int y) {
        RenderSystem.setShaderTexture(0, ICONS_TEXTURE);
        g.blit(ICONS_TEXTURE, x, y, 17, 17, 28, 0, 17, 17, 512, 512);
    }
    // Угол новый вниз влево
    public static void drawCornerDownLeft(GuiGraphics g, int x, int y) {
        RenderSystem.setShaderTexture(0, ICONS_TEXTURE);
        g.blit(ICONS_TEXTURE, x, y, 17, 17, 45, 0, 17, 17, 512, 512);
    }
    // Угол новый влево вниз
    public static void drawCornerLeftDown(GuiGraphics g, int x, int y) {
        RenderSystem.setShaderTexture(0, ICONS_TEXTURE);
        g.blit(ICONS_TEXTURE, x, y, 17, 17, 64, 0, 17, 17, 512, 512);
    }
    // Угол новый вправо вниз
    public static void drawCornerRightDown(GuiGraphics g, int x, int y) {
        RenderSystem.setShaderTexture(0, ICONS_TEXTURE);
        g.blit(ICONS_TEXTURE, x, y, 17, 17, 82, 0, 17, 17, 512, 512);
    }


    // Простые методы отрисовки полосок (рисуют ровно переданную длину)
    public static void drawHorizontalStripTiled(GuiGraphics graphics, int x, int y, int width) {
        if (width <= 0) return;

        RenderSystem.setShaderTexture(0, ICONS_TEXTURE);
        int tileW = 9, texU = 12, texV = 5, texH = 2;
        int drawn = 0;

        while (drawn < width) {
            int take = Math.min(tileW, width - drawn);
            graphics.blit(ICONS_TEXTURE, x + drawn, y, take, texH, texU, texV, take, texH, 512, 512);
            drawn += take;
        }
    }
    // Отрисовка углового квадратика 2x2 (текстура U=1,V=16)
    public static void drawCornerSquare(GuiGraphics graphics, int x, int y) {
        RenderSystem.setShaderTexture(0, ICONS_TEXTURE);
        int texU = 1, texV = 16, size = CORNER_SIZE;
        graphics.blit(ICONS_TEXTURE, x, y, size, size, texU, texV, size, size, 512, 512);
    }
    public static void drawVerticalStripTiled(GuiGraphics graphics, int x, int y, int height) {
        if (height <= 0) return;

        RenderSystem.setShaderTexture(0, ICONS_TEXTURE);
        int tileH = 9, texU = 1, texV = 2, texW = 2;
        int drawn = 0;

        while (drawn < height) {
            int take = Math.min(tileH, height - drawn);
            graphics.blit(ICONS_TEXTURE, x, y + drawn, texW, take, texU, texV, texW, take, 512, 512);
            drawn += take;
        }
    }
    private static ItemStack createItemStackFromResource(String res) {
        if (res == null || res.isEmpty()) return ItemStack.EMPTY;
        try {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(res));
            if (item != null) return new ItemStack(item);
        } catch (Exception ignored) {}
        return ItemStack.EMPTY;
    }
}