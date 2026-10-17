package dev.robotgryphon.screenlib.graph.schema.types;

import dev.robotgryphon.screenlib.graph.schema.types.GraphDataType;

public interface DataTypeWithDefault<T> extends GraphDataType<T> {
    T defaultValue();
}
