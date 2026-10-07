package dev.robotgryphon.screenlib.graph.render;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.graph.computed.ComputeContext;
import dev.robotgryphon.screenlib.transfer.ItemHandlerSnapshot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

public class ItemHandlerPreviewProvider implements NodeRenderProvider<ItemHandlerSnapshot> {
    public static final int COLUMNS = 9;
    public static final int ROWS = 3;
    public static final int SLOT_SIZE = 18;
    public static final int INFO_HEIGHT = 12;

    @Override
    public int width() {
        return COLUMNS * SLOT_SIZE;
    }

    @Override
    public int height() {
        return ROWS * SLOT_SIZE + INFO_HEIGHT;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, ItemHandlerSnapshot> codec() {
        return ItemHandlerSnapshot.STREAM_CODEC;
    }

    @Override
    public Optional<ItemHandlerSnapshot> extract(ComputeContext context) {
        return context.get("storage", ScreenLib.ITEM_HANDLER_DATA_TYPE.value())
                .flatMap(access -> context.itemHandler(access, COLUMNS * ROWS));
    }
}
