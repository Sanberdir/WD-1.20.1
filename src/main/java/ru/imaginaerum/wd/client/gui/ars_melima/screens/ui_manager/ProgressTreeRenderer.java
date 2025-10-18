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

import java.util.*;

import static ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager.ArsMelimaConstants.ICONS_TEXTURE;

public class ProgressTreeRenderer {
    // Логический размер ноды (фиксированный)
    private static final int NODE_SIZE = 20;
    // Длина видимой линии между отступами
    private static final int LINE_LENGTH = 9;

    // gap (отступ между визуальной рамкой и линией) для состояний
    private static final int LINE_OFFSET_UNLOCKED = 2; // для разблокированной
    private static final int LINE_OFFSET_LOCKED = 3;   // для заблокированной

    // реальные размеры фреймов (текстур)
    private static final int UNLOCKED_SIZE = 22;
    private static final int LOCKED_SIZE = 20;

    public static void renderProgressTree(ArsMelimaUIManager manager, GuiGraphics graphics,
                                          int mouseX, int mouseY,
                                          ArsMelimaMenu menu,
                                          Font font) {
        List<ProgressNode> nodes = menu.getProgressNodes();
        if (nodes == null || nodes.isEmpty()) return;

        int startX = manager.getGuiLeft() + 7;
        int startY = manager.getGuiTop() + 25;

        Map<String, Point> positions = computePositions(nodes, startX, startY);
        manager.getNodePositionsStore().setPositions(positions);

        drawLinks(graphics, positions, nodes);
        drawNodes(manager, graphics, positions, nodes, mouseX, mouseY, font);
    }

    /**
     * Теперь при вычислении позиций используется средняя половина суммарных frameSize:
     * pos_child = pos_parent + (parentFrame + childFrame)/2 + parentOffset + LINE_LENGTH + childOffset
     * Это гарантирует, что визуальные края фреймов имеют gap = parentOffset ... childOffset.
     */
    private static Map<String, Point> computePositions(List<ProgressNode> nodes, int startX, int startY) {
        Map<String, Point> computed = new HashMap<>();
        Map<String, ProgressNode> byId = new HashMap<>();
        for (ProgressNode n : nodes)
            if (n != null && n.getId() != null)
                byId.put(n.getId(), n);

        Set<String> unresolved = new LinkedHashSet<>(byId.keySet());
        int safety = 0;

        while (!unresolved.isEmpty() && safety++ < 1000) {
            boolean any = false;
            Iterator<String> it = unresolved.iterator();

            while (it.hasNext()) {
                String id = it.next();
                ProgressNode node = byId.get(id);
                if (node == null) { it.remove(); continue; }

                String parentId = node.getParentId();
                boolean hasParent = parentId != null && !parentId.isEmpty();

                if (!hasParent || computed.containsKey(parentId) || !byId.containsKey(parentId)) {
                    Point base = hasParent && computed.containsKey(parentId)
                            ? computed.get(parentId)
                            : new Point(startX, startY);

                    int nx = base.x;
                    int ny = base.y;

                    // frameSize родителя и ребёнка (если нет родителя — используем NODE_SIZE как базу)
                    int parentFrame = NODE_SIZE;
                    if (hasParent) {
                        ProgressNode pnode = byId.get(parentId);
                        if (pnode != null) {
                            parentFrame = (!pnode.isLocked() || ClientCookingData.isProgressUnlocked(pnode.getId()))
                                    ? UNLOCKED_SIZE : LOCKED_SIZE;
                        }
                    }
                    int childFrame = (!node.isLocked() || ClientCookingData.isProgressUnlocked(node.getId()))
                            ? UNLOCKED_SIZE : LOCKED_SIZE;

                    // конкретные отступы (gap от рамки до линии) для каждого
                    int parentOffset = hasParent && byId.get(parentId) != null
                            && (!byId.get(parentId).isLocked() || ClientCookingData.isProgressUnlocked(parentId))
                            ? LINE_OFFSET_UNLOCKED : LINE_OFFSET_LOCKED;
                    int childOffset = (!node.isLocked() || ClientCookingData.isProgressUnlocked(node.getId()))
                            ? LINE_OFFSET_UNLOCKED : LINE_OFFSET_LOCKED;

                    // расстояние между логическими pos.x (или pos.y) должно учитывать половину суммарных frameSize
                    int halfSumFrames = (parentFrame + childFrame) / 2;
                    int gap = halfSumFrames + parentOffset + LINE_LENGTH + childOffset;

                    String dir = node.getSide() == null ? "" : node.getSide().trim().toLowerCase(Locale.ROOT);
                    switch (dir) {
                        case "right" -> nx += gap;
                        case "left"  -> nx -= gap;
                        case "down"  -> ny += gap;
                        case "up"    -> ny -= gap;
                    }

                    computed.put(id, new Point(nx, ny));
                    it.remove();
                    any = true;
                }
            }

            if (!any) break;
        }

        return computed;
    }

