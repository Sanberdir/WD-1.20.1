package ru.imaginaerum.wd.client.gui.ars_melima.screens;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemStack;
import ru.imaginaerum.wd.client.gui.ars_melima.*;
import ru.imaginaerum.wd.client.gui.ars_melima.progress_tree.ProgressNode;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaRenderer.*;
import static ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaRenders.OPEN_STRIP_HEIGHT;
import static ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaRenders.TOTAL_STRIP_HEIGHT;

public class ArsMelimaInputHandler {
    private static final int NAV_LEFT_REL_X = 10;
    private static final int NAV_RIGHT_REL_X = 276;
    private static final int NAV_REL_Y = 184;

    /**
     * Обработчик клика мыши — маршрутизует по режимам (дерево прогресса, текст главы, список глав).
     */
    public boolean handleMouseClick(double mouseX, double mouseY, int button,
                                    ArsMelimaUIManager uiManager, ArsMelimaMenu menu, ItemStack book) {
        int mx = (int) Math.floor(mouseX);
        int my = (int) Math.floor(mouseY);
        int guiLeft = uiManager.getGuiLeft();
        int guiTop = uiManager.getGuiTop();

        if (menu.isProgressionOpen()) {
            // Режим дерева прогресса
            if (handleBackArrowClick(mx, my, button, guiLeft, guiTop, menu, uiManager)) return true;
            if (handleProgressPageArrowClick(mx, my, button, guiLeft, guiTop, menu, uiManager)) return true;
            return handleProgressNodesClick(mx, my, button, uiManager, menu);
        }

        if (menu.getCurrentIndex() != -1) {
            // Режим просмотра текста главы
            if (handleBackArrowClick(mx, my, button, guiLeft, guiTop, menu, uiManager)) return true;
            if (handleTextPageArrowClick(mx, my, button, guiLeft, guiTop, menu, uiManager)) return true;
        } else {
            // Режим списка глав
            if (handleChapterPageArrowClick(mx, my, button, guiLeft, guiTop, menu, uiManager)) return true;
            return handleChapterListClick(mx, my, button, guiLeft, guiTop, menu, uiManager);
        }

        return false;
    }

