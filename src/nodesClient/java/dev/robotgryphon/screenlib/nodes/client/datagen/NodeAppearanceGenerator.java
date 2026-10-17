package dev.robotgryphon.screenlib.nodes.client.datagen;

import dev.robotgryphon.screenlib.nodes.api.appearance.NodeAppearanceProvider;
import dev.robotgryphon.screenlib.core.ScreenLib;
import dev.robotgryphon.screenlib.nodes.api.appearance.NodeAppearance;
import dev.robotgryphon.screenlib.nodes.api.appearance.PropertyAppearance;
import dev.robotgryphon.screenlib.nodes.client.render.NodeWidgets;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;

import java.util.concurrent.CompletableFuture;

public class NodeAppearanceGenerator extends NodeAppearanceProvider {
    public NodeAppearanceGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, ScreenLib.MOD_ID);
    }

    @Override
    protected void gather() {
        this.add(NodeGenerator.RESOURCE_ACCESS_ITEMS, NodeAppearance.builder()
                .property("storage", PropertyAppearance::withHiddenLabel)
                .widget(NodeWidgets.ITEM_HANDLER_PREVIEW.key(), "storage")
                .build());
    }
}
