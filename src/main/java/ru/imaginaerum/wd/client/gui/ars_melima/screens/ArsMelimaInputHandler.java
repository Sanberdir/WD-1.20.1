package ru.imaginaerum.wd.client.gui.ars_melima.screens;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import ru.imaginaerum.wd.client.gui.ars_melima.*;
import ru.imaginaerum.wd.client.gui.ars_melima.progress_tree.ProgressNode;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager.ArsMelimaConstants;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager.Point;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager.RequestUnlockProgressPacket;

import java.util.List;
import java.util.Map;

import static ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaRenders.OPEN_STRIP_HEIGHT;
import static ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaRenders.TOTAL_STRIP_HEIGHT;

public class ArsMelimaInputHandler {
    private static final int NAV_LEFT_REL_X = 10;
    private static final int NAV_RIGHT_REL_X = 276;
    private static final int NAV_REL_Y = 184;
    private static final int TREE_LINKS_PER_COLUMN = 5;
    private static final int TREE_LINKS_PER_PAGE = TREE_LINKS_PER_COLUMN * 2;
    // Эти константы нужны для работы handleColumnClick
    private static final int CHAPTERS_PER_COLUMN = 5;
    private static final int CHAPTERS_PER_PAGE = CHAPTERS_PER_COLUMN * 2;
    private static final int CONTENT_PADDING = 2;
    // --- state preservation (куда вернуться после Tasks) ---
    private int preservedChapterIndex = -1;
    private int preservedTextPage = 0;
    private int preservedLearningPage = 0;


    public boolean handleMouseClick(double mouseX, double mouseY, int button,
                                    ArsMelimaUIManager uiManager, ArsMelimaMenu menu, ItemStack book) {
        int mx = (int) Math.floor(mouseX);
        int my = (int) Math.floor(mouseY);
        int guiLeft = uiManager.getGuiLeft();
        int guiTop = uiManager.getGuiTop();

        if (menu.isProgressionOpen()) {
            if (handleBackArrowClick(mx, my, button, menu, uiManager)) return true;
            if (handlePageArrowClick(mx, my, button, guiLeft, guiTop, uiManager::getCurrentProgressPage,
                    uiManager::setCurrentProgressPage, computeProgressPageCount(menu.getProgressNodes()))) return true;
            return handleProgressNodesClick(mx, my, button, uiManager, menu);

        } else if (menu.isLearningChaptersOpen()) {
            if (handleBackArrowClick(mx, my, button, menu, uiManager)) return true;
            if (handlePageArrowClick(mx, my, button, guiLeft, guiTop, uiManager::getCurrentLearningPage,
                    uiManager::setCurrentLearningPage, computeLearningPageCount(menu.getCurrentLearningChapters()))) return true;
            return handleLearningChaptersClick(mx, my, button, uiManager, menu);

        } else if (menu.getCurrentIndex() != -1) {
            if (handleBackArrowClick(mx, my, button, menu, uiManager)) return true;
            if (handlePageArrowClick(mx, my, button, guiLeft, guiTop, uiManager::getCurrentTextPage,
                    uiManager::setCurrentTextPage, Integer.MAX_VALUE)) return true;
            return handleChapterContentClick(mx, my, button, guiLeft, guiTop, uiManager, menu);

        } else if (menu.isTasksOpen()) {
            if (handleBackArrowClick(mx, my, button, menu, uiManager)) return true;
            return false;

        } else {
            if (handlePageArrowClick(mx, my, button, guiLeft, guiTop, uiManager::getCurrentChapterPage,
                    uiManager::setCurrentChapterPage, ArsMelimaRenders.computeChapterPageCount(menu.getChapters()))) return true;
            return handleChapterListClick(mx, my, button, guiLeft, guiTop, menu, uiManager);
        }
    }


