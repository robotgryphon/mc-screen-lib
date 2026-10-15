package dev.robotgryphon.screenlib.client.appearance;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.graph.sync.ClientDataSubscription;
import dev.robotgryphon.screenlib.graph.nodes.properties.schema.ComputedPropertyDefinition;
import dev.robotgryphon.screenlib.graph.nodes.schema.NodeDefinition;
import net.minecraft.core.Holder;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class NodeAppearances extends SimpleJsonResourceReloadListener<NodeAppearance> {
    public static final Identifier ID = ScreenLib.id("node_appearance");
    public static final String DIRECTORY = "screenlib/node_appearance";
    public static final NodeAppearances INSTANCE = new NodeAppearances();

    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private volatile Map<Identifier, NodeAppearance> appearances = Map.of();

    private NodeAppearances() {
        super(NodeAppearance.CODEC, FileToIdConverter.json(DIRECTORY));
    }

    @Override
    protected void apply(Map<Identifier, NodeAppearance> prepared, ResourceManager manager, ProfilerFiller profiler) {
        this.appearances = Map.copyOf(prepared);
        WARNED.clear();
    }

    public static NodeAppearance get(ResourceKey<NodeDefinition> type) {
        return INSTANCE.appearances.getOrDefault(type.identifier(), NodeAppearance.EMPTY);
    }

    public static List<WidgetAppearance> widgets(Holder<NodeDefinition> definition) {
        ResourceKey<NodeDefinition> type = definition.key();
        return get(type).widgets().stream()
                .filter(widget -> isDisplayable(type, definition.value(), widget))
                .toList();
    }

    private static boolean isDisplayable(ResourceKey<NodeDefinition> type, NodeDefinition definition, WidgetAppearance widget) {
        if (!(definition.property(widget.property()).orElse(null) instanceof ComputedPropertyDefinition computed)) {
            warn(type, widget, "which is not a computed property");
            return false;
        }
        if (computed.clientData().isEmpty()) {
            warn(type, widget, "whose computation does not provide client data");
            return false;
        }
        return true;
    }

    private static void warn(ResourceKey<NodeDefinition> type, WidgetAppearance widget, String reason) {
        if (WARNED.add(type.identifier() + "/" + widget.property())) {
            ScreenLib.LOGGER.warn("Appearance for node {} has a widget for '{}', {}", type.identifier(), widget.property(), reason);
        }
    }

    public static List<ClientDataSubscription> subscriptions() {
        return INSTANCE.appearances.entrySet().stream()
                .flatMap(entry -> entry.getValue().widgets().stream().map(widget -> new ClientDataSubscription(
                        ResourceKey.create(NodeDefinition.REGISTRY_KEY, entry.getKey()), widget.property())))
                .toList();
    }
}
