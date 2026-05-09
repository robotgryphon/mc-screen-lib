package dev.robotgryphon.screenlib.client.ui;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.client.ui.widget.CanvasWidget;
import dev.robotgryphon.screenlib.client.ui.widget.NodeBuilder;
import dev.robotgryphon.screenlib.graph.Canvas;
import dev.robotgryphon.screenlib.graph.PortSide;
import dev.robotgryphon.screenlib.types.NodeDefinition;
import dev.robotgryphon.screenlib.types.PortDefinition;
import dev.robotgryphon.screenlib.types.PropertyType;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
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
        int widgetWidth = 110;
        int widgetHeight = 70;
        int gap = 20;

        // Move canvas to be in the center of the screen
        canvas.pan((float) -this.width / 2, 0);

        // Loop through all node definitions and add one to the canvas
        final var nodes = minecraft.level.registryAccess()
                .lookupOrThrow(NodeDefinition.REGISTRY_KEY)
                .listElements()
                .toList();

        int x = (this.width - widgetWidth) / 2;
        int y = 20;

        for (final var nodeRef : nodes) {
            final var title = Component.translatable(nodeRef.key().identifier().toLanguageKey("node"));
            final var node = new NodeBuilder(x, y)
                    .setWidth(widgetWidth)
                    .setHeight(widgetHeight)
                    .setTitle(title);

            final var def = nodeRef.value();

            for (PortDefinition input : def.inputs()) {
                node.addPort(PortSide.LEFT, input);
            }

            for (PortDefinition output : def.outputs()) {
                node.addPort(PortSide.RIGHT, output);
            }

            canvas.addNode(node.build());
            y += widgetHeight + gap;
        }
    }

    private static Holder<PropertyType<?>> lookup(HolderLookup.RegistryLookup<PropertyType<?>> registry,
                                                  String namespace, String path) {
        return registry.getOrThrow(ResourceKey.create(
                PropertyType.REGISTRY_KEY, Identifier.fromNamespaceAndPath(namespace, path)));
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(new CanvasWidget(this.canvas, 0, 0, this.width, this.height));
    }
}
