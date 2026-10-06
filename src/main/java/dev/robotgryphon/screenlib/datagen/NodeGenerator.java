package dev.robotgryphon.screenlib.datagen;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.types.NodeBuilder;
import dev.robotgryphon.screenlib.types.NodeDefinition;
import dev.robotgryphon.screenlib.types.PropertyDefinition;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class NodeGenerator {

    //region Setup
    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(NodeDefinition.REGISTRY_KEY, NodeGenerator::addCoreNodeTypes);

    public static DatapackBuiltinEntriesProvider create(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {
        return DatapackBuiltinEntriesProvider.forReloadableLayer(packOutput, "NodeGenerator",
                registries, registries, BUILDER, Set.of(ScreenLib.MOD_ID));
    }

    private static ResourceKey<NodeDefinition> nodeType(String name) {
        return ResourceKey.create(NodeDefinition.REGISTRY_KEY, ScreenLib.id(name));
    }

    /**
     * Resolves a built-in property definition by namespace+path so node
     * defs read clearly. The combined {@link PropertyDefinition} registry
     * now covers both the "what's the type" use (for ports) and the
     * "what's the type and what's its default" use (for properties).
     */
    private static Holder<PropertyDefinition<?>> propertyDef(HolderGetter<PropertyDefinition<?>> lookup,
                                                             Identifier id) {
        var key = ResourceKey.create(
                PropertyDefinition.REGISTRY_KEY, id);
        return lookup.getOrThrow(key);
    }
    //endregion

    private static void addCoreNodeTypes(BootstrapContext<NodeDefinition> ctx) {
        var defs = ctx.lookup(PropertyDefinition.REGISTRY_KEY);

        // Generic types — used for ports (no defaults needed).
        var boolType = propertyDef(defs, Identifier.withDefaultNamespace("bool"));
        var intType = propertyDef(defs, Identifier.withDefaultNamespace("int"));
        var floatType = propertyDef(defs, Identifier.withDefaultNamespace("float"));

        var blockPos = propertyDef(defs, Identifier.withDefaultNamespace("block_pos"));
        var direction = propertyDef(defs, Identifier.withDefaultNamespace("direction"));

        var itemHandler = propertyDef(defs, Identifier.fromNamespaceAndPath(ScreenLib.MOD_ID, "item_handler"));

        // Integer primitive — single typed value
        ctx.register(nodeType("integer"), new NodeBuilder()
                .addOutput("INTEGER", intType, "value")
                .addProperty("value", intType)
                .build());

        // Boolean primitive — single typed value
        ctx.register(nodeType("float"), new NodeBuilder()
                .addOutput("FLOAT", floatType, "value")
                .addProperty("value", floatType)
                .build());

        // Boolean primitive — single typed values
        ctx.register(nodeType("boolean"), new NodeBuilder()
                .addOutput("BOOLEAN", boolType, "value")
                .addProperty("value", boolType)
                .build());


        // Block Position — a pure source: the position holder plus its scalar parts.
        ctx.register(nodeType("block_position"), new NodeBuilder()
                .addOutput("Position", blockPos)
                .addProperty("x", intType)
                .addProperty("y", intType)
                .addProperty("z", intType)
                .build());

        // Direction — pick one of the six axis-aligned directions from a
        // dropdown and emit it through a single typed output. Both the
        // output port and the property's implicit input port reference
        // the same {@code minecraft:direction} {@link PropertyDefinition}
        // (which carries the allowed-value list + default registered in
        // {@link ScreenLib}), so a wire from one Direction node's output
        // to another Direction node's {@code value} input round-trips
        // without a type mismatch. The output's
        // {@link PortDefinition#linkedProperty} = {@code "value"} relays
        // the property's current pick through to downstream consumers.
        ctx.register(nodeType("direction"), new NodeBuilder()
                .addOutput("direction", direction, "value")
                .addProperty("value", direction)
                .build());

        // Resource Access (Items) — given a block position (and an optional
        // direction the user picks from the property dropdown), hand back
        // the item-handler exposed by that face of the block. The Side
        // value lives inside the node body as a {@code minecraft:direction}
        // property (dropdown over the six concrete directions) rather than
        // as a left-side input port, since most users want to commit to a
        // direction once and forget about it; wires can still drive the
        // value through the property's implicit input port when needed.
        ctx.register(nodeType("resource_access_items"), new NodeBuilder()
                .addInput("Position", blockPos)
                .addProperty("Side", direction)
                .addOutput("Storage", itemHandler)
                .build());

        // Tree Cutter Upgrade — a sink-style node: it consumes two item handles
        // (where to pull tools from, where to push drops to) and produces nothing.
        // Both inputs are required — the cutter has nowhere to source tools and
        // nowhere to deposit drops without them, so leaving either unwired is
        // a configuration error that validation should surface.
        ctx.register(nodeType("tree_cutter_upgrade"), new NodeBuilder()
                .addInput("Tools", itemHandler)
                .addInput("Drops", itemHandler)
                .build());


    }
}
