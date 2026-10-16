package dev.robotgryphon.screenlib.graph.sync;

import dev.robotgryphon.screenlib.graph.types.RestrictedDataType;
import dev.robotgryphon.screenlib.graph.nodes.properties.AllowedValues;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record AllowedValuesSnapshot(List<Entry<?>> entries) {
    private static final StreamCodec<RegistryFriendlyByteBuf, GraphDataType<?>> TYPE_STREAM_CODEC =
            ByteBufCodecs.registry(GraphDataType.REGISTRY_KEY);

    public static final StreamCodec<RegistryFriendlyByteBuf, AllowedValuesSnapshot> STREAM_CODEC = StreamCodec.of(
            (buffer, snapshot) -> {
                ByteBufCodecs.writeCount(buffer, snapshot.entries().size(), Integer.MAX_VALUE);
                snapshot.entries().forEach(entry -> entry.write(buffer));
            },
            buffer -> {
                int size = ByteBufCodecs.readCount(buffer, Integer.MAX_VALUE);
                List<Entry<?>> entries = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    entries.add(Entry.read(TYPE_STREAM_CODEC.decode(buffer), buffer));
                }
                return new AllowedValuesSnapshot(List.copyOf(entries));
            });

    public static AllowedValuesSnapshot capture(RegistryAccess registries) {
        List<Entry<?>> entries = new ArrayList<>();
        registries.lookupOrThrow(GraphDataType.REGISTRY_KEY).listElements()
                .forEach(holder -> Entry.capture(holder.value(), registries).ifPresent(entries::add));
        return new AllowedValuesSnapshot(List.copyOf(entries));
    }

    public AllowedValues resolve() {
        Map<GraphDataType<?>, Collection<Object>> resolved = new HashMap<>();
        for (Entry<?> entry : this.entries) {
            resolved.put(entry.type(), List.copyOf(entry.values()));
        }
        return type -> Optional.ofNullable(resolved.get(type.value()));
    }

    public record Entry<T>(GraphDataType<T> type, List<T> values) {
        static <T> Optional<Entry<T>> capture(GraphDataType<T> type, RegistryAccess registries) {
            return type instanceof RestrictedDataType<T> restricted
                    ? Optional.of(new Entry<>(type, List.copyOf(restricted.allowedValues(registries))))
                    : Optional.empty();
        }

        static <T> Entry<T> read(GraphDataType<T> type, RegistryFriendlyByteBuf buffer) {
            int size = ByteBufCodecs.readCount(buffer, Integer.MAX_VALUE);
            List<T> values = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                values.add(type.streamCodec().decode(buffer));
            }
            return new Entry<>(type, List.copyOf(values));
        }

        void write(RegistryFriendlyByteBuf buffer) {
            TYPE_STREAM_CODEC.encode(buffer, this.type);
            ByteBufCodecs.writeCount(buffer, this.values.size(), Integer.MAX_VALUE);
            this.values.forEach(value -> this.type.streamCodec().encode(buffer, value));
        }
    }
}
