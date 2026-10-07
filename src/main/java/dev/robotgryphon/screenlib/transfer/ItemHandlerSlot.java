package dev.robotgryphon.screenlib.transfer;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

public class ItemHandlerSlot extends ResourceHandlerSlot {
    private ItemStack tracked = ItemStack.EMPTY;

    public ItemHandlerSlot(ResourceHandler<ItemResource> handler, int index) {
        super(handler, (slot, resource, amount) -> {}, index, 0, 0);
    }

    public ItemStack tracked() {
        return this.tracked;
    }

    public boolean track() {
        ItemStack current = this.getItem();
        if (ItemStack.matches(this.tracked, current)) {
            return false;
        }
        this.tracked = current;
        return true;
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(Player player) {
        return false;
    }
}
