package dev.robotgryphon.screenlib.nodes.api.canvas;

import dev.robotgryphon.screenlib.nodes.api.NodeReference;
import dev.robotgryphon.screenlib.graph.schema.properties.DataKey;
import dev.robotgryphon.screenlib.nodes.api.spi.NodesProvider;
import net.minecraft.server.MinecraftServer;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface CanvasSupplier {
    static CanvasSupplier of(CanvasDefinition canvas) {
        return NodesProvider.SLOT.get().canvasSupplier(canvas);
    }

    CanvasDefinition canvas();

    <T> CanvasSupplier watch(NodeReference node, DataKey<T> key, Consumer<Optional<T>> listener);

    CanvasSupplier watchValidity(NodeReference node, Consumer<List<String>> listener);

    CanvasSupplier onChanged(Runnable onChanged);

    void refresh(MinecraftServer server);
}
