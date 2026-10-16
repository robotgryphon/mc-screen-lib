package dev.robotgryphon.screenlib.client.ui.widget.toolbar;

import dev.robotgryphon.screenlib.ScreenLib;
import net.minecraft.resources.Identifier;

public final class ToolbarIcons {
    private ToolbarIcons() {}

    public static final Identifier PROVIDED = icon("provided");
    public static final Identifier FAVORITES = icon("favorites");
    public static final Identifier ADD_NODE = icon("add_node");
    public static final Identifier FIT = icon("fit");
    public static final Identifier ZOOM = icon("zoom");
    public static final Identifier MINIMAP = icon("minimap");

    private static Identifier icon(String name) {
        return ScreenLib.id("toolbar/" + name);
    }
}
