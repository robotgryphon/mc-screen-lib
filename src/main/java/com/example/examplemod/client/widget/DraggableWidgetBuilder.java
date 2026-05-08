package com.example.examplemod.client.widget;

import com.example.examplemod.graph.NodeConnection;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DraggableWidgetBuilder {
    private int x;
    private int y;
    private int width;
    private int height;
    private Component title = Component.empty();
    private List<NodeConnection.NodeSide> sides = new ArrayList<>();

    public DraggableWidgetBuilder(int x, int y) {
        this.x = x;
        this.y = y;
        this.width = 120;
        this.height = 80;
        this.title = Component.empty();
    }

    public DraggableWidgetBuilder setTitle(Component title) {
        this.title = title;
        return this;
    }

    public DraggableWidgetBuilder setWidth(int width) {
        this.width = width;
        return this;
    }

    public DraggableWidgetBuilder setHeight(int height) {
        this.height = height;
        return this;
    }

    public DraggableWidgetBuilder addConnection(NodeConnection.NodeSide side) {
        this.sides.add(side);
        return this;
    }

    public DraggableWidget build() {
        return new DraggableWidget(x, y, width, height, title, sides.toArray(NodeConnection.NodeSide[]::new));
    }
}