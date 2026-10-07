package dev.robotgryphon.screenlib.graph.nodes;

import com.mojang.serialization.DataResult;
import dev.robotgryphon.screenlib.graph.Canvas;
import dev.robotgryphon.screenlib.graph.ports.*;
import dev.robotgryphon.screenlib.graph.properties.PropertyManager;
import dev.robotgryphon.screenlib.graph.validation.GraphValidator;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;

public class Node {
    private final ResourceKey<NodeDefinition> type;
    public final Canvas canvas;
    public final Component title;
    private final PortManager portManager;
    private final PropertyManager propertyManager;

    public Node(Canvas canvas, Holder<NodeDefinition> definitionHolder, Component title, int x, int y) {
        this.type = definitionHolder.key();
        this.canvas = canvas;
        this.title = title;
        canvas.place(this, x, y);

        NodeDefinition definition = definitionHolder.value();
        this.portManager = new PortManager(this, definition);
        this.propertyManager = new PropertyManager(this, definition.properties());
    }

    public PortManager portManager() {
        return this.portManager;
    }

    public PropertyManager propertyManager() {
        return this.propertyManager;
    }

    public DataResult<Node> validate() {
        DataResult<Node> result = GraphValidator.validateNodeInGraph(this, this.canvas);
        this.canvas.submitValidity(this, result);
        return result;
    }

    public static Component defaultTitle(ResourceKey<NodeDefinition> type) {
        return Component.translatable(type.identifier().toLanguageKey("node"));
    }

    public ResourceKey<NodeDefinition> type() {
        return type;
    }
}
