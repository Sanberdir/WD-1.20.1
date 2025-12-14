package ru.imaginaerum.wd.client.gui.ars_melima;

public class LearningChapter {
    private final String id;
    private final String title;
    // убрали final, чтобы можно было менять статус
    private String status; // "locked" или "unlocked"
    private final String parent; // id родительской главы

    public LearningChapter(String id, String title, String status, String parent) {
        this.id = id;
        this.title = title;
        this.status = status != null ? status : "locked";
        this.parent = parent;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getStatus() { return status; }
    public String getParent() { return parent; }

    public boolean isUnlocked() { return "unlocked".equalsIgnoreCase(status); }
    public boolean isLocked() { return "locked".equalsIgnoreCase(status); }


    public void unlock() {
        this.status = "unlocked";
    }
}
