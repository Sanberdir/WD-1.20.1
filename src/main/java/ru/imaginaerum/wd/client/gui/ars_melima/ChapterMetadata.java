package ru.imaginaerum.wd.client.gui.ars_melima;

public class ChapterMetadata {
    private final String id;
    private final String title;
    private final boolean open;

    public ChapterMetadata(String id, String title, boolean open) {
        this.id = id;
        this.title = title;
        this.open = open;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public boolean isOpen() { return open; }
}