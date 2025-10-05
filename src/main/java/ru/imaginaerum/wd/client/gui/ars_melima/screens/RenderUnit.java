package ru.imaginaerum.wd.client.gui.ars_melima.screens;

import net.minecraft.util.FormattedCharSequence;

public class RenderUnit {
    public enum Type { TEXT, IMAGE }

    public Type type;
    public FormattedCharSequence line;
    public String imageResource;
    public int rows;

    public RenderUnit() {}
}
