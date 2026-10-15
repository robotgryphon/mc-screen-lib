package dev.robotgryphon.testmod.nodes;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.graph.nodes.properties.DataKey;
import dev.robotgryphon.screenlib.graph.nodes.schema.NodeDefinitionBuilder;
import dev.robotgryphon.screenlib.graph.nodes.schema.NodeDefinition;
import dev.robotgryphon.screenlib.graph.types.ItemHandlerAccess;
import dev.robotgryphon.testmod.TestMod;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public final class TreeCutterUpgradeNode {
    private TreeCutterUpgradeNode() {}

    public static final ResourceKey<NodeDefinition> KEY = ResourceKey.create(NodeDefinition.REGISTRY_KEY,
            Identifier.fromNamespaceAndPath(TestMod.MOD_ID, "tree_cutter_upgrade"));

    public static final DataKey<ItemHandlerAccess> TOOLS = DataKey.required("tools", ScreenLib.ITEM_HANDLER_DATA_TYPE);
    public static final DataKey<ItemHandlerAccess> DROPS = DataKey.required("drops", ScreenLib.ITEM_HANDLER_DATA_TYPE);

    public static NodeDefinition definition() {
        return new NodeDefinitionBuilder()
                .hidden()
                .addProperty(TOOLS.name(), prop -> prop.type(TOOLS.type()).input())
                .addProperty(DROPS.name(), prop -> prop.type(DROPS.type()).input())
                .build();
    }
}
