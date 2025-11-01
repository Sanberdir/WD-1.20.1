package ru.imaginaerum.wd.client.gui.ars_melima;

public class ChapterMetadata {
    private final String id;
    private final String title;
    private final boolean open;
    private final String icon; // новый

    public ChapterMetadata(String id, String title, boolean open, String icon) {
        this.id = id;
        this.title = title;
        this.open = open;
        this.icon = icon != null ? icon : "";
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public boolean isOpen() { return open; }
    public String getIcon() { return icon; }
}
