package ru.imaginaerum.wd.client.gui.ars_melima.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import ru.imaginaerum.wd.client.gui.ars_melima.*;
import ru.imaginaerum.wd.client.gui.ars_melima.progress_tree.ProgressNode;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager.*;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import static ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaRenders.OPEN_STRIP_HEIGHT;
import static ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaRenders.TOTAL_STRIP_HEIGHT;

public class ArsMelimaInputHandler {
    private static final int NAV_LEFT_REL_X = 10;
    private static final int NAV_RIGHT_REL_X = 276;
    private static final int NAV_REL_Y = 184;
    private static final int TREE_LINKS_PER_COLUMN = 5;
    private static final int TREE_LINKS_PER_PAGE = TREE_LINKS_PER_COLUMN * 2;
    private static final int CHAPTERS_PER_COLUMN = 5;
    private static final int CHAPTERS_PER_PAGE = CHAPTERS_PER_COLUMN * 2;
    private static final int CONTENT_PADDING = 2;

    // --- state preservation (куда вернуться после Tasks) ---
    private int preservedChapterIndex = -1;
    private int preservedTextPage = 0;
    private int preservedLearningPage = 0;

    // --- для базовых глав ---
    private int preservedBaseChapterIndex = -1;
    private int preservedBaseTextPage = 0;
    private int preservedBaseLearningPage = 0;

    public boolean handleMouseClick(double mouseX, double mouseY, int button,
                                    ArsMelimaUIManager uiManager, ArsMelimaMenu menu, ItemStack book) {
        int mx = (int) Math.floor(mouseX);
        int my = (int) Math.floor(mouseY);
        int guiLeft = uiManager.getGuiLeft();
        int guiTop = uiManager.getGuiTop();

        // Обработка кликов по закладкам
        if (BookmarkRenderer.handleBookmarkClick(mouseX, mouseY, uiManager.getGuiLeft(), uiManager.getGuiTop(), uiManager)) {
            return true;
        }

        // Определяем текущую секцию
        int currentSection = uiManager.getCurrentSection();

        if (currentSection == 0) { // Красная вкладка - базовые главы
            return handleBaseSectionClick(mx, my, button, guiLeft, guiTop, uiManager, menu);
        } else if (currentSection == 1) { // Синяя вкладка - основные главы
            return handleMainSectionClick(mx, my, button, guiLeft, guiTop, uiManager, menu);
        }

        return false;
    }

    // ===================== ОБРАБОТКА КРАСНОЙ ВКЛАДКИ (БАЗОВЫЕ ГЛАВЫ) =====================

    private boolean handleBaseSectionClick(int mx, int my, int button, int guiLeft, int guiTop,
                                           ArsMelimaUIManager uiManager, ArsMelimaMenu menu) {
        if (menu.isBaseChapterOpen()) {
            // Обработка содержимого открытой базовой главы
            return handleBaseChapterContentClick(mx, my, button, guiLeft, guiTop, uiManager, menu);
        } else {
            // Обработка списка базовых глав
            if (handlePageArrowClick(mx, my, button, guiLeft, guiTop,
                    uiManager::getCurrentBaseChapterPage,
                    uiManager::setCurrentBaseChapterPage,
                    ArsMelimaRenders.computeChapterPageCount(menu.getBaseChapters())))
                return true;

            return handleBaseChapterListClick(mx, my, button, guiLeft, guiTop, menu, uiManager);
        }
    }

