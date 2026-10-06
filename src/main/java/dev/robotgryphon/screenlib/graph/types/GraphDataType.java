package dev.robotgryphon.screenlib.graph.types;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Dynamic;
import dev.robotgryphon.screenlib.ScreenLib;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.resources.ResourceKey;

import java.util.Collection;
import java.util.Optional;

/// Registered definition of a typed value a node can carry — either flowing
/// through a `Port` as an input/output, or sitting inside
/// the node as a configurable property.
///
///   - [#codec] — wire format, used by persistence and by the
///     per-property "current value" lookup (
///     [dev.robotgryphon.screenlib.graph.Canvas] encodes/decodes through
///     it).
///   - [#color] — the diamond color used by the renderer for any
///     port that carries this type, and the row-tint scheme inside the
///     node body.
///   - [#defaultValueRaw] — optional default for a property of
///     this definition; ports that reference the same definition just
///     ignore the default. Stored as a codec-agnostic [ Dynamic ]
///     so the field doesn't have to be re-typed against [#codec]
///     at registration sites.
///   - [#allowedValues] — optional fixed set of valid values. When
///     present, an editor renders the property as a dropdown rather
///     than a free-form input; the values in the list are the only ones
///     the user can pick. Ports ignore this — it's purely an editor hint
///     for properties.
public interface GraphDataType<T> {

    /** Registry key for this registry. {@link ScreenLib#PROPERTY_DEFINITIONS} backs it. */
    ResourceKey<Registry<GraphDataType<?>>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ScreenLib.id("property_definitions"));

    Codec<Holder<GraphDataType<?>>> HOLDER_CODEC = RegistryCodecs.holder(REGISTRY_KEY);

    /** Convenience constant — used for ports whose type doesn't carry a strong color. */
    int DEFAULT_COLOR = 0xFF8FA0FF;

    Class<T> dataType();

    default int color() {
        return DEFAULT_COLOR;
    }

    Codec<T> codec();

    default Optional<T> defaultValueRaw() {
        return Optional.empty();
    }
    
    default Optional<Collection<T>> allowedValues() {
        return Optional.empty();
    }
}