    // Заменённый метод обработки клика по узлам прогресса (взято у тебя)
    // Вставь / замени этим методом в ArsMelimaInputHandler
    private boolean handleProgressNodesClick(int mouseX, int mouseY, int button,
                                             ArsMelimaUIManager uiManager, ArsMelimaMenu menu) {
        if (button != 0) return false; // только левая кнопка

        Map<String, ArsMelimaUIManager.Point> positions = uiManager.getNodePositions();
        if (positions == null || positions.isEmpty()) return false;

        int size = 20; // размер кликабельного квадрата узла

        for (ProgressNode node : menu.getProgressNodes()) {
            ArsMelimaUIManager.Point pos = positions.get(node.getId());
            if (pos == null) continue;

            if (mouseX >= pos.x && mouseY >= pos.y && mouseX < pos.x + size && mouseY < pos.y + size) {
                uiManager.setCurrentProgressNode(node);
                String nodeId = node.getId();
                String nodeKey = normalizeKey(nodeId);

                // 1) Ищем главу через progressionIdIndex
                int idx = menu.getChapterIndexByProgressionId(nodeId);

                // 2) fallback — если вдруг не нашли через карту, ищем внутри элементов глав (редко нужно)
                if (idx < 0) {
                    outer:
                    for (int i = 0; i < menu.getChapters().size(); i++) {
                        Chapter ch = menu.getChapters().get(i);
                        if (ch == null) continue;

                        // прямое сравнение с id/title главы
                        if (normalizeKey(ch.getId()).equals(nodeKey) || normalizeKey(ch.getTitle()).equals(nodeKey)) {
                            idx = i;
                            break;
                        }

                        // поиск внутри элементов типа TEXT
                        List<ChapterElement> elems = ch.getElements();
                        if (elems != null) {
                            for (ChapterElement el : elems) {
                                if (el == null || el.getType() != ChapterElement.Type.TEXT) continue;
                                String data = el.getData() != null ? el.getData().toString() : "";
                                if (data.isEmpty()) continue;
                                try {
                                    JsonObject obj = JsonParser.parseString(data).getAsJsonObject();
                                    if (obj.has("id") && normalizeKey(obj.get("id").getAsString()).equals(nodeKey)) {
                                        idx = i;
                                        break outer;
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                }

                // 3) Если всё ещё не нашли — пробуем загрузить контент напрямую из ars_melima/content/<id>.json
                if (idx < 0) {
                    // сначала попробуем nodeId как есть, затем вариант без namespace (после :)
                    List<ChapterElement> content = ChapterLoader.loadChapterContent(nodeId);
                    if ((content == null || content.isEmpty()) && nodeId != null && nodeId.contains(":")) {
                        String shortId = nodeId.substring(nodeId.indexOf(':') + 1);
                        content = ChapterLoader.loadChapterContent(shortId);
                        // если нашли, заменим nodeKey и nodeId на короткий вариант для открываемой главы
                        if (content != null && !content.isEmpty()) {
                            nodeKey = normalizeKey(shortId);
                            nodeId = shortId;
                        }
                    }

                    if (content != null && !content.isEmpty()) {
                        // Попробуем найти существующую главу
                        int existing = -1;
                        for (int i = 0; i < menu.getChapters().size(); i++) {
                            Chapter ch = menu.getChapters().get(i);
                            if (ch == null) continue;
                            if (normalizeKey(ch.getId()).equals(normalizeKey(nodeId))) {
                                existing = i;
                                break;
                            }
                        }

                        if (existing >= 0) {
                            idx = existing;
                        } else {
                            // Вместо создания новой главы просто открываем её напрямую
                            menu.openDynamicChapter(nodeId, content); // <- нужен метод для динамического открытия
                            uiManager.setCurrentTextPage(0);
                            playPageTurnSound();
                            return true; // клик обработан
                        }
                    }
                }

                System.out.println("[ArsMelima] progress-node click id='" + nodeId + "' normalized='" + nodeKey + "' -> chapterIndex=" + idx);

                if (idx >= 0) {
                    // Закрывать progression перед открытием главы не нужно — openChapter сам установит currentIndex.
                    // Если всё же хочешь заранее закрыть прогрессию, делай это ДО openChapter:
                    // menu.closeProgression();
                    menu.openChapter(idx);
                    // menu.closeProgression(); // <- удаляем, т.к. оно сбрасывало currentIndex обратно в -1
                    uiManager.setCurrentTextPage(0);
                    playPageTurnSound();
                }
                return true; // клик обработан
            }
        }

        return false;
    }



    // Поместите где-нибудь в ArsMelimaInputHandler (private static) ту же normalizeKey, чтобы обе стороны совпадали:
    private static String normalizeKey(String raw) {
        if (raw == null) return "";
        String s = raw.trim().toLowerCase(java.util.Locale.ROOT);
        if (s.contains(":")) s = s.substring(s.indexOf(':') + 1);
        s = s.replace('-', '_').replace(' ', '_');
        s = s.replaceAll("[^a-z0-9_]", "");
        return s;
    }

    // --- общий back (назад в списки) ---
    private boolean handleBackArrowClick(int mx, int my, int button, int guiLeft, int guiTop,
                                         ArsMelimaMenu menu, ArsMelimaUIManager uiManager) {
        if (button == 0 && isPointInRect(guiLeft + 140, guiTop + 184, 15, 15, mx, my)) {
            if (menu.isProgressionOpen()) {
                menu.closeProgression();
            } else {
                menu.closeChapter();
            }
            uiManager.setCurrentTextPage(0);
            playPageTurnSound();
            return true;
        }
        return false;
    }

    // --- навигация по страницам текста главы ---
    private boolean handleTextPageArrowClick(int mx, int my, int button, int guiLeft, int guiTop,
                                             ArsMelimaMenu menu, ArsMelimaUIManager uiManager) {
        if (button == 0) {
            // Левая стрелка
            if (isPointInRect(guiLeft + NAV_LEFT_REL_X, guiTop + NAV_REL_Y, 12, 7, mx, my)) {
                if (uiManager.getCurrentTextPage() > 0) {
                    uiManager.setCurrentTextPage(uiManager.getCurrentTextPage() - 1);
                    playPageTurnSound();
                    return true;
                }
            }
            // Правая стрелка
            if (isPointInRect(guiLeft + NAV_RIGHT_REL_X, guiTop + NAV_REL_Y, 12, 7, mx, my)) {
                // вычисление pageCount производится в UIManager при рендере; здесь позволим инкремент, UI проверит границы
                uiManager.setCurrentTextPage(uiManager.getCurrentTextPage() + 1);
                playPageTurnSound();
                return true;
            }
        }
        return false;
    }

    // --- навигация по страницам списка глав ---
    private boolean handleChapterPageArrowClick(int mx, int my, int button, int guiLeft, int guiTop,
                                                ArsMelimaMenu menu, ArsMelimaUIManager uiManager) {
        if (button == 0) {
            int totalPages = ArsMelimaRenders.computeChapterPageCount(menu.getChapters());

            // Левая стрелка
            if (isPointInRect(guiLeft + NAV_LEFT_REL_X, guiTop + NAV_REL_Y, 12, 7, mx, my)) {
                if (uiManager.getCurrentChapterPage() > 0) {
                    uiManager.setCurrentChapterPage(uiManager.getCurrentChapterPage() - 1);
                    playPageTurnSound();
                    return true;
                }
            }
            // Правая стрелка
            if (isPointInRect(guiLeft + NAV_RIGHT_REL_X, guiTop + NAV_REL_Y, 12, 7, mx, my)) {
                if (uiManager.getCurrentChapterPage() < totalPages - 1) {
                    uiManager.setCurrentChapterPage(uiManager.getCurrentChapterPage() + 1);
                    playPageTurnSound();
                    return true;
                }
            }
        }
        return false;
    }

    // --- клик по списку глав (две колонки) ---
    private boolean handleChapterListClick(int mx, int my, int button, int guiLeft, int guiTop,
                                           ArsMelimaMenu menu, ArsMelimaUIManager uiManager) {
        // Левая область
        int leftContentLeft = guiLeft + 8;
        int leftContentTop = guiTop + 20;
        int leftContentWidth = 137 - 8;    // 129 пикселей
        int leftContentHeight = 160 - 20;  // 140 пикселей

        // Правая область
        int rightContentLeft = guiLeft + 159;
        int rightContentTop = guiTop + 20;
        int rightContentWidth = leftContentWidth;
        int rightContentHeight = leftContentHeight;

        if (button == 0) {
            int currentPage = uiManager.getCurrentChapterPage();
            int startIdx = currentPage * CHAPTERS_PER_PAGE;

            // Левая колонка
            if (isPointInRect(leftContentLeft, leftContentTop, leftContentWidth, leftContentHeight, mx, my)) {
                int relativeY = my - (leftContentTop + CONTENT_PADDING);
                int chapterIndexInColumn = relativeY / TOTAL_STRIP_HEIGHT;
                int idx = startIdx + chapterIndexInColumn;

                if (chapterIndexInColumn >= 0 && chapterIndexInColumn < CHAPTERS_PER_COLUMN &&
                        idx < menu.getChapters().size() && idx < startIdx + CHAPTERS_PER_COLUMN) {

                    int stripY = leftContentTop + CONTENT_PADDING + chapterIndexInColumn * TOTAL_STRIP_HEIGHT;
                    int stripHeight = OPEN_STRIP_HEIGHT;
                    int stripX = leftContentLeft + 2;
                    int stripWidth = leftContentWidth - 4;

                    if (isPointInRect(stripX, stripY, stripWidth, stripHeight, mx, my)) {
                        // ВАЖНО: вместо открытия главы — открываем дерево прогресса
                        menu.openProgression();
                        uiManager.setCurrentProgressPage(0);
                        playPageTurnSound();
                        return true;
                    }
                }
            }

            // Правая колонка
            if (isPointInRect(rightContentLeft, rightContentTop, rightContentWidth, rightContentHeight, mx, my)) {
                int relativeY = my - (rightContentTop + CONTENT_PADDING);
                int chapterIndexInColumn = relativeY / TOTAL_STRIP_HEIGHT;
                int idx = startIdx + CHAPTERS_PER_COLUMN + chapterIndexInColumn;

                if (chapterIndexInColumn >= 0 && chapterIndexInColumn < CHAPTERS_PER_COLUMN &&
                        idx < menu.getChapters().size() && idx < startIdx + CHAPTERS_PER_PAGE) {

                    int stripY = rightContentTop + CONTENT_PADDING + chapterIndexInColumn * TOTAL_STRIP_HEIGHT;
                    int stripHeight = OPEN_STRIP_HEIGHT;
                    int stripX = rightContentLeft + 2;
                    int stripWidth = rightContentWidth - 4;

                    if (isPointInRect(stripX, stripY, stripWidth, stripHeight, mx, my)) {
                        // ВАЖНО: вместо открытия главы — открываем дерево прогресса
                        menu.openProgression();
                        uiManager.setCurrentProgressPage(0);
                        playPageTurnSound();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // --- прогресс: стрелки страниц ---
    private boolean handleProgressPageArrowClick(int mx, int my, int button, int guiLeft, int guiTop,
                                                 ArsMelimaMenu menu, ArsMelimaUIManager uiManager) {
        if (button == 0) {
            int totalPages = computeProgressPageCount(menu.getProgressNodes());

            if (isPointInRect(guiLeft + NAV_LEFT_REL_X, guiTop + NAV_REL_Y, 12, 7, mx, my)) {
                if (uiManager.getCurrentProgressPage() > 0) {
                    uiManager.setCurrentProgressPage(uiManager.getCurrentProgressPage() - 1);
                    playPageTurnSound();
                    return true;
                }
            }
            if (isPointInRect(guiLeft + NAV_RIGHT_REL_X, guiTop + NAV_REL_Y, 12, 7, mx, my)) {
                if (uiManager.getCurrentProgressPage() < totalPages - 1) {
                    uiManager.setCurrentProgressPage(uiManager.getCurrentProgressPage() + 1);
                    playPageTurnSound();
                    return true;
                }
            }
        }
        return false;
    }

    private int computeProgressPageCount(java.util.List<ProgressNode> nodes) {
        if (nodes == null || nodes.isEmpty()) return 1;
        return (nodes.size() + CHAPTERS_PER_PAGE - 1) / CHAPTERS_PER_PAGE;
    }

    // --- утилиты ---
    private boolean isPointInRect(int rx, int ry, int rw, int rh, int px, int py) {
        return px >= rx && py >= ry && px < rx + rw && py < ry + rh;
    }

    private void playPageTurnSound() {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.playSound(
                    net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN,
                    1.0F,
                    1.0F
            );
        }
    }
}
