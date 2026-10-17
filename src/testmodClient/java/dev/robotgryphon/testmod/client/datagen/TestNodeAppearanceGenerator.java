package dev.robotgryphon.testmod.client.datagen;

import dev.robotgryphon.screenlib.nodes.api.render.NodeStateRenderers;
import dev.robotgryphon.screenlib.nodes.api.render.PropertyValueRenderers;
import dev.robotgryphon.screenlib.nodes.api.appearance.NodeAppearance;
import dev.robotgryphon.screenlib.nodes.api.appearance.PropertyAppearance;
import dev.robotgryphon.screenlib.nodes.api.appearance.NodeAppearanceProvider;
import dev.robotgryphon.testmod.common.TestMod;
import dev.robotgryphon.testmod.common.nodes.TreeCutterUpgradeNode;
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
                .widget(NodeStateRenderers.BLOCK_PREVIEW, "state")
                .build());

        this.add(TreeCutterUpgradeNode.KEY, NodeAppearance.builder()
                .property(TreeCutterUpgradeNode.TOOLS.name(), p -> p.withRenderer(PropertyValueRenderers.ITEM_HANDLER_BLOCK))
                .property(TreeCutterUpgradeNode.DROPS.name(), p -> p.withRenderer(PropertyValueRenderers.ITEM_HANDLER_BLOCK))
                .build());
    }
}
