package ru.imaginaerum.wd.client.gui.ars_melima;

public class Chapter {
    private final String id; // имя файла или идентификатор
    private final String title;
    private final String content;
    private final String imageResource; // nullable, ResourceLocation string

    public Chapter(String id, String title, String content, String imageResource) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.imageResource = imageResource;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getImageResource() { return imageResource; }
}