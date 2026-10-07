package dev.robotgryphon.screenlib.graph.nodes;

import dev.robotgryphon.screenlib.graph.schema.node.WidgetDefinition;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RenderManager {
    private final List<WidgetDefinition> widgets;
    private final Map<String, Object> states = new HashMap<>();

    public RenderManager(List<WidgetDefinition> widgets) {
        this.widgets = List.copyOf(widgets);
    }

    public List<WidgetDefinition> widgets() {
        return this.widgets;
    }

    public boolean displays(String property) {
        return this.widgets.stream().anyMatch(widget -> widget.property().equals(property));
    }

    public @Nullable Object state(String property) {
        return this.states.get(property);
    }

    public void setState(String property, @Nullable Object state) {
        if (state == null) {
            this.states.remove(property);
        } else {
            this.states.put(property, state);
        }
    }

    public void clear() {
        this.states.clear();
    }
}
