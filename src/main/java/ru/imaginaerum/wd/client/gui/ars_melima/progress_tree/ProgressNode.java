package ru.imaginaerum.wd.client.gui.ars_melima.progress_tree;

public class ProgressNode {
    private final String id;
    private final String itemResource;
    private final String description;
    private final String parentId;
    private final String side;
    private final boolean locked;
    private final int level; // ← НОВОЕ ПОЛЕ
    // Новое поле: позиция корневой ноды: 1,2,3,4
    private final int rootPosition;

    public ProgressNode(
            String id,
            String description,
            String itemResource,
            String parentId,
            String side,
            boolean locked,
            int rootPosition, // <= новое поле
            int level // ← НОВЫЙ ПАРАМЕТР
    ) {
        this.id = id;
        this.itemResource = itemResource;
        this.description = description;
        this.parentId = parentId;
        this.side = side;
        this.locked = locked;
        this.rootPosition = rootPosition;
        this.level = level;
    }

    public String getId() { return id; }
    public String getItemResource() { return itemResource; }
    public String getDescription() { return description; }
    public String getParentId() { return parentId; }
    public String getSide() { return side; }
    public boolean isLocked() { return locked; }
    public int getLevel() { return level; }
    // Новое
    public int getRootPosition() { return rootPosition; }

    public boolean isRoot() {
        return parentId == null || parentId.isEmpty();
    }
}
