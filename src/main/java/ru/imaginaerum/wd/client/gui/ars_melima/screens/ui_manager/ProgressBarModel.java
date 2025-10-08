package ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager;

public class ProgressBarModel {
    private final int level;
    private final int xp;
    private final int xpForLevel;


    public ProgressBarModel(int level, int xp, int xpForLevel) {
        this.level = level;
        this.xp = xp;
        this.xpForLevel = xpForLevel;
    }


    public int getLevel() { return level; }
    public int getXp() { return xp; }
    public int getXpForLevel() { return xpForLevel; }


    public float getProgressFraction() {
        return xpForLevel > 0 ? Math.min(1.0f, xp / (float) xpForLevel) : 0f;
    }
}