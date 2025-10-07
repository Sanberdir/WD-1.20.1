package ru.imaginaerum.wd.client.gui.ars_melima.progress_tree;

public class ProgressNode {
    private final String id;
    private final String itemResource;
    private final String description;

    public ProgressNode(String id, String description, String itemResource) {
        this.id = id;
        this.itemResource = itemResource;
        this.description = description;
    }

    public String getId() { return id; }
    public String getItemResource() { return itemResource; }
    public String getDescription() { return description; }
}
