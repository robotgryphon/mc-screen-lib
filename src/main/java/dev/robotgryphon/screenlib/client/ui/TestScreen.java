package dev.robotgryphon.screenlib.client.ui;

import dev.robotgryphon.screenlib.client.ui.widget.CanvasWidget;
import dev.robotgryphon.screenlib.client.ui.widget.NodeBuilder;
import dev.robotgryphon.screenlib.graph.Port;
import dev.robotgryphon.screenlib.graph.PortSide;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class TestScreen extends Screen {
    private final Player player;

    public TestScreen(Player player) {
        super(Component.empty());
        this.player = player;
    }

    @Override
    protected void init() {
        super.init();

        int widgetWidth = 110;
        int widgetHeight = 70;
        int gap = 80;
        int totalWidth = widgetWidth * 2 + gap;
        int leftX = (this.width - totalWidth) / 2;
        int rightX = leftX + widgetWidth + gap;
        int y = (this.height - widgetHeight) / 2;

        CanvasWidget canvas = new CanvasWidget(0, 0, this.width, this.height);

        canvas.canvas.addNode(new NodeBuilder(leftX, y - 60)
                .setWidth(widgetWidth)
                .setHeight(widgetHeight)
                .setTitle(Component.literal("Inventory Reference"))
                .addPort(PortSide.RIGHT, Component.literal("Items"))
                .build());

        canvas.canvas.addNode(new NodeBuilder(leftX, y + 60)
                .setWidth(widgetWidth)
                .setHeight(widgetHeight)
                .setTitle(Component.literal("Inventory Reference"))
                .addPort(PortSide.RIGHT, Component.literal("Items"))
                .build());

        canvas.canvas.addNode(new NodeBuilder(rightX, y)
                .setWidth(widgetWidth)
                .setHeight(widgetHeight)
                .setTitle(Component.literal("Treecutter Upgrade"))
                .addPort(PortSide.LEFT, Component.literal("Tool Storage"))
                .addPort(PortSide.LEFT, Component.literal("Outputs"))
                .build());

        this.addRenderableWidget(canvas);
    }
}
