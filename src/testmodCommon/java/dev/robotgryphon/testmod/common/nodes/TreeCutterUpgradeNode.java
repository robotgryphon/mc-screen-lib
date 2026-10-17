package dev.robotgryphon.testmod.common.nodes;

import dev.robotgryphon.screenlib.graph.schema.types.GraphDataTypes;
import dev.robotgryphon.screenlib.graph.schema.properties.DataKey;
import dev.robotgryphon.screenlib.graph.schema.NodeDefinitionBuilder;
import dev.robotgryphon.screenlib.graph.schema.NodeDefinition;
import dev.robotgryphon.screenlib.graph.schema.types.ItemHandlerAccess;
import dev.robotgryphon.testmod.common.TestMod;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public final class TreeCutterUpgradeNode {
    private TreeCutterUpgradeNode() {}

    public static final ResourceKey<NodeDefinition> KEY = ResourceKey.create(NodeDefinition.REGISTRY_KEY,
            Identifier.fromNamespaceAndPath(TestMod.MOD_ID, "tree_cutter_upgrade"));

    public static final DataKey<ItemHandlerAccess> TOOLS = DataKey.required("tools", GraphDataTypes.ITEM_HANDLER);
    public static final DataKey<ItemHandlerAccess> DROPS = DataKey.required("drops", GraphDataTypes.ITEM_HANDLER);

    public static NodeDefinition definition() {
        return new NodeDefinitionBuilder()
                .hidden()
                .addProperty(TOOLS.name(), prop -> prop.type(TOOLS.type()).input())
                .addProperty(DROPS.name(), prop -> prop.type(DROPS.type()).input())
                .build();
    }
}