    private boolean handleChapterContentClick(int mx, int my, int button, int guiLeft, int guiTop,
                                              ArsMelimaUIManager uiManager, ArsMelimaMenu menu) {
        if (button != 0) return false;

        int contentLeft = guiLeft + ArsMelimaConstants.CONTENT_X1;
        int contentTop = guiTop + ArsMelimaConstants.CONTENT_Y1;
        int contentWidth = ArsMelimaConstants.CONTENT_X2 - ArsMelimaConstants.CONTENT_X1;

        int rightContentLeft = guiLeft + ArsMelimaConstants.RIGHT_CONTENT_X1;
        int rightContentTop = guiTop + ArsMelimaConstants.RIGHT_CONTENT_Y1;
        int rightContentWidth = ArsMelimaConstants.RIGHT_CONTENT_X2 - ArsMelimaConstants.RIGHT_CONTENT_X1;

        int currentTextPage = uiManager.getCurrentTextPage();
        Chapter currentChapter = menu.getCurrentChapter();
        if (currentChapter == null) return false;

        // Первая страница: слева LearningChapters, справа первые TreeLinks
        if (currentTextPage == 0) {
            List<LearningChapter> leftList = menu.getLearningChapters(currentChapter.getId());
            if (leftList != null && !leftList.isEmpty()) {
                int startIdx = uiManager.getCurrentLearningPage() * CHAPTERS_PER_PAGE;
                int leftFirstElementY = contentTop;
                // ИСПОЛЬЗУЕМ ОБЩИЙ МЕТОД handleColumnClick
                if (handleColumnClick(mx, my, contentLeft + 8, leftFirstElementY, contentWidth - 16,
                        startIdx, leftList, lc -> openLearningChapter(lc, menu, uiManager))) return true;
            }

            List<TreeLink> rightList = menu.getTreeLinks(currentChapter.getId());
            if (rightList != null && !rightList.isEmpty()) {
                int startIdx = 0;
                int rightFirstElementY = rightContentTop;
                // ИСПОЛЬЗУЕМ ОБЩИЙ МЕТОД handleColumnClick
                if (handleColumnClick(mx, my, rightContentLeft + 8, rightFirstElementY, rightContentWidth - 16,
                        startIdx, rightList, tl -> openTreeLink(tl, menu, uiManager))) return true;
            }
            return false;
        }

        // Остальные страницы: TreeLinks двумя колонками
        List<TreeLink> links = menu.getTreeLinks(currentChapter.getId());
        if (links == null || links.isEmpty()) return false;

        int page = currentTextPage - 1;
        int startIdx = page * TREE_LINKS_PER_PAGE;
        if (startIdx < 0) startIdx = 0;

        // Левая колонка
        if (handleColumnClick(mx, my, contentLeft + 8, contentTop, contentWidth - 16, startIdx, links,
                tl -> openTreeLink(tl, menu, uiManager))) return true;

        // Правая колонка
        if (handleColumnClick(mx, my, rightContentLeft + 8, rightContentTop, rightContentWidth - 16,
                startIdx + TREE_LINKS_PER_COLUMN, links, tl -> openTreeLink(tl, menu, uiManager))) return true;

        return false;
    }



