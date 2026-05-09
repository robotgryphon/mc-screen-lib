package dev.robotgryphon.screenlib.datagen;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.types.NodeDefinition;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class NodeGenerator extends DatapackBuiltinEntriesProvider {

    //region Setup
    private static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(NodeDefinition.REGISTRY_KEY, NodeGenerator::addCoreNodeTypes);

    static Holder.Reference<NodeDefinition> BLOCK_POS;

    public NodeGenerator(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {
        super(packOutput, registries, BUILDER, Set.of(ScreenLib.MOD_ID));
    }

    @SuppressWarnings("SameParameterValue")
    private static ResourceKey<NodeDefinition> nodeType(String name) {
        return ResourceKey.create(NodeDefinition.REGISTRY_KEY, ScreenLib.id(name));
    }
    //endregion

    private static void addCoreNodeTypes(BootstrapContext<NodeDefinition> ctx) {

        BLOCK_POS = ctx.register(nodeType("block_position"),
                new NodeDefinition(
                        List.of(),
                        List.of("x", "y", "z"),
                        List.of("x", "y", "z")
                ));
    }
}
