package com.example.examplemod.client;

import com.example.examplemod.client.render.BezierCurveRenderer;
import com.example.examplemod.client.widget.DraggableWidget;
import com.example.examplemod.client.widget.DraggableWidgetBuilder;
import com.example.examplemod.graph.NodeConnection;
import com.example.examplemod.graph.NodeConnection.NodeSide;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TestScreen extends Screen {
    private static final int[] CONNECTION_PALETTE = {
            CommonColors.GREEN,
            CommonColors.RED,
            CommonColors.YELLOW,
            CommonColors.WHITE,
            CommonColors.LIGHT_GRAY,
    };
    private static final int IN_FLIGHT_COLOR = 0xFFFFD24A;

    private final Player player;
    private final List<DraggableWidget> widgets = new ArrayList<>();
    private final List<WidgetConnection> connections = new ArrayList<>();
    private @Nullable PendingConnection pending;

    public TestScreen(Player player) {
        super(Component.empty());
        this.player = player;
    }

    @Override
    protected void init() {
        super.init();
        this.widgets.clear();
        // Don't clear connections — preserve them across resize.

        int widgetWidth = 100;
        int widgetHeight = 50;
        int gap = 80;
        int totalWidth = widgetWidth * 2 + gap;
        int leftX = (this.width - totalWidth) / 2;
        int rightX = leftX + widgetWidth + gap;
        int y = (this.height - widgetHeight) / 2;

        DraggableWidget left = new DraggableWidgetBuilder(leftX, y)
                .setWidth(widgetWidth)
                .setHeight(widgetHeight)
                .setTitle(Component.literal("Source"))
                .addConnection(NodeSide.RIGHT)
                .build();

        DraggableWidget right = new DraggableWidgetBuilder(rightX, y)
                .setWidth(widgetWidth)
                .setHeight(widgetHeight)
                .setTitle(Component.literal("Target"))
                .addConnection(NodeSide.LEFT)
                .build();

        addRenderableWidget(left);
        addRenderableWidget(right);

        widgets.add(left);
        widgets.add(right);

        // If we already had connections, drop ones whose endpoints aren't in
        // the rebuilt widget list (defensive — same screen instance always
        // keeps the same widgets in this demo).
        this.connections.removeIf(c -> !this.widgets.contains(c.source()) || !this.widgets.contains(c.target()));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            // Walk widgets in reverse so a port belonging to a widget drawn
            // on top wins over an overlapping port underneath.
            for (int i = this.widgets.size() - 1; i >= 0; i--) {
                DraggableWidget widget = this.widgets.get(i);
                NodeSide side = widget.portAt(event.x(), event.y());
                if (side == NodeSide.RIGHT) {
                    this.pending = new PendingConnection(widget, side, event.x(), event.y());
                    // We're handling the drag ourselves, but keep
                    // ContainerEventHandler in a sane state.
                    this.setDragging(true);
                    this.setFocused(null);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.pending != null) {
            this.pending.mouseX = event.x();
            this.pending.mouseY = event.y();
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.pending != null) {
            PendingConnection p = this.pending;
            this.pending = null;
            this.setDragging(false);

            if (event.button() == 0) {
                for (int i = this.widgets.size() - 1; i >= 0; i--) {
                    DraggableWidget widget = this.widgets.get(i);
                    if (widget == p.source) {
                        continue;
                    }
                    NodeSide side = widget.portAt(event.x(), event.y());
                    if (side != null) {
                        int color = CONNECTION_PALETTE[this.connections.size() % CONNECTION_PALETTE.length];
                        this.connections.add(new WidgetConnection(p.source, p.sourceSide, widget, side, color));
                        break;
                    }
                }
            }
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Widgets render in the current stratum.
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        // Connections render above widgets.
        graphics.nextStratum();

        for (WidgetConnection connection : this.connections) {
            NodeConnection curve = connection.toNodeConnection();
            BezierCurveRenderer.render(graphics, curve, curve.bounds());
        }

        if (this.pending != null) {
            Vector2fc start = this.pending.source.portCenter(this.pending.sourceSide);
            Vector2fc end = new Vector2f((float) mouseX, (float) mouseY);
            NodeConnection curve = NodeConnection.rightToLeft(start, end, IN_FLIGHT_COLOR);
            BezierCurveRenderer.render(graphics, curve, curve.bounds());
        }
    }

    /** A finalized connection between two widget ports. */
    private record WidgetConnection(DraggableWidget source, NodeSide sourceSide,
                                    DraggableWidget target, NodeSide targetSide,
                                    int color) {
        NodeConnection toNodeConnection() {
            return NodeConnection.rightToLeft(
                    this.source.portCenter(this.sourceSide),
                    this.target.portCenter(this.targetSide),
                    this.color);
        }
    }

    /** State held between mouseClicked-on-a-port and the corresponding mouseReleased. */
    private static final class PendingConnection {
        final DraggableWidget source;
        final NodeSide sourceSide;
        double mouseX;
        double mouseY;

        PendingConnection(DraggableWidget source, NodeSide sourceSide, double mouseX, double mouseY) {
            this.source = source;
            this.sourceSide = sourceSide;
            this.mouseX = mouseX;
            this.mouseY = mouseY;
        }
    }
}