    /**
     * Рисуем линии: стартуем от визуальной грани (учитывается delta = (frame - NODE_SIZE)/2) + конкретный offset (locked/unlocked).
     * Длина линии рисуется ровно LINE_LENGTH.
     */
    private static void drawLinks(GuiGraphics graphics, Map<String, Point> positions, List<ProgressNode> nodes) {
        if (nodes == null) return;

        Map<String, ProgressNode> byId = new HashMap<>();
        for (ProgressNode n : nodes) if (n != null && n.getId() != null) byId.put(n.getId(), n);

        for (ProgressNode node : nodes) {
            if (node.getParentId() == null || node.getParentId().isEmpty()) continue;
            Point parentPos = positions.get(node.getParentId());
            Point childPos = positions.get(node.getId());
            if (parentPos == null || childPos == null) continue;

            String dir = node.getSide() == null ? "" : node.getSide().trim().toLowerCase(Locale.ROOT);
            if (dir.isEmpty()) continue;

            RenderSystem.setShaderTexture(0, ICONS_TEXTURE);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

            ProgressNode parentNode = byId.get(node.getParentId());
            ProgressNode childNode = node;

            int parentFrame = parentNode != null && (!parentNode.isLocked() || ClientCookingData.isProgressUnlocked(parentNode.getId()))
                    ? UNLOCKED_SIZE : LOCKED_SIZE;
            int childFrame = (!childNode.isLocked() || ClientCookingData.isProgressUnlocked(childNode.getId()))
                    ? UNLOCKED_SIZE : LOCKED_SIZE;

            int parentDelta = (parentFrame - NODE_SIZE) / 2;
            int childDelta = (childFrame - NODE_SIZE) / 2;

            int parentOffset = parentNode != null && (!parentNode.isLocked() || ClientCookingData.isProgressUnlocked(parentNode.getId()))
                    ? LINE_OFFSET_UNLOCKED : LINE_OFFSET_LOCKED;
            int childOffset = (!childNode.isLocked() || ClientCookingData.isProgressUnlocked(childNode.getId()))
                    ? LINE_OFFSET_UNLOCKED : LINE_OFFSET_LOCKED;

            int visible = LINE_LENGTH;
            if (visible <= 0) continue;

            switch (dir) {
                case "right" -> {
                    // визуальная правая грань = parentPos.x + NODE_SIZE/2 + parentFrame/2
                    // эквивалентно parentPos.x + NODE_SIZE + parentDelta
                    int startX = parentPos.x + NODE_SIZE + parentDelta + parentOffset;
                    int y = parentPos.y + NODE_SIZE / 2 - 1;
                    drawHorizontalStripTiled(graphics, startX, y, visible);
                }
                case "left" -> {
                    int startX = childPos.x + NODE_SIZE + childDelta + childOffset;
                    int y = childPos.y + NODE_SIZE / 2 - 1;
                    drawHorizontalStripTiled(graphics, startX, y, visible);
                }
                case "down" -> {
                    int startY = parentPos.y + NODE_SIZE + parentDelta + parentOffset;
                    int x = parentPos.x + NODE_SIZE / 2 - 1;
                    drawVerticalStripTiled(graphics, x, startY, visible);
                }
                case "up" -> {
                    int startY = childPos.y + NODE_SIZE + childDelta + childOffset;
                    int x = childPos.x + NODE_SIZE / 2 - 1;
                    drawVerticalStripTiled(graphics, x, startY, visible);
                }
            }
        }
    }

    /**
     * Центрируем фрейм (22/20) вокруг логической позиции pos (которая хранится в computed).
     */
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

    // Простые методы отрисовки полосок (рисуют ровно переданную длину)
    private static void drawHorizontalStripTiled(GuiGraphics graphics, int x, int y, int width) {
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

    private static void drawVerticalStripTiled(GuiGraphics graphics, int x, int y, int height) {
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