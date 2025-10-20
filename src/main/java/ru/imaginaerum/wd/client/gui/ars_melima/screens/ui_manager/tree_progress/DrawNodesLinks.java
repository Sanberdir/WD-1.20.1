package ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager.tree_progress;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import ru.imaginaerum.wd.client.gui.ars_melima.progress_tree.ProgressNode;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ClientCookingData;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager.Point;

import java.util.*;

import static ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager.ArsMelimaConstants.ICONS_TEXTURE;
import static ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager.ProgressTreeRenderer.*;

public class DrawNodesLinks {
    public static Map<String, Point> computePositions(List<ProgressNode> nodes, int startX, int startY) {
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

                    // дельты для центрирования фрейма вокруг логической позиции
                    int parentDelta = (parentFrame - NODE_SIZE) / 2;
                    int childDelta = (childFrame - NODE_SIZE) / 2;

                    // расстояние между логическими pos.x (или pos.y) должно учитывать половину суммарных frameSize
                    int halfSumFrames = (parentFrame + childFrame) / 2;
                    int gap = halfSumFrames + parentOffset + LINE_LENGTH + childOffset;

                    String dirRaw = node.getSide() == null ? "" : node.getSide().trim().toLowerCase(Locale.ROOT);
                    String[] parts = dirRaw.split("\\s*,\\s*");

                    if (parts.length == 1) {
                        String dir = parts[0];
                        switch (dir) {
                            case "right" -> nx += gap;
                            case "left"  -> nx -= gap;
                            case "down"  -> ny += gap;
                            case "up"    -> ny -= gap;
                            default -> { /* unknown - leave at base */ }
                        }
                    } else if (parts.length == 2) {
                        String first = parts[0];
                        String second = parts[1];

                        // Обрабатываем только поддерживаемые комбинации:
                        // right,down | down,right | left,down | down,left
                        if ("right".equals(first) && "down".equals(second)) {
                            // horizontal first -> compute child.x based on corner alignment, child.y based on vertical part
                            int parentRightX = base.x + NODE_SIZE + parentDelta + parentOffset;
                            // startX = parentRightX
                            int startXSeg = parentRightX;
                            // square (corner) top-left x: end of first segment + 1 (one-pixel gap)
                            int squareX = startXSeg + LINE_LENGTH + 1; // <-- +1 to have 1px gap before corner
                            // child.x so that vertical strip's x == child.x + NODE_SIZE/2 - 1
                            nx = squareX - (NODE_SIZE / 2) + 1;
                            // squareY aligned to parent's center
                            int squareY = base.y + NODE_SIZE / 2 - 1;
                            // vertical start: corner bottom + 1 (1px gap)
                            int verticalStartY = squareY + CORNER_SIZE + 1; // <-- +1 gap after corner
                            ny = verticalStartY + LINE_LENGTH + childDelta + childOffset;
                        } else if ("down".equals(first) && "right".equals(second)) {
                            int parentBottomY = base.y + NODE_SIZE + parentDelta + parentOffset;
                            int startYSeg = parentBottomY;
                            int squareY = startYSeg + LINE_LENGTH + 1; // <-- +1 gap before corner
                            ny = squareY - (NODE_SIZE / 2) + 1;
                            int squareX = base.x + NODE_SIZE / 2 - 1;
                            // horizontal start: corner right + CORNER_SIZE + 1 (gap after corner)
                            int horizontalStartX = squareX + CORNER_SIZE + 1; // <-- +1 gap after corner
                            nx = horizontalStartX + LINE_LENGTH + childDelta + childOffset;
                        } else if ("left".equals(first) && "down".equals(second)) {
                            int parentLeftX = base.x - parentDelta - parentOffset;
                            int startXSeg = parentLeftX - LINE_LENGTH; // left segment leftmost
                            // square (corner) should be to the left of startXSeg with a 1px gap:
                            int squareX = startXSeg - CORNER_SIZE - 1; // <-- -1 to make 1px gap before corner
                            nx = squareX - (NODE_SIZE / 2) + 1;
                            int squareY = base.y + NODE_SIZE / 2 - 1;
                            int verticalStartY = squareY + CORNER_SIZE + 1; // <-- +1 gap after corner
                            ny = verticalStartY + LINE_LENGTH + childDelta + childOffset;
                        } else if ("down".equals(first) && "left".equals(second)) {
                            // зеркальная и согласованная с drawLinks реализация по отношению к down,right:
                            int parentBottomY = base.y + NODE_SIZE + parentDelta + parentOffset;
                            int startYSeg = parentBottomY;
                            // идём вниз на LINE_LENGTH и делаем 1px gap перед углом
                            int squareY = startYSeg + LINE_LENGTH + 1;
                            // вертикальное выравнивание как в down,right
                            ny = squareY - (NODE_SIZE / 2) + 1;
                            // центр по X родителя
                            int squareX = base.x + NODE_SIZE / 2 - 1;
                            // начало горизонтальной полосы (слева) соответствует cornerX - 1 - LINE_LENGTH (в drawLinks используется именно это)
                            int horizontalStartX = squareX - 1 - LINE_LENGTH;
                            // хотим, чтобы правая грань фрейма ноды была в точке (horizontalStartX - childOffset)
                            // следовательно логическая позиция (pos.x) вычисляется так, чтобы справа от фрейма был gap = childOffset:
                            // nx = horizontalStartX - childOffset - NODE_SIZE - childDelta
                            nx = horizontalStartX - childOffset - NODE_SIZE - childDelta;
                        } else {
                            // unsupported combo - fallback: use simple gap on primary direction
                            if ("right".equals(first) || "left".equals(first)) {
                                if ("right".equals(first)) nx += gap;
                                else nx -= gap;
                            } else if ("down".equals(first) || "up".equals(first)) {
                                if ("down".equals(first)) ny += gap;
                                else ny -= gap;
                            }
                        }
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
     * Рисуем линии: поддерживаются прямые направления и угловые переходы.
     * Для углов: сначала первый сегмент (LINE_LENGTH), затем ОДИН пустой пиксель, затем квадратик (2×2),
     * затем ОДИН пустой пиксель, затем второй сегмент (LINE_LENGTH).
     */
    public static void drawLinks(GuiGraphics graphics, Map<String, Point> positions, List<ProgressNode> nodes) {
        if (nodes == null) return;

        Map<String, ProgressNode> byId = new HashMap<>();
        for (ProgressNode n : nodes) if (n != null && n.getId() != null) byId.put(n.getId(), n);

        for (ProgressNode node : nodes) {
            if (node.getParentId() == null || node.getParentId().isEmpty()) continue;
            Point parentPos = positions.get(node.getParentId());
            Point childPos = positions.get(node.getId());
            if (parentPos == null || childPos == null) continue;

            String dirRaw = node.getSide() == null ? "" : node.getSide().trim().toLowerCase(Locale.ROOT);
            if (dirRaw.isEmpty()) continue;

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

            boolean parentUnlocked = parentNode != null && (!parentNode.isLocked() || ClientCookingData.isProgressUnlocked(parentNode.getId()));
            boolean childUnlocked = (!childNode.isLocked() || ClientCookingData.isProgressUnlocked(childNode.getId()));
            boolean useNew = parentUnlocked || childUnlocked;

            // проникновение в рамку: locked -> 1px, unlocked -> 2px
            int parentPenetration = parentUnlocked ? 2 : 1;
            int childPenetration = childUnlocked ? 2 : 1;

            int parentOffset = parentNode != null && (!parentNode.isLocked() || ClientCookingData.isProgressUnlocked(parentNode.getId()))
                    ? LINE_OFFSET_UNLOCKED : LINE_OFFSET_LOCKED;
            int childOffset = (!childNode.isLocked() || ClientCookingData.isProgressUnlocked(childNode.getId()))
                    ? LINE_OFFSET_UNLOCKED : LINE_OFFSET_LOCKED;

            int visible = LINE_LENGTH;
            if (visible <= 0) continue;

            // параметры новой текстуры
            final int NEW_W = 17;   // new horizontal width
            final int NEW_H = 4;    // new horizontal/vertical thickness
            final int NEW_CENTER_SHIFT = (NEW_W - visible) / 2; // смещение для сохранения центровки

            String[] parts = dirRaw.split("\\s*,\\s*");
            if (parts.length == 1) {
                // простые направления
                switch (parts[0]) {
                    case "right" -> {
                        if (useNew) {
                            int startX = parentPos.x + NODE_SIZE + parentDelta + parentOffset - parentPenetration - NEW_CENTER_SHIFT;
                            int y = parentPos.y + NODE_SIZE / 2 - (NEW_H / 2);
                            drawLineHorizontal17(graphics, startX, y);
                        } else {
                            int startX = parentPos.x + NODE_SIZE + parentDelta + parentOffset;
                            int y = parentPos.y + NODE_SIZE / 2 - 1;
                            drawHorizontalStripTiled(graphics, startX, y, visible);
                        }
                    }
                    case "left" -> {
                        if (useNew) {
                            int startX = childPos.x + NODE_SIZE + childDelta + childOffset - childPenetration - NEW_CENTER_SHIFT;
                            int y = childPos.y + NODE_SIZE / 2 - (NEW_H / 2);
                            drawLineHorizontal17(graphics, startX, y);
                        } else {
                            int startX = childPos.x + NODE_SIZE + childDelta + childOffset;
                            int y = childPos.y + NODE_SIZE / 2 - 1;
                            drawHorizontalStripTiled(graphics, startX, y, visible);
                        }
                    }
                    case "down" -> {
                        if (useNew) {
                            int startY = parentPos.y + NODE_SIZE + parentDelta + parentOffset - parentPenetration - NEW_CENTER_SHIFT;
                            int x = parentPos.x + NODE_SIZE / 2 - (NEW_H / 2);
                            drawLineVertical17(graphics, x, startY);
                        } else {
                            int startY = parentPos.y + NODE_SIZE + parentDelta + parentOffset;
                            int x = parentPos.x + NODE_SIZE / 2 - 1;
                            drawVerticalStripTiled(graphics, x, startY, visible);
                        }
                    }
                    case "up" -> {
                        if (useNew) {
                            int startY = childPos.y + NODE_SIZE + childDelta + childOffset - childPenetration - NEW_CENTER_SHIFT;
                            int x = childPos.x + NODE_SIZE / 2 - (NEW_H / 2);
                            drawLineVertical17(graphics, x, startY);
                        } else {
                            int startY = childPos.y + NODE_SIZE + childDelta + childOffset;
                            int x = childPos.x + NODE_SIZE / 2 - 1;
                            drawVerticalStripTiled(graphics, x, startY, visible);
                        }
                    }
                }
            }

            // L-образные линии
            if (parts.length == 2) {
                String first = parts[0];
                String second = parts[1];

                // right,down
                if ("right".equals(first) && "down".equals(second)) {
                    if (useNew) {
                        // горизонтальный сегмент (новый)
                        int startX = parentPos.x + NODE_SIZE + parentDelta + parentOffset - parentPenetration - NEW_CENTER_SHIFT;
                        int y = parentPos.y + NODE_SIZE / 2 - (NEW_H / 2);
                        drawLineHorizontal17(graphics, startX, y);

                        // угол после конца новой полосы
                        int cornerX = startX + NEW_W + 1;   // +1 gap before corner
                        int cornerY = y;
                        drawCornerSquare(graphics, cornerX, cornerY);

                        int vertX = cornerX;
                        int vertStartY = cornerY + CORNER_SIZE + 1; // +1 gap after corner
                        drawLineVertical17(graphics, vertX, vertStartY);
                    } else {
                        int startX = parentPos.x + NODE_SIZE + parentDelta + parentOffset;
                        int y = parentPos.y + NODE_SIZE / 2 - 1;
                        drawHorizontalStripTiled(graphics, startX, y, visible);

                        int cornerX = startX + visible + 1;   // +1 gap before corner
                        int cornerY = y;
                        drawCornerSquare(graphics, cornerX, cornerY);

                        int vertX = cornerX;
                        int vertStartY = cornerY + CORNER_SIZE + 1; // +1 gap after corner
                        drawVerticalStripTiled(graphics, vertX, vertStartY, visible);
                    }
                }
                // down,right
                else if ("down".equals(first) && "right".equals(second)) {
                    if (useNew) {
                        int startY = parentPos.y + NODE_SIZE + parentDelta + parentOffset - parentPenetration - NEW_CENTER_SHIFT;
                        int x = parentPos.x + NODE_SIZE / 2 - (NEW_H / 2);
                        drawLineVertical17(graphics, x, startY);

                        int cornerX = x;
                        int cornerY = startY + NEW_W + 1; // +1 gap before corner
                        drawCornerSquare(graphics, cornerX, cornerY);

                        int horizStartX = cornerX + CORNER_SIZE + 1; // +1 gap after corner
                        int horizY = cornerY;
                        drawLineHorizontal17(graphics, horizStartX, horizY);
                    } else {
                        int startY = parentPos.y + NODE_SIZE + parentDelta + parentOffset;
                        int x = parentPos.x + NODE_SIZE / 2 - 1;
                        drawVerticalStripTiled(graphics, x, startY, visible);

                        int cornerX = x;
                        int cornerY = startY + visible + 1; // +1 gap before corner
                        drawCornerSquare(graphics, cornerX, cornerY);

                        int horizStartX = cornerX + CORNER_SIZE + 1; // +1 gap after corner
                        int horizY = cornerY;
                        drawHorizontalStripTiled(graphics, horizStartX, horizY, visible);
                    }
                }
                // left,down
                else if ("left".equals(first) && "down".equals(second)) {
                    if (useNew) {
                        int parentLeftFaceX = parentPos.x - parentDelta - parentOffset;
                        // конечная правая координата сегмента должна быть parentLeftFaceX - parentPenetration
                        int startX = parentLeftFaceX - parentPenetration - NEW_W;
                        int y = parentPos.y + NODE_SIZE / 2 - (NEW_H / 2);
                        drawLineHorizontal17(graphics, startX, y);

                        int cornerX = startX - CORNER_SIZE - 1; // gap before corner
                        int cornerY = y;
                        drawCornerSquare(graphics, cornerX, cornerY);

                        int vertX = cornerX;
                        int vertStartY = cornerY + CORNER_SIZE + 1; // +1 gap after corner
                        drawLineVertical17(graphics, vertX, vertStartY);
                    } else {
                        int parentLeftFaceX = parentPos.x - parentDelta - parentOffset;
                        int startX = parentLeftFaceX - visible;
                        int y = parentPos.y + NODE_SIZE / 2 - 1;
                        drawHorizontalStripTiled(graphics, startX, y, visible);

                        int cornerX = startX - CORNER_SIZE - 1; // gap before corner
                        int cornerY = y;
                        drawCornerSquare(graphics, cornerX, cornerY);

                        int vertX = cornerX;
                        int vertStartY = cornerY + CORNER_SIZE + 1; // +1 gap after corner
                        drawVerticalStripTiled(graphics, vertX, vertStartY, visible);
                    }
                }
                // down,left
                else if ("down".equals(first) && "left".equals(second)) {
                    if (useNew) {
                        int startY = parentPos.y + NODE_SIZE + parentDelta + parentOffset - parentPenetration - NEW_CENTER_SHIFT;
                        int x = parentPos.x + NODE_SIZE / 2 - (NEW_H / 2);
                        drawLineVertical17(graphics, x, startY);

                        int cornerX = x;
                        int cornerY = startY + NEW_W + 1; // gap before corner
                        drawCornerSquare(graphics, cornerX, cornerY);

                        int horizStartX = cornerX - 1 - NEW_W;
                        int horizY = cornerY;
                        drawLineHorizontal17(graphics, horizStartX, horizY);
                    } else {
                        int startY = parentPos.y + NODE_SIZE + parentDelta + parentOffset;
                        int x = parentPos.x + NODE_SIZE / 2 - 1;
                        drawVerticalStripTiled(graphics, x, startY, visible);

                        int cornerX = x;
                        int cornerY = startY + visible + 1; // gap before corner
                        drawCornerSquare(graphics, cornerX, cornerY);

                        int horizStartX = cornerX - 1 - visible;
                        int horizY = cornerY;
                        drawHorizontalStripTiled(graphics, horizStartX, horizY, visible);
                    }
                }
            }
        }
    }

}
