package dev.robotgryphon.screenlib.client.appearance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.robotgryphon.screenlib.client.render.NodeStateRenderer;
import net.minecraft.resources.ResourceKey;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

public record NodeAppearance(List<WidgetAppearance> widgets, Map<String, PropertyAppearance> properties) {
    public static final NodeAppearance EMPTY = new NodeAppearance(List.of(), Map.of());

    public static final Codec<NodeAppearance> CODEC = RecordCodecBuilder.create(i -> i.group(
            WidgetAppearance.CODEC.listOf().optionalFieldOf("widgets", List.of()).forGetter(NodeAppearance::widgets),
            Codec.unboundedMap(Codec.STRING, PropertyAppearance.CODEC).optionalFieldOf("properties", Map.of())
                    .forGetter(NodeAppearance::properties)
    ).apply(i, NodeAppearance::new));

    public PropertyAppearance property(String name) {
        return this.properties.getOrDefault(name, PropertyAppearance.DEFAULT);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<WidgetAppearance> widgets = new ArrayList<>();
        private final Map<String, PropertyAppearance> properties = new LinkedHashMap<>();

        private Builder() {}

        public Builder widget(ResourceKey<NodeStateRenderer<?>> type, String property) {
            this.widgets.add(new WidgetAppearance(type, property));
            return this;
        }

        public Builder property(String name, UnaryOperator<PropertyAppearance> configure) {
            this.properties.put(name, configure.apply(this.properties.getOrDefault(name, PropertyAppearance.DEFAULT)));
            return this;
        }

        public NodeAppearance build() {
            return new NodeAppearance(List.copyOf(this.widgets), Map.copyOf(this.properties));
        }
    }
}