    private boolean handleBaseChapterContentClick(int mx, int my, int button, int guiLeft, int guiTop,
                                                  ArsMelimaUIManager uiManager, ArsMelimaMenu menu) {
        if (button != 0) return false;

        // Кнопка "назад" для базовых глав
        if (handleBackArrowClick(mx, my, button, guiLeft, guiTop, uiManager, menu, true)) return true;

        // Навигация по страницам для базовых глав
        if (handlePageArrowClick(mx, my, button, guiLeft, guiTop,
                uiManager::getCurrentBaseTextPage,
                uiManager::setCurrentBaseTextPage,
                Integer.MAX_VALUE)) return true;

        // Обработка LearningChapters и TreeLinks внутри базовой главы
        int contentLeft = guiLeft + ArsMelimaConstants.CONTENT_X1;
        int contentTop = guiTop + ArsMelimaConstants.CONTENT_Y1;
        int contentWidth = ArsMelimaConstants.CONTENT_X2 - ArsMelimaConstants.CONTENT_X1;

        int rightContentLeft = guiLeft + ArsMelimaConstants.RIGHT_CONTENT_X1;
        int rightContentTop = guiTop + ArsMelimaConstants.RIGHT_CONTENT_Y1;
        int rightContentWidth = ArsMelimaConstants.RIGHT_CONTENT_X2 - ArsMelimaConstants.RIGHT_CONTENT_X1;

        int currentBaseTextPage = uiManager.getCurrentBaseTextPage();
        Chapter currentChapter = menu.getCurrentBaseChapter();
        if (currentChapter == null) return false;

        // Первая страница: слева LearningChapters, справа первые TreeLinks
        if (currentBaseTextPage == 0) {
            List<LearningChapter> leftList = menu.getLearningChapters(currentChapter.getId());
            if (leftList != null && !leftList.isEmpty()) {
                int startIdx = uiManager.getCurrentBaseLearningPage() * CHAPTERS_PER_PAGE;
                int leftFirstElementY = contentTop;
                if (handleColumnClick(mx, my, contentLeft + 8, leftFirstElementY, contentWidth - 16,
                        startIdx, leftList, lc -> openBaseLearningChapter(lc, menu, uiManager))) return true;
            }

            List<TreeLink> rightList = menu.getBaseTreeLinks(currentChapter.getId());
            if (rightList != null && !rightList.isEmpty()) {
                int startIdx = 0;
                int rightFirstElementY = rightContentTop;
                if (handleColumnClick(mx, my, rightContentLeft + 8, rightFirstElementY, rightContentWidth - 16,
                        startIdx, rightList, tl -> openBaseTreeLink(tl, menu, uiManager))) return true;
            }
            return false;
        }

        // Остальные страницы: TreeLinks двумя колонками
        List<TreeLink> links = menu.getBaseTreeLinks(currentChapter.getId());
        if (links == null || links.isEmpty()) return false;

        int page = currentBaseTextPage - 1;
        int startIdx = page * TREE_LINKS_PER_PAGE;
        if (startIdx < 0) startIdx = 0;

        // Левая колонка
        if (handleColumnClick(mx, my, contentLeft + 8, contentTop, contentWidth - 16, startIdx, links,
                tl -> openBaseTreeLink(tl, menu, uiManager))) return true;

        // Правая колонка
        if (handleColumnClick(mx, my, rightContentLeft + 8, rightContentTop, rightContentWidth - 16,
                startIdx + TREE_LINKS_PER_COLUMN, links, tl -> openBaseTreeLink(tl, menu, uiManager))) return true;

        return false;
    }

    private void openBaseTreeLink(TreeLink link, ArsMelimaMenu menu, ArsMelimaUIManager uiManager) {
        if (link == null) return;

        // --- сохраняем текущее состояние (для возврата из Tasks) ---
        preservedBaseChapterIndex = menu.getCurrentBaseIndex();
        preservedBaseTextPage = uiManager.getCurrentBaseTextPage();
        preservedBaseLearningPage = uiManager.getCurrentBaseLearningPage();

        String id = link.getId();
        if (id == null || id.isEmpty()) {
            System.out.println("[ArsMelima] Base TreeLink ID пустой, ничего не открываем.");
            return;
        }

        System.out.println("[ArsMelima] Base TreeLink clicked: id=\"" + id + "\" title=\"" + link.getTitle() + "\"");

        // --- основной вариант: открываем прогрессию если есть ---
        if (menu.progressTreeExists(id)) {
            System.out.println("[ArsMelima] Opening progression from base chapter: " + id);
            menu.openProgression(id);
            uiManager.setCurrentProgressPage(0);
            playPageTurnSound();
            return;
        }

        // --- fallback на главу ---
        // Сначала пробуем найти в базовых главах
        int baseIdx = findBaseChapterIndexById(menu, id);
        if (baseIdx >= 0) {
            menu.openBaseChapter(baseIdx);
            uiManager.setCurrentBaseTextPage(0);
            uiManager.setCurrentBaseLearningPage(0);
            playPageTurnSound();
            return;
        }

        // Потом пробуем в обычных главах
        int idx = menu.getChapterIndexByProgressionId(id);
        if (idx >= 0) {
            menu.openChapter(idx);
            uiManager.setCurrentTextPage(0);
            uiManager.setCurrentLearningPage(0);
            playPageTurnSound();
            return;
        }

        System.out.println("[ArsMelima] Не найдено ни прогрессии, ни главы для Base TreeLink id: " + id);
    }

