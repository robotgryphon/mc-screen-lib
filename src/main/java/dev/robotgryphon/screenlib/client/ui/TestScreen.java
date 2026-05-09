package dev.robotgryphon.screenlib.client.ui;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.client.ui.widget.CanvasWidget;
import dev.robotgryphon.screenlib.client.ui.widget.NodeBuilder;
import dev.robotgryphon.screenlib.client.ui.widget.NodeWidget;
import dev.robotgryphon.screenlib.graph.Canvas;
import dev.robotgryphon.screenlib.graph.PortSide;
import dev.robotgryphon.screenlib.types.NodeDefinition;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

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
        int gap = 80;
        int totalWidth = widgetWidth * 2 + gap;
        int leftX = (this.width - totalWidth) / 2;
        int rightX = leftX + widgetWidth + gap;
        int y = (this.height - widgetHeight) / 2;

        canvas.addNode(new NodeBuilder(leftX, y - 60)
                .setWidth(widgetWidth)
                .setHeight(widgetHeight)
                .setTitle(Component.literal("Inventory Reference"))
                .addPort(PortSide.RIGHT, Component.literal("Items"))
                .build());

        canvas.addNode(new NodeBuilder(leftX, y + 60)
                .setWidth(widgetWidth)
                .setHeight(widgetHeight)
                .setTitle(Component.literal("Inventory Reference"))
                .addPort(PortSide.RIGHT, Component.literal("Items"))
                .build());

        // Loop through all node definitions and add one to the canvas
        minecraft.level.registryAccess()
                .lookupOrThrow(NodeDefinition.REGISTRY_KEY)
                .listElements()
                .forEach(ref -> {
                    final var title = Component.translatable(ref.key().identifier().toLanguageKey("node"));
                    final var node = new NodeBuilder(rightX, y)
                            .setWidth(widgetWidth)
                            .setHeight(widgetHeight)
                            .setTitle(title);

                    final var def = ref.value();

                    def.inputs().stream()
                            .sorted()
                            .forEach(propIn -> node.addPort(PortSide.LEFT, Component.literal(propIn)));
                    
                    def.outputs().forEach(propOut -> node.addPort(PortSide.RIGHT, Component.literal(propOut)));

                    canvas.addNode(node.build());
                });
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(new CanvasWidget(this.canvas, 0, 0, this.width, this.height));
    }
}
