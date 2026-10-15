package dev.robotgryphon.screenlib.graph.nodes.ports;

public sealed interface OutputPort extends Port
        permits PropertyOutputPort {
    @Override
    default PortSide side() {
        return PortSide.RIGHT;
    }
}
