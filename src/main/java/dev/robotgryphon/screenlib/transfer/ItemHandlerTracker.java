package dev.robotgryphon.screenlib.transfer;

import dev.robotgryphon.screenlib.graph.types.ItemHandlerAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ItemHandlerTracker {
    private final MinecraftServer server;
    private final Map<ItemHandlerAccess, List<ItemHandlerSlot>> slots = new HashMap<>();
    private final Set<ItemHandlerAccess> available = new HashSet<>();

    public ItemHandlerTracker(MinecraftServer server) {
        this.server = server;
    }

    public boolean refresh() {
        boolean changed = false;
        for (var entry : this.slots.entrySet()) {
            changed |= track(entry.getKey(), entry.getValue());
        }
        return changed;
    }

    public Optional<ItemHandlerSnapshot> snapshot(ItemHandlerAccess access, int visibleSlots) {
        List<ItemHandlerSlot> tracked = this.slots.get(access);
        if (tracked == null) {
            tracked = new ArrayList<>();
            this.slots.put(access, tracked);
            track(access, tracked);
        }
        if (!this.available.contains(access)) {
            return Optional.empty();
        }

        int total = 0;
        List<ItemStack> visible = new ArrayList<>(Math.min(visibleSlots, tracked.size()));
        for (int slot = 0; slot < tracked.size(); slot++) {
            ItemStack stack = tracked.get(slot).tracked();
            total += stack.getCount();
            if (slot < visibleSlots) {
                visible.add(stack);
            }
        }
        return Optional.of(new ItemHandlerSnapshot(tracked.size(), total, visible));
    }

    private boolean track(ItemHandlerAccess access, List<ItemHandlerSlot> tracked) {
        ResourceHandler<ItemResource> handler = handler(access);
        boolean changed = handler == null
                ? this.available.remove(access)
                : this.available.add(access);
        int size = handler == null ? 0 : handler.size();
        while (tracked.size() > size) {
            tracked.remove(tracked.size() - 1);
            changed = true;
        }
        while (tracked.size() < size) {
            tracked.add(new ItemHandlerSlot(handler, tracked.size()));
            changed = true;
        }
        for (ItemHandlerSlot slot : tracked) {
            changed |= slot.track();
        }
        return changed;
    }

    private @Nullable ResourceHandler<ItemResource> handler(ItemHandlerAccess access) {
        var level = this.server.getLevel(access.level());
        return level == null ? null : level.getCapability(Capabilities.Item.BLOCK, access.pos(), access.side());
    }
}
