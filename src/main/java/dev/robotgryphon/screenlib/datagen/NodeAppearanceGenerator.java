package dev.robotgryphon.screenlib.datagen;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.client.appearance.NodeAppearance;
import dev.robotgryphon.screenlib.client.appearance.PropertyAppearance;
import dev.robotgryphon.screenlib.client.render.NodeWidgets;
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
