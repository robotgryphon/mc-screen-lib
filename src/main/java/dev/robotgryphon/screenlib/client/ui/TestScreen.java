package dev.robotgryphon.screenlib.client.ui;

import dev.robotgryphon.screenlib.client.ui.widget.CanvasWidget;
import dev.robotgryphon.screenlib.client.ui.widget.NodeBuilder;
import dev.robotgryphon.screenlib.graph.Canvas;
import dev.robotgryphon.screenlib.graph.PortSide;
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

        canvas.addNode(new NodeBuilder(rightX, y)
                .setWidth(widgetWidth)
                .setHeight(widgetHeight)
                .setTitle(Component.literal("Treecutter Upgrade"))
                .addPort(PortSide.LEFT, Component.literal("Tool Storage"))
                .addPort(PortSide.LEFT, Component.literal("Outputs"))
                .build());
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(new CanvasWidget(this.canvas, 0, 0, this.width, this.height));
    }
}
