package ru.imaginaerum.wd.client.gui.ars_melima;

/**
 * Простая модель одной записи TreeLink.
 * id — уникальный ключ (используется также как имя/идентификатор),
 * title — локализованное название (если отсутствует, используется id),
 * icon — опциональная ссылка на ресурс иконки (например "minecraft:book").
 */
public class TreeLink {
    private final String id;
    private final String title;
    private final String icon;

    public TreeLink(String id, String title, String icon) {
        this.id = id != null ? id : "";
        this.title = (title != null && !title.isEmpty()) ? title : this.id;
        this.icon = icon != null ? icon : "";
    }

    public String getId() {
        return id;
    }

    /** Локализованное название (если было в lang/.../tree_links/<chapter>.json). */
    public String getTitle() {
        return title;
    }

    public String getIcon() {
        return icon;
    }

    @Override
    public String toString() {
        return "TreeLink{" + id + " -> " + title + "}";
    }
}
