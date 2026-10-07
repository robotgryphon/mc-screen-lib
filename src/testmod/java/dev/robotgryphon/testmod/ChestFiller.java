package dev.robotgryphon.testmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public final class ChestFiller {
    private ChestFiller() {}

    public static void fill(ServerLevel level, BlockPos pos, RandomSource random) {
        level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);
        if (!(level.getBlockEntity(pos) instanceof Container container)) {
            return;
        }
        List<Item> items = BuiltInRegistries.ITEM.stream().filter(item -> item != Items.AIR).toList();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (random.nextInt(3) == 0) {
                continue;
            }
            Item item = items.get(random.nextInt(items.size()));
            container.setItem(slot, new ItemStack(item, 1 + random.nextInt(item.getDefaultMaxStackSize())));
        }
        container.setChanged();
    }
}
