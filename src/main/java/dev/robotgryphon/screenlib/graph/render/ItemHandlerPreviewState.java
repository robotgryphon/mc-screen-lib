package dev.robotgryphon.screenlib.graph.render;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public record ItemHandlerPreviewState(int slotCount, int totalItems, List<ItemStack> stacks) {
}
