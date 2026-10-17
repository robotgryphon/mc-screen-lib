package dev.robotgryphon.screenlib.nodes.common.menu;

import dev.robotgryphon.screenlib.graph.schema.NodeDefinition;
import dev.robotgryphon.screenlib.nodes.api.state.CanvasState;
import dev.robotgryphon.screenlib.nodes.common.sync.AllowedValuesSnapshot;
import dev.robotgryphon.screenlib.nodes.common.sync.ClientDataSubscription;
import dev.robotgryphon.screenlib.nodes.common.network.CanvasEditPayload;
import net.minecraft.resources.ResourceKey;
import org.joml.Vector2ic;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CanvasMenuBackend {
    CanvasState state();

    AllowedValuesSnapshot allowedValues();

    List<UUID> requiredNodes();

    void subscribe(Collection<ClientDataSubscription> subscriptions);

    void applyEdit(CanvasEditPayload edit);

    void addNode(ResourceKey<NodeDefinition> type, Vector2ic position);

    void tick();
}
