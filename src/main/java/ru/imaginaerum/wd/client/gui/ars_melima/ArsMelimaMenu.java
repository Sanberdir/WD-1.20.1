package ru.imaginaerum.wd.client.gui.ars_melima;

import ru.imaginaerum.wd.client.gui.ars_melima.progress_tree.ProgressNode;

import java.util.ArrayList;
import java.util.List;

public class ArsMelimaMenu {
    public static final int PROGRESSION_INDEX = -2; // новый "индекс" для режима дерева прогресса

    private final List<Chapter> chapters = new ArrayList<>();
    private final List<ProgressNode> progressNodes = new ArrayList<>();
    private int currentIndex = -1; // -1 = список глав

    public ArsMelimaMenu() { }

    public void setChapters(List<Chapter> list) {
        chapters.clear();
        if (list != null) chapters.addAll(list);
    }

    public List<Chapter> getChapters() { return chapters; }

    public void setProgressNodes(List<ProgressNode> nodes) {
        progressNodes.clear();
        if (nodes != null) progressNodes.addAll(nodes);
    }
    public List<ProgressNode> getProgressNodes() { return progressNodes; }

    public int getCurrentIndex() { return currentIndex; }
    public void setCurrentIndex(int idx) { this.currentIndex = idx; }
    public void openChapter(int idx) {
        if (idx >= 0 && idx < chapters.size()) this.currentIndex = idx;
    }
    public void closeChapter() { this.currentIndex = -1; }

    public Chapter getCurrentChapter() {
        if (currentIndex >= 0 && currentIndex < chapters.size()) return chapters.get(currentIndex);
        return null;
    }

    // --- progression helpers ---
    public void openProgression() { this.currentIndex = PROGRESSION_INDEX; }
    public void closeProgression() { this.currentIndex = -1; }
    public boolean isProgressionOpen() { return this.currentIndex == PROGRESSION_INDEX; }
}
