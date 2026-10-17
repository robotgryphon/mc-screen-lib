package dev.robotgryphon.screenlib.nodes.client.widget.canvas;

import dev.robotgryphon.screenlib.graph.schema.NodeDefinition;
import dev.robotgryphon.screenlib.ui.api.UiHost;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import org.joml.Vector2dc;

import java.util.List;

public class ProvidedNodesPanel extends NodePalettePanel {
    private static final Component ALL_PLACED = Component.literal("All provided nodes are placed");

    public ProvidedNodesPanel(UiHost host) {
        super(host, Component.literal("Provided nodes"), definitions(host));
    }

    private static List<Holder.Reference<NodeDefinition>> definitions(UiHost host) {
        var definitions = Minecraft.getInstance().level.registryAccess().lookupOrThrow(NodeDefinition.REGISTRY_KEY);
        return host.panel(CanvasPanel.class)
                .map(panel -> panel.session().providedNodes())
                .orElse(List.of())
                .stream()
                .flatMap(type -> definitions.get(type).stream())
                .toList();
    }

    @Override
    protected boolean available(ResourceKey<NodeDefinition> type) {
        return this.host.panel(CanvasPanel.class).map(panel -> panel.session().isUnplaced(type)).orElse(false);
    }

    @Override
    protected Component emptyMessage() {
        return ALL_PLACED;
    }

    @Override
    protected void place(CanvasPanel panel, ResourceKey<NodeDefinition> type, Vector2dc canvasPos) {
        panel.session().addProvidedNode(type, canvasPos);
    }
}
