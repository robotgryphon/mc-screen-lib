package dev.robotgryphon.screenlib.graph.ports;

public sealed interface InputPort extends Port permits PropertyInputPort {
    @Override
    default PortSide side() {
        return PortSide.LEFT;
    }

    boolean optional();
}
