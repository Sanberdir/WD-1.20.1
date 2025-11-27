package ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager.tree_progress;

import java.util.HashMap;
import java.util.Map;

public class ProgressTreeTitlesCache {
    private static final Map<String, String> TREE_TITLES = new HashMap<>();

    public static void setTitle(String treeId, String titleKey) {
        TREE_TITLES.put(treeId, titleKey);
    }

    public static String getTitle(String treeId) {
        return TREE_TITLES.get(treeId);
    }

    public static void clear() {
        TREE_TITLES.clear();
    }
}