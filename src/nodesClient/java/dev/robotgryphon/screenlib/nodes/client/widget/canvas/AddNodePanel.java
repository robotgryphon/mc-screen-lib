package dev.robotgryphon.screenlib.nodes.client.widget.canvas;

import dev.robotgryphon.screenlib.graph.schema.NodeDefinition;
import dev.robotgryphon.screenlib.ui.api.UiHost;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import org.joml.Vector2dc;

public class AddNodePanel extends NodePalettePanel {
    public AddNodePanel(UiHost host) {
        super(host, Component.literal("Add node"), Minecraft.getInstance().level.registryAccess()
                .lookupOrThrow(NodeDefinition.REGISTRY_KEY)
                .listElements()
                .filter(definition -> !definition.value().hidden())
                .toList());
    }

    @Override
    protected void place(CanvasPanel panel, ResourceKey<NodeDefinition> type, Vector2dc canvasPos) {
        panel.session().addNode(type, canvasPos);
    }
}
