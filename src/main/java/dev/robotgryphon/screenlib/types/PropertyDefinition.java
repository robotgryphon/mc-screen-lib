package dev.robotgryphon.screenlib.types;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import dev.robotgryphon.screenlib.ScreenLib;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.resources.ResourceKey;

import java.util.List;
import java.util.Optional;

/**
 * Registered definition of a typed value a node can carry — either flowing
 * through a {@code PortDefinition} as an input/output, or sitting inside
 * the node body as a configurable property. A single
 * {@code PropertyDefinition} bundles everything the rest of the codebase
 * needs about that value:
 *
 * <ul>
 *   <li>{@link #codec} — wire format, used by persistence and by the
 *       per-property "current value" lookup ({@link
 *       dev.robotgryphon.screenlib.graph.Canvas} encodes/decodes through
 *       it).</li>
 *   <li>{@link #color} — the diamond color used by the renderer for any
 *       port that carries this type, and the row-tint scheme inside the
 *       node body.</li>
 *   <li>{@link #displayName} — the human-facing name of the type, surfaced
 *       in the Add Node filter chips and similar UI.</li>
 *   <li>{@link #defaultValueRaw} — optional default for a property of
 *       this definition; ports that reference the same definition just
 *       ignore the default. Stored as a codec-agnostic {@link Dynamic}
 *       so the field doesn't have to be re-typed against {@link #codec}
 *       at registration sites.</li>
 *   <li>{@link #allowedValues} — optional fixed set of valid values. When
 *       present, an editor renders the property as a dropdown rather
 *       than a free-form input; the values in the list are the only ones
 *       the user can pick. Ports ignore this — it's purely an editor hint
 *       for properties.</li>
 * </ul>
 *
 * <p>Earlier versions split this across two records: a registered
 * {@code PropertyType} (codec + color + displayName) and a per-node
 * {@code PropertyDefinition} (name + type ref + default). The split was
 * paying for very little — anything that wanted the type info had to
 * thread a holder through anyway — so they collapse here. Where a
 * per-node entry needs a local label or key (the property name on a
 * specific node), that's just a string on the surrounding
 * {@link PortDefinition}.
 *
 * <p>Per-property defaults that aren't shared globally mean registering a
 * new {@code PropertyDefinition} per case (e.g., the sampler's
 * {@code seed} gets its own entry with its own default rather than
 * reusing the generic {@code minecraft:int}).
 */
public record PropertyDefinition<T>(Codec<T> codec,
                                    int color,
                                    Component displayName,
                                    Optional<T> defaultValueRaw,
                                    Optional<List<T>> allowedValues) {

    /** Convenience: no default, no fixed value set. Used for generic port types like minecraft:int. */
    public PropertyDefinition(Codec<T> codec, int color, Component displayName) {
        this(codec, color, displayName, Optional.empty(), Optional.empty());
    }

    /**
     * Convenience: with default, no fixed value set. The common case for
     * numeric properties whose value is free-form within the type's range.
     */
    public PropertyDefinition(Codec<T> codec, int color, Component displayName, Optional<T> defaultValueRaw) {
        this(codec, color, displayName, defaultValueRaw, Optional.empty());
    }

    /** Registry key for this registry. {@link ScreenLib#PROPERTY_DEFINITIONS} backs it. */
    public static final ResourceKey<Registry<PropertyDefinition<?>>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ScreenLib.id("property_definitions"));

    /** Convenience constant — used for ports whose type doesn't carry a strong color. */
    public static final int DEFAULT_COLOR = 0xFF8FA0FF;

    /**
     * Codec used for the {@link Holder} reference that
     * {@link PortDefinition} carries — resolves to an entry in the
     * {@link #REGISTRY_KEY} registry by id at codec-eval time, so
     * JSON entries read like {@code "type": "minecraft:int"}.
     *
     * <p>There is no direct codec for {@code PropertyDefinition} itself
     * because the {@link #codec()} field isn't reflectively
     * serializable — entries are registered through code (mod or
     * built-in), not loaded from datapack JSON. Adding a datapack-loaded
     * codec would require a dispatch-by-type-id scheme, which the
     * current call sites don't need.
     */
    public static final Codec<Holder<PropertyDefinition<?>>> HOLDER_CODEC =
            RegistryFixedCodec.create(REGISTRY_KEY);
}