    private void openTreeLink(TreeLink link, ArsMelimaMenu menu, ArsMelimaUIManager uiManager) {
        if (link == null) return;

        // --- сохраняем текущее состояние (для возврата из Tasks) ---
        preservedChapterIndex = menu.getCurrentIndex();
        preservedTextPage = uiManager.getCurrentTextPage();
        preservedLearningPage = uiManager.getCurrentLearningPage();

        String id = link.getId();
        if (id == null || id.isEmpty()) {
            System.out.println("[ArsMelima] TreeLink ID пустой, ничего не открываем.");
            return;
        }

        System.out.println("[ArsMelima] TreeLink clicked: id=\"" + id + "\" title=\"" + link.getTitle() + "\"");

        // --- основной вариант: открываем прогрессию если есть ---
        if (menu.progressTreeExists(id)) {
            System.out.println("[ArsMelima] Opening progression: " + id);
            menu.openProgression(id);
            uiManager.setCurrentProgressPage(0);
            playPageTurnSound();
            return;
        }

        // --- fallback на главу ---
        int idx = menu.getChapterIndexByProgressionId(id);
        if (idx >= 0) {
            menu.openChapter(idx);
            uiManager.setCurrentTextPage(0);
            uiManager.setCurrentLearningPage(0);
            playPageTurnSound();
            return;
        }

        System.out.println("[ArsMelima] Не найдено ни прогрессии, ни главы для TreeLink id: " + id);
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
        if (nodeId == null || nodeId.isEmpty()) return;

        String nodeKey = normalizeKey(nodeId);
        int idx = menu.getChapterIndexByProgressionId(nodeId);
        if (idx < 0) idx = menu.getChapterIndexByProgressionId(nodeKey);

        if (idx >= 0) {
            menu.openChapter(idx);
            uiManager.setCurrentTextPage(0);
            playPageTurnSound();
            return;
        }

        List<ChapterElement> content = ChapterLoader.loadChapterContent(nodeId);
        if ((content == null || content.isEmpty()) && nodeId.contains(":")) {
            String shortId = nodeId.substring(nodeId.indexOf(':') + 1);
            content = ChapterLoader.loadChapterContent(shortId);
            if (content != null && !content.isEmpty()) nodeId = shortId;
        }

        if (content != null && !content.isEmpty()) {
            menu.openDynamicChapter(nodeId, content);
            uiManager.setCurrentTextPage(0);
            playPageTurnSound();
        }
    }


    // --- ОБЩИЙ КЛИК ПО КОЛОНКАМ (LEFT/RIGHT) ---
    private <T> boolean handleColumnClick(int mx, int my, int colLeft, int colTop, int colWidth, int startIdx,
                                          List<T> list, java.util.function.Consumer<T> onClick) {
        // СИНХРОНИЗИРОВАНО С РЕНДЕРЕРОМ: добавляем CONTENT_PADDING
        int relativeY = my - (colTop + CONTENT_PADDING);
        if (relativeY < 0) return false;

        int indexInCol = relativeY / TOTAL_STRIP_HEIGHT;
        int idx = startIdx + indexInCol;

        if (indexInCol < 0 || indexInCol >= CHAPTERS_PER_COLUMN || idx >= list.size()) return false;

        // Расчет области - ТОЧНО как в рендерере
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

        // ПРАВИЛЬНЫЕ КООРДИНАТЫ КОНТЕНТНЫХ ОБЛАСТЕЙ
        int contentLeft = guiLeft + ArsMelimaConstants.CONTENT_X1;
        int contentTop = guiTop + ArsMelimaConstants.CONTENT_Y1;
        int contentWidth = ArsMelimaConstants.CONTENT_X2 - ArsMelimaConstants.CONTENT_X1;

        int rightContentLeft = guiLeft + ArsMelimaConstants.RIGHT_CONTENT_X1;
        int rightContentTop = guiTop + ArsMelimaConstants.RIGHT_CONTENT_Y1;
        int rightContentWidth = ArsMelimaConstants.RIGHT_CONTENT_X2 - ArsMelimaConstants.RIGHT_CONTENT_X1;

        // ИСПОЛЬЗУЕМ ОБЩИЙ МЕТОД handleColumnClick
        return handleColumnClick(mx, my, contentLeft + 8, contentTop, contentWidth - 16, startIdx, list,
                lc -> openLearningChapter(lc, menu, uiManager)) ||
                handleColumnClick(mx, my, rightContentLeft + 8, rightContentTop, rightContentWidth - 16,
                        startIdx + CHAPTERS_PER_COLUMN, list, lc -> openLearningChapter(lc, menu, uiManager));
    }


