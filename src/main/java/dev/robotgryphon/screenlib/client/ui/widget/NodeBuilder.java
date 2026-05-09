package dev.robotgryphon.screenlib.client.ui.widget;

import dev.robotgryphon.screenlib.graph.Port;
import dev.robotgryphon.screenlib.graph.PortSide;
import dev.robotgryphon.screenlib.types.PortDefinition;
import dev.robotgryphon.screenlib.types.PropertyType;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
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

    /** Adds a typed port with an explicit label. */
    public NodeBuilder addPort(PortSide side, Component label, Holder<PropertyType<?>> type) {
        this.ports.add(new PendingPort(side, label, type));
        return this;
    }

    /** Adds a typed port from a {@link PortDefinition} (uses the def's name as the label). */
    public NodeBuilder addPort(PortSide side, PortDefinition def) {
        return this.addPort(side, Component.literal(def.name()), def.type());
    }

    public NodeWidget build() {
        // List, not Set — declaration order is the user-facing contract: the
        // first port added on a side renders at the top of that side. A
        // HashSet's iteration order is the bucket layout, which scrambles
        // the layout from frame to frame and run to run.
        final Function<NodeWidget, List<Port>> portMaker = node -> ports.stream()
                .map(p -> p.toPort(node))
                .collect(Collectors.toUnmodifiableList());

        return new NodeWidget(x, y, width, height, title, portMaker);
    }

    private record PendingPort(PortSide side, Component title, Holder<PropertyType<?>> type) {
        public Port toPort(NodeWidget n) {
            return new Port(n, side, title, type);
        }
    }
}
