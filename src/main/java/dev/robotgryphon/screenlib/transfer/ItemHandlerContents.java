package dev.robotgryphon.screenlib.transfer;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record ItemHandlerContents(List<ItemStack> stacks) {
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemHandlerContents> STREAM_CODEC =
            ItemStack.OPTIONAL_LIST_STREAM_CODEC.map(ItemHandlerContents::new, ItemHandlerContents::stacks);

    public int size() {
        return this.stacks.size();
    }

    public ItemStack get(int slot) {
        return this.stacks.get(slot);
    }
}
