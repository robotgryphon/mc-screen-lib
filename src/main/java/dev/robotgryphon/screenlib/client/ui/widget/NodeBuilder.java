package dev.robotgryphon.screenlib.client.ui.widget;

import dev.robotgryphon.screenlib.graph.Port;
import dev.robotgryphon.screenlib.graph.PortSide;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class NodeBuilder {
    private final int x;
    private final int y;
    private int width = 120;
    private int height = 80;
    private Component title = Component.empty();
    private final List<PendingPort> ports = new ArrayList<>();

    public NodeBuilder(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public NodeBuilder setTitle(Component title) {
        this.title = title;
        return this;
    }

    public NodeBuilder setWidth(int width) {
        this.width = width;
        return this;
    }

    public NodeBuilder setHeight(int height) {
        this.height = height;
        return this;
    }

    public NodeBuilder addPort(PortSide side, Component label) {
        this.ports.add(new PendingPort(side, label));
        return this;
    }

    public Node build() {
        final Function<Node, Set<Port>> portMaker = node -> ports.stream()
                .map(p -> p.toPort(node))
                .collect(Collectors.toUnmodifiableSet());

        return new Node(x, y, width, height, title, portMaker);
    }

    private record PendingPort(PortSide side, Component title) {
        public Port toPort(Node n) {
            return new Port(n, side, title);
        }
    }
}
