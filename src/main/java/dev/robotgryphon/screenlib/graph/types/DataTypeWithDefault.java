package dev.robotgryphon.screenlib.graph.types;

public interface DataTypeWithDefault<T> extends GraphDataType<T> {
    T defaultValue();
}
