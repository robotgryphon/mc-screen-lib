package dev.robotgryphon.screenlib.graph.ports;

public sealed interface OutputPort extends Port
        permits StandardOutputPort, PropertyOutputPort, ConstantOutputPort {
    @Override
    default PortSide side() {
        return PortSide.RIGHT;
    }
}
