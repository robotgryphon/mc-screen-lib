package dev.robotgryphon.screenlib.graph.nodes;

import dev.robotgryphon.screenlib.graph.nodes.properties.PropertyManager;

import dev.robotgryphon.screenlib.graph.nodes.schema.NodeDefinition;
import dev.robotgryphon.screenlib.graph.canvas.CanvasGraph;
import dev.robotgryphon.screenlib.graph.nodes.ports.*;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;

import java.util.UUID;

public class Node implements NodeReference {
    private final UUID id;
    private final Holder<NodeDefinition> definition;
    public final CanvasGraph canvas;
    public final Component title;
    private final PortManager portManager;
    private final PropertyManager propertyManager;

    public Node(CanvasGraph canvas, UUID id, Holder<NodeDefinition> definitionHolder, Component title) {
        this.id = id;
        this.definition = definitionHolder;
        this.canvas = canvas;
        this.title = title;

        NodeDefinition definition = definitionHolder.value();
        this.portManager = new PortManager(this, definition);
        this.propertyManager = new PropertyManager(this, definition);
    }

    public PortManager portManager() {
        return this.portManager;
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

    @Override
    public UUID id() {
        return this.id;
    }

    @Override
    public ResourceKey<NodeDefinition> type() {
        return this.definition.key();
    }

    public Holder<NodeDefinition> definition() {
        return this.definition;
    }
}
