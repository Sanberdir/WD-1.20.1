package ru.imaginaerum.wd.client.gui.ars_melima;

import java.util.List;

public class Chapter {
    private final String id;
    private final String title;
    private final List<ChapterElement> elements;
    private final boolean open; // статус
    private final String icon; // ресурс предмета, например "minecraft:diamond_sword"

    public Chapter(String id, String title, List<ChapterElement> elements, boolean open, String icon) {
        this.id = id;
        this.title = title;
        this.elements = elements;
        this.open = open;
        this.icon = icon != null ? icon : "";
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public List<ChapterElement> getElements() { return elements; }
    public boolean isOpen() { return open; }
    public String getIcon() { return icon; }
}