    private void openLearningChapter(LearningChapter lc, ArsMelimaMenu menu, ArsMelimaUIManager uiManager) {
        if (!lc.isUnlocked()) {
            playPageTurnSound();
            return;
        }

        // --- сохраняем состояние, ОТКУДА открыли Tasks ---
        preservedChapterIndex = menu.getCurrentIndex();
        preservedTextPage = uiManager.getCurrentTextPage();
        preservedLearningPage = uiManager.getCurrentLearningPage();

        menu.openTasks(lc.getId());
        uiManager.setCurrentTaskPage(0);
        playPageTurnSound();

        try {
            boolean completed = isLearningChapterCompleted(lc.getId());
            if (completed) {
                List<LearningChapter> siblings = menu.getCurrentLearningChapters();
                if (siblings != null) {
                    for (LearningChapter child : siblings) {
                        if (child != null && lc.getId().equals(child.getParent()) && child.isLocked()) {
                            menu.unlockLearningChapter(child.getId());
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
        if (tasks == null || tasks.isEmpty()) return false;

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

        // ПРАВИЛЬНЫЕ КООРДИНАТЫ КОНТЕНТНЫХ ОБЛАСТЕЙ
        int contentLeft = guiLeft + ArsMelimaConstants.CONTENT_X1;
        int contentTop = guiTop + ArsMelimaConstants.CONTENT_Y1;
        int contentWidth = ArsMelimaConstants.CONTENT_X2 - ArsMelimaConstants.CONTENT_X1;

        int rightContentLeft = guiLeft + ArsMelimaConstants.RIGHT_CONTENT_X1;
        int rightContentTop = guiTop + ArsMelimaConstants.RIGHT_CONTENT_Y1;
        int rightContentWidth = ArsMelimaConstants.RIGHT_CONTENT_X2 - ArsMelimaConstants.RIGHT_CONTENT_X1;

        // ИСПОЛЬЗУЕМ ОБЩИЙ МЕТОД handleColumnClick
        return handleColumnClick(mx, my, contentLeft + 8, contentTop, contentWidth - 16, startIdx, list, ch -> {
            int chapterIndex = menu.getChapters().indexOf(ch);
            if (chapterIndex >= 0) {
                menu.openChapter(chapterIndex);
                uiManager.setCurrentTextPage(0);
                uiManager.setCurrentLearningPage(0);
                playPageTurnSound();
            }
        }) ||
                handleColumnClick(mx, my, rightContentLeft + 8, rightContentTop, rightContentWidth - 16,
                        startIdx + CHAPTERS_PER_COLUMN, list, ch -> {
                            int chapterIndex = menu.getChapters().indexOf(ch);
                            if (chapterIndex >= 0) {
                                menu.openChapter(chapterIndex);
                                uiManager.setCurrentTextPage(0);
                                uiManager.setCurrentLearningPage(0);
                                playPageTurnSound();
                            }
                        });
    }

    private boolean handlePageArrowClick(int mx, int my, int button, int guiLeft, int guiTop,
                                         java.util.function.Supplier<Integer> getPage, java.util.function.Consumer<Integer> setPage, int totalPages) {
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

    private boolean handleBackArrowClick(int mx, int my, int button,
                                         ArsMelimaMenu menu, ArsMelimaUIManager uiManager) {
        if (button != 0 || !isPointInRect(uiManager.getGuiLeft() + 140, uiManager.getGuiTop() + 184, 15, 15, mx, my))
            return false;

        if (menu.isProgressionOpen()) {
            menu.closeProgression();

        } else if (menu.isTasksOpen()) {
            // закрываем Tasks и восстанавливаем предыдущую главу/страницу (если сохранили)
            menu.closeTasks();
            if (preservedChapterIndex >= 0) {
                menu.openChapter(preservedChapterIndex);
                uiManager.setCurrentTextPage(preservedTextPage);
                uiManager.setCurrentLearningPage(preservedLearningPage);
            }
            // сбрасываем сохранение — чтобы следующее открытие не подхватило старые данные
            preservedChapterIndex = -1;
            preservedTextPage = 0;
            preservedLearningPage = 0;

        } else if (menu.isLearningChaptersOpen()) {
            // если закрываем список LearningChapters — можно попытаться восстановить предыдущую главу,
            // но чаще всего список учебных глав открывается прямо из главы, так что восстановление безопасно:
            menu.closeLearningChapters();
            if (preservedChapterIndex >= 0) {
                menu.openChapter(preservedChapterIndex);
                uiManager.setCurrentTextPage(preservedTextPage);
            }
            preservedChapterIndex = -1;
            preservedTextPage = 0;
            preservedLearningPage = 0;

        } else {
            menu.closeChapter();
        }

        playPageTurnSound();
        return true;
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