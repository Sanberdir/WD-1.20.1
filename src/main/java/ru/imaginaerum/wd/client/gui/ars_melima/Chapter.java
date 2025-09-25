package ru.imaginaerum.wd.client.gui.ars_melima;

import java.util.List;


public class Chapter {
    private final String id;
    private final String title;
    private final List<ChapterElement> elements;

    public Chapter(String id, String title, List<ChapterElement> elements) {
        this.id = id;
        this.title = title;
        this.elements = elements;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public List<ChapterElement> getElements() { return elements; }
}