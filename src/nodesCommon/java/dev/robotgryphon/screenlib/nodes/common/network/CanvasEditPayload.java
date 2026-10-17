package dev.robotgryphon.screenlib.nodes.common.network;

import dev.robotgryphon.screenlib.nodes.api.state.CanvasState;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public sealed interface CanvasEditPayload extends CustomPacketPayload
        permits MoveNodePayload, SetNodePropertyPayload, ConnectNodesPayload, DisconnectNodesPayload, RemoveNodePayload {
    Optional<CanvasState> apply(CanvasState state, RegistryAccess registries, Set<UUID> required);
}
