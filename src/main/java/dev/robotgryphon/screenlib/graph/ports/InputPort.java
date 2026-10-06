package dev.robotgryphon.screenlib.graph.ports;

public sealed interface InputPort extends Port permits StandardInputPort, PropertyInputPort {
    @Override
    default PortSide side() {
        return PortSide.LEFT;
    }

    boolean optional();
}
