package dev.robotgryphon.screenlib.nodes.client.state;

import dev.robotgryphon.screenlib.graph.schema.types.GraphDataType;
import net.minecraft.core.Holder;
import org.jspecify.annotations.Nullable;

public final class DragState {
    private @Nullable Holder<GraphDataType<?>> type;

    public @Nullable Holder<GraphDataType<?>> type() {
        return this.type;
    }

    public void setType(@Nullable Holder<GraphDataType<?>> type) {
        this.type = type;
    }
}