    private int findBaseChapterIndexById(ArsMelimaMenu menu, String id) {
        List<Chapter> baseChapters = menu.getBaseChapters();
        if (baseChapters == null || id == null) return -1;

        for (int i = 0; i < baseChapters.size(); i++) {
            Chapter chapter = baseChapters.get(i);
            if (chapter != null && id.equals(chapter.getId())) {
                return i;
            }
        }
        return -1;
    }

    private void openBaseLearningChapter(LearningChapter lc, ArsMelimaMenu menu, ArsMelimaUIManager uiManager) {
        if (!lc.isUnlocked()) {
            playPageTurnSound();
            return;
        }

        // --- сохраняем состояние, ОТКУДА открыли Tasks ---
        preservedBaseChapterIndex = menu.getCurrentBaseIndex();
        preservedBaseTextPage = uiManager.getCurrentBaseTextPage();
        preservedBaseLearningPage = uiManager.getCurrentBaseLearningPage();

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

    private boolean handleBaseChapterListClick(int mx, int my, int button, int guiLeft, int guiTop,
                                               ArsMelimaMenu menu, ArsMelimaUIManager uiManager) {
        if (button != 0) return false;
        List<Chapter> list = menu.getBaseChapters();
        int currentPage = uiManager.getCurrentBaseChapterPage();
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
            int chapterIndex = menu.getBaseChapters().indexOf(ch);
            if (chapterIndex >= 0) {
                menu.openBaseChapter(chapterIndex);
                uiManager.setCurrentBaseTextPage(0);
                uiManager.setCurrentBaseLearningPage(0);
                playPageTurnSound();
            }
        }) ||
                handleColumnClick(mx, my, rightContentLeft + 8, rightContentTop, rightContentWidth - 16,
                        startIdx + CHAPTERS_PER_COLUMN, list, ch -> {
                            int chapterIndex = menu.getBaseChapters().indexOf(ch);
                            if (chapterIndex >= 0) {
                                menu.openBaseChapter(chapterIndex);
                                uiManager.setCurrentBaseTextPage(0);
                                uiManager.setCurrentBaseLearningPage(0);
                                playPageTurnSound();
                            }
                        });
    }

    // ===================== ОБРАБОТКА СИНЕЙ ВКЛАДКИ (ОСНОВНЫЕ ГЛАВЫ) =====================

    private boolean handleMainSectionClick(int mx, int my, int button, int guiLeft, int guiTop,
                                           ArsMelimaUIManager uiManager, ArsMelimaMenu menu) {
        if (menu.isProgressionOpen()) {
            if (handleBackArrowClick(mx, my, button, guiLeft, guiTop, uiManager, menu, false)) return true;
            if (handlePageArrowClick(mx, my, button, guiLeft, guiTop, uiManager::getCurrentProgressPage,
                    uiManager::setCurrentProgressPage, computeProgressPageCount(menu.getProgressNodes()))) return true;
            return handleProgressNodesClick(mx, my, button, uiManager, menu);

        } else if (menu.isLearningChaptersOpen()) {
            if (handleBackArrowClick(mx, my, button, guiLeft, guiTop, uiManager, menu, false)) return true;
            if (handlePageArrowClick(mx, my, button, guiLeft, guiTop, uiManager::getCurrentLearningPage,
                    uiManager::setCurrentLearningPage, computeLearningPageCount(menu.getCurrentLearningChapters()))) return true;
            return handleLearningChaptersClick(mx, my, button, uiManager, menu);

        } else if (menu.getCurrentIndex() != -1) {
            if (handleBackArrowClick(mx, my, button, guiLeft, guiTop, uiManager, menu, false)) return true;
            if (handlePageArrowClick(mx, my, button, guiLeft, guiTop, uiManager::getCurrentTextPage,
                    uiManager::setCurrentTextPage, Integer.MAX_VALUE)) return true;
            return handleChapterContentClick(mx, my, button, guiLeft, guiTop, uiManager, menu);

        } else if (menu.isTasksOpen()) {
            if (handleBackArrowClick(mx, my, button, guiLeft, guiTop, uiManager, menu, false)) return true;
            return false;

        } else {
            if (handlePageArrowClick(mx, my, button, guiLeft, guiTop, uiManager::getCurrentChapterPage,
                    uiManager::setCurrentChapterPage, ArsMelimaRenders.computeChapterPageCount(menu.getChapters()))) return true;
            return handleChapterListClick(mx, my, button, guiLeft, guiTop, menu, uiManager);
        }
    }

    private boolean handleBackArrowClick(int mx, int my, int button, int guiLeft, int guiTop,
                                         ArsMelimaUIManager uiManager, ArsMelimaMenu menu, boolean isBaseSection) {
        if (button != 0 || !isPointInRect(guiLeft + 140, guiTop + 184, 15, 15, mx, my))
            return false;

        if (isBaseSection) {
            // Обработка кнопки "назад" для базовых глав
            if (menu.isTasksOpen()) {
                menu.closeTasks();
                if (preservedBaseChapterIndex >= 0) {
                    menu.openBaseChapter(preservedBaseChapterIndex);
                    uiManager.setCurrentBaseTextPage(preservedBaseTextPage);
                    uiManager.setCurrentBaseLearningPage(preservedBaseLearningPage);
                }
                preservedBaseChapterIndex = -1;
                preservedBaseTextPage = 0;
                preservedBaseLearningPage = 0;
            } else if (menu.isBaseChapterOpen()) {
                menu.closeBaseChapter();
            }
        } else {
            // Обработка кнопки "назад" для основных глав (старая логика)
            if (menu.isProgressionOpen()) {
                menu.closeProgression();
            } else if (menu.isTasksOpen()) {
                menu.closeTasks();
                if (preservedChapterIndex >= 0) {
                    menu.openChapter(preservedChapterIndex);
                    uiManager.setCurrentTextPage(preservedTextPage);
                    uiManager.setCurrentLearningPage(preservedLearningPage);
                }
                preservedChapterIndex = -1;
                preservedTextPage = 0;
                preservedLearningPage = 0;
            } else if (menu.isLearningChaptersOpen()) {
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
        }

        playPageTurnSound();
        return true;
    }

    // ===================== СУЩЕСТВУЮЩИЕ МЕТОДЫ (без RequestUnlockProgressPacket) =====================

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
                if (handleColumnClick(mx, my, contentLeft + 8, leftFirstElementY, contentWidth - 16,
                        startIdx, leftList, lc -> openLearningChapter(lc, menu, uiManager))) return true;
            }

            List<TreeLink> rightList = menu.getTreeLinks(currentChapter.getId());
            if (rightList != null && !rightList.isEmpty()) {
                int startIdx = 0;
                int rightFirstElementY = rightContentTop;
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

                // УБРАЛИ сетевой запрос - открываем главу напрямую
                handleChapterByNode(node.getId(), menu, uiManager);
                return true;
            }
        }
        return false;
    }

    private void handleChapterByNode(String nodeId, ArsMelimaMenu menu, ArsMelimaUIManager uiManager) {
        System.out.println("[DEBUG] handleChapterByNode: nodeId=" + nodeId);

        int idx = menu.getChapterIndexByProgressionId(nodeId);
        System.out.println("[DEBUG] Chapter index by progressionId: " + idx);

        List<String> candidates = buildContentCandidates(nodeId);
        System.out.println("[DEBUG] Content candidates: " + candidates);

        for (String cand : candidates) {
            System.out.println("[DEBUG] Trying to load candidate: " + cand);
            List<ChapterElement> loaded = ChapterLoader.loadChapterContent(cand);
            System.out.println("[DEBUG] ChapterLoader.loadChapterContent(\"" + cand + "\") returned " + (loaded == null ? "null" : loaded.size() + " elements"));
            if (loaded != null && !loaded.isEmpty()) {
                System.out.println("[DEBUG] SUCCESS: Loaded content for " + cand);
                menu.openDynamicChapter(cand, loaded);
                uiManager.setCurrentTextPage(0);
                playPageTurnSound();
                return;
            }
        }

        System.out.println("[DEBUG] No content found for any candidate");
    }

    private List<String> buildContentCandidates(String nodeId) {
        List<String> out = new ArrayList<>();
        if (nodeId == null || nodeId.isEmpty()) return out;

        out.add(nodeId);

        if (nodeId.contains(":")) {
            String after = nodeId.substring(nodeId.indexOf(':') + 1);
            out.add(after);
        }

        if (nodeId.contains("/")) {
            String base = nodeId.substring(nodeId.lastIndexOf('/') + 1);
            out.add(base);
        }

        String norm = normalizeKey(nodeId);
        if (!norm.isEmpty()) out.add(norm);

        if (nodeId.contains(":")) {
            String after = nodeId.substring(nodeId.indexOf(':') + 1);
            String normAfter = normalizeKey(after);
            if (!normAfter.isEmpty()) out.add(normAfter);
        }

        String replaced = nodeId.replace('/', '_').replace('.', '_').replace('-', '_');
        if (!out.contains(replaced)) out.add(replaced);

        String withoutNs = nodeId.contains(":") ? nodeId.substring(nodeId.indexOf(':') + 1) : nodeId;
        String replaced2 = withoutNs.replace('/', '_').replace('.', '_').replace('-', '_');
        if (!out.contains(replaced2)) out.add(replaced2);

        List<String> cleaned = new ArrayList<>();
        for (String s : out) {
            if (s == null) continue;
            String t = s.trim();
            if (t.isEmpty()) continue;
            if (!cleaned.contains(t)) cleaned.add(t);
        }

        return cleaned;
    }

    // --- ОБЩИЙ КЛИК ПО КОЛОНКАМ (LEFT/RIGHT) ---
    private <T> boolean handleColumnClick(int mx, int my, int colLeft, int colTop, int colWidth, int startIdx,
                                          List<T> list, java.util.function.Consumer<T> onClick) {
        int relativeY = my - (colTop + CONTENT_PADDING);
        if (relativeY < 0) return false;

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

        int contentLeft = guiLeft + ArsMelimaConstants.CONTENT_X1;
        int contentTop = guiTop + ArsMelimaConstants.CONTENT_Y1;
        int contentWidth = ArsMelimaConstants.CONTENT_X2 - ArsMelimaConstants.CONTENT_X1;

        int rightContentLeft = guiLeft + ArsMelimaConstants.RIGHT_CONTENT_X1;
        int rightContentTop = guiTop + ArsMelimaConstants.RIGHT_CONTENT_Y1;
        int rightContentWidth = ArsMelimaConstants.RIGHT_CONTENT_X2 - ArsMelimaConstants.RIGHT_CONTENT_X1;

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

        int contentLeft = guiLeft + ArsMelimaConstants.CONTENT_X1;
        int contentTop = guiTop + ArsMelimaConstants.CONTENT_Y1;
        int contentWidth = ArsMelimaConstants.CONTENT_X2 - ArsMelimaConstants.CONTENT_X1;

        int rightContentLeft = guiLeft + ArsMelimaConstants.RIGHT_CONTENT_X1;
        int rightContentTop = guiTop + ArsMelimaConstants.RIGHT_CONTENT_Y1;
        int rightContentWidth = ArsMelimaConstants.RIGHT_CONTENT_X2 - ArsMelimaConstants.RIGHT_CONTENT_X1;

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