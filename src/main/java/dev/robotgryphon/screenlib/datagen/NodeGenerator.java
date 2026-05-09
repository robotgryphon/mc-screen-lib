package dev.robotgryphon.screenlib.datagen;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.types.NodeDefinition;
import dev.robotgryphon.screenlib.types.PortDefinition;
import dev.robotgryphon.screenlib.types.PropertyType;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;

import java.util.List;
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

    /** Resolves a built-in property type by namespace+path so node defs read clearly. */
    private static Holder<PropertyType<?>> propertyType(HolderGetter<PropertyType<?>> lookup,
                                                        String namespace, String path) {
        return lookup.getOrThrow(ResourceKey.create(
                PropertyType.REGISTRY_KEY, Identifier.fromNamespaceAndPath(namespace, path)));
    }
    //endregion

    private static void addCoreNodeTypes(BootstrapContext<NodeDefinition> ctx) {
        HolderGetter<PropertyType<?>> types = ctx.lookup(PropertyType.REGISTRY_KEY);

        Holder<PropertyType<?>> blockPos     = propertyType(types, "minecraft", "block_pos");
        Holder<PropertyType<?>> direction    = propertyType(types, "minecraft", "direction");
        Holder<PropertyType<?>> intType      = propertyType(types, "minecraft", "int");
        Holder<PropertyType<?>> itemHandler  = propertyType(types, ScreenLib.MOD_ID, "item_handler");

        // Block Position — a pure source: the position holder plus its scalar parts.
        ctx.register(nodeType("block_position"), new NodeDefinition(
                List.of(),
                List.of(
                        new PortDefinition("Position", blockPos),
                        new PortDefinition("x", intType),
                        new PortDefinition("y", intType),
                        new PortDefinition("z", intType)
                )
        ));

        // Direction — exposes each axis-aligned direction (and a wildcard) as a
        // separate output, the way the mockup lays out a "constants" node.
        ctx.register(nodeType("direction"), new NodeDefinition(
                List.of(),
                List.of(
                        new PortDefinition("Any", direction),
                        new PortDefinition("Up", direction),
                        new PortDefinition("Down", direction),
                        new PortDefinition("North", direction),
                        new PortDefinition("South", direction),
                        new PortDefinition("West", direction),
                        new PortDefinition("East", direction)
                )
        ));

        // Resource Access (Items) — given a block position and a side, hand back
        // the item-handler exposed by that face of the block.
        ctx.register(nodeType("resource_access_items"), new NodeDefinition(
                List.of(
                        new PortDefinition("Position", blockPos),
                        new PortDefinition("Side", direction)
                ),
                List.of(
                        new PortDefinition("Storage", itemHandler)
                )
        ));

        // Tree Cutter Upgrade — a sink-style node: it consumes two item handles
        // (where to pull tools from, where to push drops to) and produces nothing.
        ctx.register(nodeType("tree_cutter_upgrade"), new NodeDefinition(
                List.of(
                        new PortDefinition("Tools", itemHandler),
                        new PortDefinition("Drops", itemHandler)
                ),
                List.of()
        ));
    }
}
