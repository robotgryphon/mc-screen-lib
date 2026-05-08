package com.example.examplemod.client;

import com.example.examplemod.client.widget.Canvas;
import com.example.examplemod.client.widget.DraggableWidget;
import com.example.examplemod.client.widget.DraggableWidgetBuilder;
import com.example.examplemod.graph.NodeConnection.NodeSide;
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

        Canvas canvas = new Canvas(0, 0, this.width, this.height);

        canvas.addNode(new DraggableWidgetBuilder(leftX, y - 60)
                .setWidth(widgetWidth)
                .setHeight(widgetHeight)
                .setTitle(Component.literal("Inventory Reference"))
                .addPort(NodeSide.RIGHT, "Items")
                .build());

        canvas.addNode(new DraggableWidgetBuilder(leftX, y + 60)
                .setWidth(widgetWidth)
                .setHeight(widgetHeight)
                .setTitle(Component.literal("Inventory Reference"))
                .addPort(NodeSide.RIGHT, "Items")
                .build());

        canvas.addNode(new DraggableWidgetBuilder(rightX, y)
                .setWidth(widgetWidth)
                .setHeight(widgetHeight)
                .setTitle(Component.literal("Treecutter Upgrade"))
                .addPort(NodeSide.LEFT, "Tool Storage")
                .addPort(NodeSide.LEFT, "Outputs")
                .build());

        this.addRenderableWidget(canvas);
    }
}
