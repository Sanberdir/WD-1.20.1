package ru.imaginaerum.wd.client.gui.ars_melima.progress_tree;

public class ProgressNode {
    private final String id;
    private final String itemResource;
    private final String description;
    private final String parentId; // id родителя
    private final String side;     // down, right, left
    private final boolean locked;

    public ProgressNode(String id, String description, String itemResource, String parentId, String side, boolean locked) {
        this.id = id;
        this.itemResource = itemResource;
        this.description = description;
        this.parentId = parentId;
        this.side = side;
        this.locked = locked;
    }

    public String getId() { return id; }
    public String getItemResource() { return itemResource; }
    public String getDescription() { return description; }
    public String getParentId() { return parentId; }
    public String getSide() { return side; }
    public boolean isLocked() { return locked; }
}

