package ru.imaginaerum.wd.client.gui.ars_melima;

import java.util.ArrayList;
import java.util.List;

public class ArsMelimaMenu {
    private final List<Chapter> chapters = new ArrayList<>();
    private int currentIndex = -1; // -1 = список глав

    public ArsMelimaMenu() { }

    public void setChapters(List<Chapter> list) {
        chapters.clear();
        if (list != null) chapters.addAll(list);
    }

    public List<Chapter> getChapters() {
        return chapters;
    }

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
}
