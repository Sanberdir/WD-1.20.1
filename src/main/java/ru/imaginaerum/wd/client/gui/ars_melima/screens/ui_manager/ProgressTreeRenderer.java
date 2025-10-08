package ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import ru.imaginaerum.wd.client.gui.ars_melima.progress_tree.ProgressNode;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ArsMelimaDraws;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ArsMelimaUIManager;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ClientCookingData;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ProgressTreeRenderer {

    public static void renderProgressTree(ArsMelimaUIManager manager, GuiGraphics graphics,
                                          int mouseX, int mouseY,
                                          ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaMenu menu,
                                          net.minecraft.client.gui.Font font) {
        java.util.List<ProgressNode> nodes = menu.getProgressNodes();
        if (nodes == null || nodes.isEmpty()) return;

        int startX = manager.getGuiLeft() + 13;
        int startY = manager.getGuiTop() + 25;
        int size = 20;
        int spacing = 13;

        Map<String, Point> computed = computePositions(nodes, startX, startY, size, spacing);
        manager.getNodePositionsStore().setPositions(computed);

        drawLinks(graphics, computed, nodes, size);
        // ВАЖНО: drawNodes — без mouseClicked, только визуал
        drawNodes(manager, graphics, computed, nodes, size, mouseX, mouseY, font);
    }

    private static Map<String, Point> computePositions(java.util.List<ProgressNode> nodes,
                                                       int startX, int startY, int size, int spacing) {
        Map<String, Point> computed = new HashMap<>();
        for (ProgressNode node : nodes) {
            int nx, ny;
            String parentId = node.getParentId();
            if (parentId == null || parentId.isEmpty() || !computed.containsKey(parentId)) {
                nx = startX;
                ny = startY;
            } else {
                Point parentPos = computed.get(parentId);
                switch (node.getSide()) {
                    case "right":
                        nx = parentPos.x + size + spacing;
                        ny = parentPos.y;
                        break;
                    case "left":
                        nx = parentPos.x - size - spacing;
                        ny = parentPos.y;
                        break;
                    case "down":
                    default:
                        nx = parentPos.x;
                        ny = parentPos.y + size + spacing;
                        break;
                }
            }
            computed.put(node.getId(), new Point(nx, ny));
        }
        return computed;
    }

    private static void drawLinks(GuiGraphics graphics, Map<String, Point> computed,
                                  java.util.List<ProgressNode> nodes, int size) {
        final int gap = 2;
        for (ProgressNode node : nodes) {
            if (node.getParentId() == null || node.getParentId().isEmpty()) continue;
            Point parentPos = computed.get(node.getParentId());
            Point childPos = computed.get(node.getId());
            if (parentPos == null || childPos == null) continue;

            int parentCenterX = parentPos.x + size / 2;
            int parentCenterY = parentPos.y + size / 2;
            int childCenterX = childPos.x + size / 2;
            int childCenterY = childPos.y + size / 2;

            if ("right".equals(node.getSide())) {
                int startLineX = parentPos.x + size + gap;
                int endLineX = childPos.x - gap;
                int w = endLineX - startLineX;
                if (w > 0) {
                    int lineY = ((parentCenterY + childCenterY) / 2) - 1;
                    drawHorizontalStripTiled(graphics, startLineX, lineY, w);
                }
            } else if ("left".equals(node.getSide())) {
                int startLineX = childPos.x + size + gap;
                int endLineX = parentPos.x - gap;
                int w = endLineX - startLineX;
                if (w > 0) {
                    int lineY = ((parentCenterY + childCenterY) / 2) - 1;
                    drawHorizontalStripTiled(graphics, startLineX, lineY, w);
                }
            } else {
                int startLineY = parentPos.y + size + gap;
                int endLineY = childPos.y - gap;
                int h = endLineY - startLineY;
                if (h > 0) {
                    int lineX = ((parentCenterX + childCenterX) / 2) - 1;
                    drawVerticalStripTiled(graphics, lineX, startLineY, h);
                }
            }
        }
    }

    private static void drawNodes(ArsMelimaUIManager manager, GuiGraphics graphics,
                                  Map<String, Point> computed, java.util.List<ProgressNode> nodes,
                                  int size, int mouseX, int mouseY, net.minecraft.client.gui.Font font) {
        for (ProgressNode node : nodes) {
            Point pos = computed.get(node.getId());
            if (pos == null) continue;

            int sx = pos.x;
            int sy = pos.y;
            boolean locked = node.isLocked() && !ClientCookingData.isProgressUnlocked(node.getId());

            // Сбрасываем цвет перед рендером квадрата
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            // Рамка узла
            RenderSystem.setShaderTexture(0, ArsMelimaConstants.ICONS_TEXTURE);
            graphics.blit(ArsMelimaConstants.ICONS_TEXTURE, sx, sy, 0, 61, size, size, 512, 512);

            // Затемнение для заблокированных узлов
            if (locked) {
                graphics.fill(sx, sy, sx + size, sy + size, 0x88000000);
            }

            // Рендер предмета
            ItemStack stack = createItemStackFromResource(node.getItemResource());
            if (!stack.isEmpty()) {
                ArsMelimaDraws.renderItem(graphics, stack, sx + 2, sy + 2);
            } else {
                graphics.fill(sx + 2, sy + 2, sx + size - 2, sy + size - 2, 0xFFAAAAAA);
            }

            // Hover и тултип только для открытых
            if (!locked && UIUtils.isPointInRect(sx, sy, size, size, mouseX, mouseY)) {
                // подсветка квадрата
                graphics.fill(sx, sy, sx + size, sy + size, 0x80FFFFFF);

                // тултип всегда белый
                if (node.getDescription() != null && !node.getDescription().isEmpty()) {
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    graphics.renderTooltip(font, Component.literal(node.getDescription()), mouseX, mouseY);
                }
            }
        }
    }


    private static void drawHorizontalStripTiled(GuiGraphics graphics, int x, int y, int width) {
        if (width <= 0) return;
        RenderSystem.setShaderTexture(0, ArsMelimaConstants.ICONS_TEXTURE);
        int tileW = 9;
        int texU = 0, texV = 11, texH = 2;
        int drawn = 0;
        while (drawn < width) {
            int take = Math.min(tileW, width - drawn);
            graphics.blit(
                    ArsMelimaConstants.ICONS_TEXTURE,
                    x + drawn, y,
                    take, texH,
                    texU, texV,
                    take, texH,
                    512, 512
            );
            drawn += take;
        }
    }

    private static void drawVerticalStripTiled(GuiGraphics graphics, int x, int y, int height) {
        if (height <= 0) return;
        RenderSystem.setShaderTexture(0, ArsMelimaConstants.ICONS_TEXTURE);
        int tileH = 9;
        int texU = 0, texV = 0, texW = 2;
        int drawn = 0;
        while (drawn < height) {
            int take = Math.min(tileH, height - drawn);
            graphics.blit(
                    ArsMelimaConstants.ICONS_TEXTURE,
                    x, y + drawn,
                    texW, take,
                    texU, texV,
                    texW, take,
                    512, 512
            );
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
