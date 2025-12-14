package ru.imaginaerum.wd.client.gui.ars_melima;

public class ChapterElement {
    public enum Type { TEXT, IMAGE }

    private final Type type;
    private final String data;

    public ChapterElement(Type type, String data) {
        this.type = type;
        this.data = data;
    }

    public Type getType() { return type; }
    public String getData() { return data; }
}
