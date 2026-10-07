package dev.robotgryphon.screenlib.graph.schema;

import com.mojang.serialization.Codec;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record AllowedValuesSnapshot(Map<ResourceKey<GraphDataType<?>>, ListTag> values) {
    public static final StreamCodec<RegistryFriendlyByteBuf, AllowedValuesSnapshot> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ResourceKey.streamCodec(GraphDataType.REGISTRY_KEY), ByteBufCodecs.TAG.map(tag -> (ListTag) tag, tag -> tag)),
            AllowedValuesSnapshot::values,
            AllowedValuesSnapshot::new
    );

    @SuppressWarnings("unchecked")
    public static AllowedValuesSnapshot capture(RegistryAccess registries) {
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        Map<ResourceKey<GraphDataType<?>>, ListTag> captured = new HashMap<>();
        registries.lookupOrThrow(GraphDataType.REGISTRY_KEY).listElements().forEach(holder -> {
            Optional<Collection<Object>> values = AllowedValues.unchecked(holder.value().allowedValues(registries));
            if (values.isEmpty()) {
                return;
            }
            Codec<Object> codec = (Codec<Object>) holder.value().codec();
            ListTag list = new ListTag();
            for (Object value : values.get()) {
                codec.encodeStart(ops, value).result().ifPresent(list::add);
            }
            captured.put(holder.key(), list);
        });
        return new AllowedValuesSnapshot(captured);
    }

    @SuppressWarnings("unchecked")
    public AllowedValues resolve(RegistryAccess registries) {
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        var types = registries.lookupOrThrow(GraphDataType.REGISTRY_KEY);
        Map<ResourceKey<GraphDataType<?>>, Collection<Object>> decoded = new HashMap<>();
        this.values.forEach((key, list) -> types.get(key).ifPresent(holder -> {
            Codec<Object> codec = (Codec<Object>) holder.value().codec();
            List<Object> out = new ArrayList<>();
            for (Tag tag : list) {
                codec.parse(ops, tag).result().ifPresent(out::add);
            }
            decoded.put(key, List.copyOf(out));
        }));
        return type -> type.unwrapKey().map(decoded::get);
    }
}
