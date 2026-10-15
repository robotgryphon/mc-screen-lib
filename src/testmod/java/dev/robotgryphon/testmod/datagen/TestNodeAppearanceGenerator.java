package dev.robotgryphon.testmod.datagen;

import dev.robotgryphon.screenlib.client.appearance.NodeAppearance;
import dev.robotgryphon.screenlib.client.appearance.PropertyAppearance;
import dev.robotgryphon.screenlib.client.render.NodeWidgets;
import dev.robotgryphon.screenlib.client.render.PropertyRenderers;
import dev.robotgryphon.screenlib.datagen.NodeAppearanceProvider;
import dev.robotgryphon.testmod.TestMod;
import dev.robotgryphon.testmod.nodes.TreeCutterUpgradeNode;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;

import java.util.concurrent.CompletableFuture;

public class TestNodeAppearanceGenerator extends NodeAppearanceProvider {
    public TestNodeAppearanceGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, TestMod.MOD_ID);
    }

    @Override
    protected void gather() {
        this.add(TestNodeGenerator.GET_BLOCK, NodeAppearance.builder()
                .property("state", PropertyAppearance::withHiddenLabel)
                .widget(NodeWidgets.BLOCK_PREVIEW.key(), "state")
                .build());

        this.add(TreeCutterUpgradeNode.KEY, NodeAppearance.builder()
                .property(TreeCutterUpgradeNode.TOOLS.name(), p -> p.withRenderer(PropertyRenderers.ITEM_HANDLER_BLOCK.key()))
                .property(TreeCutterUpgradeNode.DROPS.name(), p -> p.withRenderer(PropertyRenderers.ITEM_HANDLER_BLOCK.key()))
                .build());
    }
}
