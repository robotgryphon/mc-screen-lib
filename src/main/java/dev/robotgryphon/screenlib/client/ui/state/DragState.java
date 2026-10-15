package dev.robotgryphon.screenlib.client.ui.state;

import dev.robotgryphon.screenlib.graph.types.GraphDataType;
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
