package dev.robotgryphon.screenlib.transfer;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record ItemHandlerSnapshot(int slotCount, int totalItems, List<ItemStack> stacks) {
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemHandlerSnapshot> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ItemHandlerSnapshot::slotCount,
            ByteBufCodecs.VAR_INT,
            ItemHandlerSnapshot::totalItems,
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()),
            ItemHandlerSnapshot::stacks,
            ItemHandlerSnapshot::new
    );
}
