package dev.robotgryphon.screenlib.transfer;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.List;

public record ItemHandlerContents(List<Entry> entries) {
    public static final long INFINITE = Long.MAX_VALUE;

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemHandlerContents> STREAM_CODEC =
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()).map(ItemHandlerContents::new, ItemHandlerContents::entries);

    public int size() {
        return this.entries.size();
    }

    public Entry get(int slot) {
        return this.entries.get(slot);
    }

    public record Entry(ItemResource resource, long amount) {
        public static final Entry EMPTY = new Entry(ItemResource.EMPTY, 0);

        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ItemResource.STREAM_CODEC, Entry::resource,
                ByteBufCodecs.VAR_LONG, Entry::amount,
                Entry::new);

        public boolean isEmpty() {
            return this.resource.isEmpty() || this.amount <= 0;
        }

        public boolean isInfinite() {
            return this.amount == INFINITE || this.amount == Integer.MAX_VALUE;
        }
    }
}
