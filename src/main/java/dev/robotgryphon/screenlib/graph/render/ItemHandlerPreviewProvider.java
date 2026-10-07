package dev.robotgryphon.screenlib.graph.render;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.graph.computed.ComputeContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemHandlerPreviewProvider implements NodeRenderProvider<ItemHandlerPreviewState> {
    public static final int COLUMNS = 9;
    public static final int ROWS = 3;
    public static final int SLOT_SIZE = 18;
    public static final int INFO_HEIGHT = 12;

    private static final StreamCodec<RegistryFriendlyByteBuf, ItemHandlerPreviewState> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ItemHandlerPreviewState::slotCount,
            ByteBufCodecs.VAR_INT,
            ItemHandlerPreviewState::totalItems,
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()),
            ItemHandlerPreviewState::stacks,
            ItemHandlerPreviewState::new
    );

    @Override
    public int width() {
        return COLUMNS * SLOT_SIZE;
    }

    @Override
    public int height() {
        return ROWS * SLOT_SIZE + INFO_HEIGHT;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, ItemHandlerPreviewState> codec() {
        return CODEC;
    }

    @Override
    public Optional<ItemHandlerPreviewState> extract(ComputeContext context) {
        var server = context.server();
        var access = context.get("storage", ScreenLib.ITEM_HANDLER_DATA_TYPE.value());
        if (server == null || access.isEmpty()) {
            return Optional.empty();
        }
        var level = server.getLevel(access.get().level());
        if (level == null) {
            return Optional.empty();
        }
        var handler = level.getCapability(Capabilities.Item.BLOCK, access.get().pos(), access.get().side());
        if (handler == null) {
            return Optional.empty();
        }

        int total = 0;
        List<ItemStack> stacks = new ArrayList<>();
        for (int slot = 0; slot < handler.size(); slot++) {
            ItemStack stack = handler.getResource(slot).toStack(handler.getAmountAsInt(slot));
            total += stack.getCount();
            if (slot < COLUMNS * ROWS) {
                stacks.add(stack);
            }
        }
        return Optional.of(new ItemHandlerPreviewState(handler.size(), total, stacks));
    }
}
