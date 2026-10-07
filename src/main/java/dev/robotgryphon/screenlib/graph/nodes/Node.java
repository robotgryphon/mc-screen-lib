package dev.robotgryphon.screenlib.graph.nodes;

import dev.robotgryphon.screenlib.graph.schema.node.NodeDefinition;
import dev.robotgryphon.screenlib.graph.Canvas;
import dev.robotgryphon.screenlib.graph.ports.*;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;

public class Node {
    private final ResourceKey<NodeDefinition> type;
    public final Canvas canvas;
    public final Component title;
    private final PortManager portManager;
    private final PropertyManager propertyManager;
    private final RenderManager renderManager;

    public Node(Canvas canvas, Holder<NodeDefinition> definitionHolder, Component title, int x, int y) {
        this.type = definitionHolder.key();
        this.canvas = canvas;
        this.title = title;
        canvas.place(this, x, y);

        NodeDefinition definition = definitionHolder.value();
        this.portManager = new PortManager(this, definition);
        this.propertyManager = new PropertyManager(this, definition);
        this.renderManager = new RenderManager(definition.widgets());
    }

    public PortManager portManager() {
        return this.portManager;
    }

    public RenderManager renderManager() {
        return this.renderManager;
    }

    public PropertyManager propertyManager() {
        return this.propertyManager;
    }

    public static Component defaultTitle(ResourceKey<NodeDefinition> type) {
        return Component.translatable(type.identifier().toLanguageKey("node"));
    }

    public static Component propertyLabel(ResourceKey<NodeDefinition> type, String property) {
        return Component.translatableWithFallback(type.identifier().toLanguageKey("node", property), property);
    }

    public ResourceKey<NodeDefinition> type() {
        return type;
    }
}
