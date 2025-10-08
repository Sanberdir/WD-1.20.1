package ru.imaginaerum.wd.client.gui.ars_melima.screens;

import java.util.HashSet;
import java.util.Set;

public class ClientCookingData {
    public static int clientXp = 0;
    public static int clientLevel = 0;
    public static boolean showNotEnoughLevels = false;

    // Список разблокированных id прогрессий на клиенте (для быстрого рендера)
    public static final Set<String> unlockedProgress = new HashSet<>();

    public static boolean isProgressUnlocked(String id) {
        if (id == null) return false;
        return unlockedProgress.contains(id);
    }

    public static void setUnlockedProgressSet(java.util.Collection<String> ids) {
        unlockedProgress.clear();
        if (ids != null) unlockedProgress.addAll(ids);
    }
}
