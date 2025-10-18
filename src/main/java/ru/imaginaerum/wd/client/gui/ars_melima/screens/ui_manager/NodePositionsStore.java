package ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager;

import java.util.HashMap;
import java.util.Map;


public class NodePositionsStore {
    private Map<String, Point> positions = new HashMap<>();
    public void setPositions(Map<String, Point> p) { this.positions = new HashMap<>(p); }
    public Map<String, Point> getPositions() { return positions; }
}