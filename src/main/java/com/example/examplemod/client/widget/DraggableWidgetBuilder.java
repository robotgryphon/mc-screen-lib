package com.example.examplemod.client.widget;

import com.example.examplemod.graph.NodeConnection;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class DraggableWidgetBuilder {
    private final int x;
    private final int y;
    private int width = 120;
    private int height = 80;
    private Component title = Component.empty();
    private final List<Port> ports = new ArrayList<>();

    public DraggableWidgetBuilder(int x, int y) {
        this.x = x;
        this.y = y;
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

    public DraggableWidgetBuilder addPort(Port port) {
        this.ports.add(port);
        return this;
    }

    public DraggableWidgetBuilder addPort(NodeConnection.NodeSide side, Component title) {
        return this.addPort(new Port(side, title));
    }

    public DraggableWidgetBuilder addPort(NodeConnection.NodeSide side, String title) {
        return this.addPort(new Port(side, Component.literal(title)));
    }

    /**
     * Convenience: add an unlabelled port on the given side. Equivalent to
     * {@code addPort(side, Component.empty())}.
     */
    public DraggableWidgetBuilder addConnection(NodeConnection.NodeSide side) {
        return this.addPort(new Port(side, Component.empty()));
    }

    public DraggableWidget build() {
        return new DraggableWidget(x, y, width, height, title, ports.toArray(Port[]::new));
    }
}
