package dev.robotgryphon.screenlib.client.ui;

import dev.robotgryphon.screenlib.client.ui.widget.CanvasWidget;
import dev.robotgryphon.screenlib.client.ui.widget.NodeWidget;
import dev.robotgryphon.screenlib.graph.Canvas;
import dev.robotgryphon.screenlib.graph.Node;
import dev.robotgryphon.screenlib.types.NodeDefinition;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class TestScreen extends Screen {
    private final Player player;
    private final Canvas canvas = new Canvas();

    public TestScreen(Player player) {
        super(Component.empty());
        this.player = player;
        this.initCanvas();
    }

    private void initCanvas() {
        int gap = 20;

        // Move canvas to be in the center of the screen
        canvas.pan((float) -this.width / 2, 0);

        // Loop through all node definitions and add one to the canvas
        final var defs = minecraft.level.registryAccess()
                .lookupOrThrow(NodeDefinition.REGISTRY_KEY)
                .listElements()
                .toList();

        int y = 20;
        for (final var defRef : defs) {
            final var title = Component.translatable(defRef.key().identifier().toLanguageKey("node"));

            // Two-step assembly mirrors the Canvas / CanvasWidget split: the
            // Node owns the graph state (definition + layout + materialized
            // ports), and the NodeWidget is purely the on-screen view of it.
            // The Node sizes itself from its title and ports, so we construct
            // it at the origin, then read node.width() to center it horizontally
            // and node.height() to advance the stacking cursor below.
            Node node = new Node(defRef.value(), title, 0, y);
            node.setX((this.width - node.width()) / 2);
            canvas.addNode(new NodeWidget(node));

            y += node.height() + gap;
        }
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(new CanvasWidget(this.canvas, 0, 0, this.width, this.height));
    }
}
