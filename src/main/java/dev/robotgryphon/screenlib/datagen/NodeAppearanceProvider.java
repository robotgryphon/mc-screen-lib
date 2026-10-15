package dev.robotgryphon.screenlib.datagen;

import dev.robotgryphon.screenlib.client.appearance.NodeAppearance;
import dev.robotgryphon.screenlib.client.appearance.NodeAppearances;
import dev.robotgryphon.screenlib.graph.nodes.schema.NodeDefinition;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.common.data.JsonCodecProvider;

import java.util.concurrent.CompletableFuture;

public abstract class NodeAppearanceProvider extends JsonCodecProvider<NodeAppearance> {
    protected NodeAppearanceProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, String modId) {
        super(output, PackOutput.Target.RESOURCE_PACK, NodeAppearances.DIRECTORY, NodeAppearance.CODEC, lookupProvider, modId);
    }

    protected void add(ResourceKey<NodeDefinition> node, NodeAppearance appearance) {
        this.unconditional(node.identifier(), appearance);
    }
}
