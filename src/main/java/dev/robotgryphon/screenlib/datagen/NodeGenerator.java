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

public class NodeGenerator extends DatapackBuiltinEntriesProvider {

    //region Setup
    private static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(NodeDefinition.REGISTRY_KEY, NodeGenerator::addCoreNodeTypes);

    public NodeGenerator(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {
        super(packOutput, registries, BUILDER, Set.of(ScreenLib.MOD_ID));
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
                                                          String namespace, String path) {
        var key = ResourceKey.create(
                PropertyDefinition.REGISTRY_KEY, Identifier.fromNamespaceAndPath(namespace, path));
        return lookup.getOrThrow(key);
    }
    //endregion

    private static void addCoreNodeTypes(BootstrapContext<NodeDefinition> ctx) {
        var defs = ctx.lookup(PropertyDefinition.REGISTRY_KEY);

        // Generic types — used for ports (no defaults needed).
        var blockPos    = propertyDef(defs, "minecraft", "block_pos");
        var direction   = propertyDef(defs, "minecraft", "direction");
        var intType     = propertyDef(defs, "minecraft", "int");
        var itemHandler = propertyDef(defs, ScreenLib.MOD_ID, "item_handler");

        // Block Position — a pure source: the position holder plus its scalar parts.
        ctx.register(nodeType("block_position"), new NodeBuilder()
                .addOutput("Position", blockPos)
                .addOutput("x", intType)
                .addOutput("y", intType)
                .addOutput("z", intType)
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

        // Sampler — exercises the property-row layout the way the KSampler
        // mockup does. Each property references a distinct registered
        // PropertyDefinition (registered in ScreenLib's static block) so its
        // default lands without needing a per-use override path on
        // NodeBuilder. The "type" registrations carry the codec/color/
        // displayName/default; this node definition just names them.
        var seedDef     = propertyDef(defs, ScreenLib.MOD_ID, "sampler/seed");
        var stepsDef    = propertyDef(defs, ScreenLib.MOD_ID, "sampler/steps");
        var cfgDef      = propertyDef(defs, ScreenLib.MOD_ID, "sampler/cfg");
        var samplerDef  = propertyDef(defs, ScreenLib.MOD_ID, "sampler/sampler_name");
        var schedDef    = propertyDef(defs, ScreenLib.MOD_ID, "sampler/scheduler");
        var denoiseDef  = propertyDef(defs, ScreenLib.MOD_ID, "sampler/denoise");

        ctx.register(nodeType("sampler"), new NodeBuilder()
                .addProperty("seed", seedDef)
                .addProperty("steps", stepsDef)
                .addProperty("cfg", cfgDef)
                .addProperty("sampler_name", samplerDef)
                .addProperty("scheduler", schedDef)
                .addProperty("denoise", denoiseDef)
                .build());

        // Boolean primitive — single typed value paired with an output
        // port that advertises the node's role ("BOOLEAN") to the canvas.
        // The {@code value} property uses the dedicated
        // {@code boolean_primitive/value} definition so the slot ships
        // with a {@code false} default; the {@code BOOLEAN} output uses
        // the generic {@code bool} type for wire compatibility with any
        // other boolean port, linked to {@code value} so wires from it
        // carry the property's current state.
        var boolType    = propertyDef(defs, "minecraft", "bool");
        var boolValDef  = propertyDef(defs, ScreenLib.MOD_ID, "boolean_primitive/value");
        ctx.register(nodeType("boolean"), new NodeBuilder()
                .addOutput("BOOLEAN", boolType, "value")
                .addProperty("value", boolValDef)
                .build());
    }
}
