package ru.imaginaerum.wd.client.gui.ars_melima.screens;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import ru.imaginaerum.wd.client.gui.ars_melima.*;
import ru.imaginaerum.wd.client.gui.ars_melima.progress_tree.ProgressNode;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager.Point;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager.RequestUnlockProgressPacket;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaRenderer.*;
import static ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaRenders.OPEN_STRIP_HEIGHT;
import static ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaRenders.TOTAL_STRIP_HEIGHT;

public class ArsMelimaInputHandler {
    private static final int NAV_LEFT_REL_X = 10;
    private static final int NAV_RIGHT_REL_X = 276;
    private static final int NAV_REL_Y = 184;

    public boolean handleMouseClick(double mouseX, double mouseY, int button,
                                    ArsMelimaUIManager uiManager, ArsMelimaMenu menu, ItemStack book) {
        int mx = (int) Math.floor(mouseX);
        int my = (int) Math.floor(mouseY);
        int guiLeft = uiManager.getGuiLeft();
        int guiTop = uiManager.getGuiTop();

        if (menu.isProgressionOpen()) {
            if (handleBackArrowClick(mx, my, button, guiLeft, guiTop, menu, uiManager)) return true;
            if (handlePageArrowClick(mx, my, button, guiLeft, guiTop, uiManager::getCurrentProgressPage,
                    uiManager::setCurrentProgressPage, computeProgressPageCount(menu.getProgressNodes()))) return true;
            return handleProgressNodesClick(mx, my, button, uiManager, menu);
        } else if (menu.isLearningChaptersOpen()) {
            if (handleBackArrowClick(mx, my, button, guiLeft, guiTop, menu, uiManager)) return true;
            if (handlePageArrowClick(mx, my, button, guiLeft, guiTop, uiManager::getCurrentLearningPage,
                    uiManager::setCurrentLearningPage, computeLearningPageCount(menu.getCurrentLearningChapters()))) return true;
            return handleLearningChaptersClick(mx, my, button, uiManager, menu);
        } else if (menu.getCurrentIndex() != -1) {
            if (handleBackArrowClick(mx, my, button, guiLeft, guiTop, menu, uiManager)) return true;
            if (handlePageArrowClick(mx, my, button, guiLeft, guiTop, uiManager::getCurrentTextPage,
                    uiManager::setCurrentTextPage, Integer.MAX_VALUE)) return true;
        } else if (menu.isTasksOpen()) {
            if (handleBackArrowClick(mx, my, button, guiLeft, guiTop, menu, uiManager)) return true;
            // Задачи не требуют пагинации, просто отображаем список
            return false; // или добавьте обработку кликов по задачам если нужно
        } else {
            if (handlePageArrowClick(mx, my, button, guiLeft, guiTop, uiManager::getCurrentChapterPage,
                    uiManager::setCurrentChapterPage, ArsMelimaRenders.computeChapterPageCount(menu.getChapters()))) return true;
            return handleChapterListClick(mx, my, button, guiLeft, guiTop, menu, uiManager);
        }

        return false;
    }

    private boolean handleProgressNodesClick(int mouseX, int mouseY, int button,
                                             ArsMelimaUIManager uiManager, ArsMelimaMenu menu) {
        if (button != 0) return false;
        Map<String, Point> positions = uiManager.getNodePositions();
        if (positions == null || positions.isEmpty()) return false;

        int size = 20;
        for (ProgressNode node : menu.getProgressNodes()) {
            Point pos = positions.get(node.getId());
            if (pos == null) continue;

            if (isPointInRect(pos.x, pos.y, size, size, mouseX, mouseY)) {
                uiManager.setCurrentProgressNode(node);
                boolean clientUnlocked = ClientCookingData.isProgressUnlocked(node.getId());
                boolean baseLocked = node.isLocked();

                if (baseLocked && !clientUnlocked) {
                    NetworkCookingXp.CHANNEL.send(
                            net.minecraftforge.network.PacketDistributor.SERVER.noArg(),
                            new RequestUnlockProgressPacket(node.getId())
                    );
                    return true;
                }

                handleChapterByNode(node.getId(), menu, uiManager);
                return true;
            }
        }
        return false;
    }

