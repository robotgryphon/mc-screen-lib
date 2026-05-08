package com.example.examplemod.client.widget;

import com.example.examplemod.graph.NodeConnection.NodeSide;
import net.minecraft.network.chat.Component;

/**
 * A single connection port on a {@link DraggableWidget}. Multiple ports can
 * share a side; the widget distributes them evenly along that edge.
 *
 * @param side  which edge of the widget this port lives on
 * @param title small label rendered inside the widget body next to the port
 */
public record Port(NodeSide side, Component title) {

    public static Port of(NodeSide side, Component title) {
        return new Port(side, title);
    }

    public static Port of(NodeSide side, String title) {
        return new Port(side, Component.literal(title));
    }
}