    private void handleChapterByNode(String nodeId, ArsMelimaMenu menu, ArsMelimaUIManager uiManager) {
        String nodeKey = normalizeKey(nodeId);
        int idx = menu.getChapterIndexByProgressionId(nodeId);

        if (idx < 0) {
            outer:
            for (int i = 0; i < menu.getChapters().size(); i++) {
                Chapter ch = menu.getChapters().get(i);
                if (ch == null) continue;
                if (normalizeKey(ch.getId()).equals(nodeKey) || normalizeKey(ch.getTitle()).equals(nodeKey)) {
                    idx = i; break;
                }
                List<ChapterElement> elems = ch.getElements();
                if (elems != null) {
                    for (ChapterElement el : elems) {
                        if (el == null || el.getType() != ChapterElement.Type.TEXT) continue;
                        String data = el.getData() != null ? el.getData().toString() : "";
                        if (data.isEmpty()) continue;
                        try {
                            JsonObject obj = JsonParser.parseString(data).getAsJsonObject();
                            if (obj.has("id") && normalizeKey(obj.get("id").getAsString()).equals(nodeKey)) {
                                idx = i; break outer;
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        }

        if (idx < 0) {
            List<ChapterElement> content = ChapterLoader.loadChapterContent(nodeId);
            if ((content == null || content.isEmpty()) && nodeId != null && nodeId.contains(":")) {
                String shortId = nodeId.substring(nodeId.indexOf(':') + 1);
                content = ChapterLoader.loadChapterContent(shortId);
                if (content != null && !content.isEmpty()) nodeId = shortId;
            }

            if (content != null && !content.isEmpty()) {
                menu.openDynamicChapter(nodeId, content);
                uiManager.setCurrentTextPage(0);
                playPageTurnSound();
                return;
            }
        }

        if (idx >= 0) {
            menu.openChapter(idx);
            uiManager.setCurrentTextPage(0);
            playPageTurnSound();
        }
    }

    // --- ОБЩИЙ КЛИК ПО КОЛОНКАМ (LEFT/RIGHT) ---
    private <T> boolean handleColumnClick(int mx, int my, int colLeft, int colTop, int colWidth, int startIdx,
                                          List<T> list, Consumer<T> onClick) {
        int relativeY = my - (colTop + CONTENT_PADDING);
        int indexInCol = relativeY / TOTAL_STRIP_HEIGHT;
        int idx = startIdx + indexInCol;
        if (indexInCol < 0 || indexInCol >= CHAPTERS_PER_COLUMN || idx >= list.size()) return false;

        int stripX = colLeft + 2;
        int stripY = colTop + CONTENT_PADDING + indexInCol * TOTAL_STRIP_HEIGHT;
        int stripWidth = colWidth - 4;
        int stripHeight = OPEN_STRIP_HEIGHT;

        if (!isPointInRect(stripX, stripY, stripWidth, stripHeight, mx, my)) return false;

        onClick.accept(list.get(idx));
        return true;
    }

    private boolean handleLearningChaptersClick(int mx, int my, int button,
                                                ArsMelimaUIManager uiManager, ArsMelimaMenu menu) {
        if (button != 0) return false;
        List<LearningChapter> list = menu.getCurrentLearningChapters();
        if (list == null || list.isEmpty()) return false;
        int guiLeft = uiManager.getGuiLeft();
        int guiTop = uiManager.getGuiTop();
        int currentPage = uiManager.getCurrentLearningPage();
        int startIdx = currentPage * CHAPTERS_PER_PAGE;

        int leftColX = guiLeft + 8;
        int rightColX = guiLeft + 159;
        int colY = guiTop + 20;
        int colWidth = 129;

        return handleColumnClick(mx, my, leftColX, colY, colWidth, startIdx, list, lc -> openLearningChapter((LearningChapter) lc, menu, uiManager)) ||
                handleColumnClick(mx, my, rightColX, colY, colWidth, startIdx + CHAPTERS_PER_COLUMN, list, lc -> openLearningChapter((LearningChapter) lc, menu, uiManager));
    }

    private void openLearningChapter(LearningChapter lc, ArsMelimaMenu menu, ArsMelimaUIManager uiManager) {
        if (!lc.isUnlocked()) {
            playPageTurnSound();
            return;
        }

        menu.openTasks(lc.getId());
        uiManager.setCurrentTaskPage(0);
        playPageTurnSound();

        // ОСТАВЛЯЕМ существующую логику авто-разблокировки дочерних глав
        try {
            boolean completed = isLearningChapterCompleted(lc.getId());
            if (completed) {
                List<LearningChapter> siblings = menu.getCurrentLearningChapters();
                if (siblings != null) {
                    for (LearningChapter child : siblings) {
                        if (child != null && lc.getId().equals(child.getParent()) && child.isLocked()) {
                            menu.unlockLearningChapter(child.getId());
                            System.out.println("[ArsMelima] Auto-unlocked child learning chapter: " + child.getId());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[ArsMelima] Error while checking/unlocking child learning chapters: " + e.getMessage());
        }
    }
    private boolean isLearningChapterCompleted(String chapterId) {
        if (chapterId == null || chapterId.isEmpty()) return false;

        List<Task> tasks = TaskLoader.loadTasks(chapterId);
        if (tasks == null || tasks.isEmpty()) return false; // либо true — по вашему выбору. Здесь считаем что отсутствие задач = не выполнено.

        for (Task t : tasks) {
            int progress = ClientTaskData.getTaskProgress(chapterId, t.getId());
            if (progress < t.getRequiredCount()) return false;
        }
        return true;
    }

    private boolean handleChapterListClick(int mx, int my, int button, int guiLeft, int guiTop,
                                           ArsMelimaMenu menu, ArsMelimaUIManager uiManager) {
        if (button != 0) return false;
        List<Chapter> list = menu.getChapters();
        int currentPage = uiManager.getCurrentChapterPage();
        int startIdx = currentPage * CHAPTERS_PER_PAGE;

        int leftColX = guiLeft + 8;
        int rightColX = guiLeft + 159;
        int colY = guiTop + 20;
        int colWidth = 129;

        return handleColumnClick(mx, my, leftColX, colY, colWidth, startIdx, list, ch -> {
            menu.openLearningChapters(ch.getId());
            uiManager.setCurrentLearningPage(0);
            playPageTurnSound();
        }) ||
                handleColumnClick(mx, my, rightColX, colY, colWidth, startIdx + CHAPTERS_PER_COLUMN, list, ch -> {
                    menu.openLearningChapters(ch.getId());
                    uiManager.setCurrentLearningPage(0);
                    playPageTurnSound();
                });
    }

    // --- ОБЩАЯ НАВИГАЦИЯ СТРЕЛКАМИ ---
    private boolean handlePageArrowClick(int mx, int my, int button, int guiLeft, int guiTop,
                                         Supplier<Integer> getPage, Consumer<Integer> setPage, int totalPages) {
        if (button != 0) return false;
        if (isPointInRect(guiLeft + NAV_LEFT_REL_X, guiTop + NAV_REL_Y, 12, 7, mx, my)) {
            int p = getPage.get();
            if (p > 0) { setPage.accept(p - 1); playPageTurnSound(); return true; }
        }
        if (isPointInRect(guiLeft + NAV_RIGHT_REL_X, guiTop + NAV_REL_Y, 12, 7, mx, my)) {
            int p = getPage.get();
            if (p < totalPages - 1) { setPage.accept(p + 1); playPageTurnSound(); return true; }
        }
        return false;
    }

    private boolean handleBackArrowClick(int mx, int my, int button, int guiLeft, int guiTop,
                                         ArsMelimaMenu menu, ArsMelimaUIManager uiManager) {
        if (button == 0 && isPointInRect(guiLeft + 140, guiTop + 184, 15, 15, mx, my)) {
            if (menu.isProgressionOpen()) {
                menu.closeProgression();
            } else if (menu.isTasksOpen()) {
                menu.closeTasks(); // ДОБАВЛЕНО: закрываем задачи
            } else if (menu.isLearningChaptersOpen()) {
                menu.closeLearningChapters();
            } else {
                menu.closeChapter();
            }
            uiManager.setCurrentTextPage(0);
            playPageTurnSound();
            return true;
        }
        return false;
    }
    private int computeLearningPageCount(List<LearningChapter> list) {
        return (list == null || list.isEmpty()) ? 1 : (list.size() + CHAPTERS_PER_PAGE - 1) / CHAPTERS_PER_PAGE;
    }

    private int computeProgressPageCount(List<ProgressNode> nodes) {
        return (nodes == null || nodes.isEmpty()) ? 1 : (nodes.size() + CHAPTERS_PER_PAGE - 1) / CHAPTERS_PER_PAGE;
    }

    private boolean isPointInRect(int rx, int ry, int rw, int rh, int px, int py) {
        return px >= rx && py >= ry && px < rx + rw && py < ry + rh;
    }

    private void playPageTurnSound() {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.playSound(
                    net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN,
                    1.0F, 1.0F
            );
        }
    }

    private static String normalizeKey(String raw) {
        if (raw == null) return "";
        String s = raw.trim().toLowerCase(java.util.Locale.ROOT);
        if (s.contains(":")) s = s.substring(s.indexOf(':') + 1);
        s = s.replace('-', '_').replace(' ', '_');
        return s.replaceAll("[^a-z0-9_]", "");
    }
}
