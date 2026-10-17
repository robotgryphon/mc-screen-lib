package dev.robotgryphon.testmod.common;

import dev.robotgryphon.screenlib.nodes.api.ScreenLibCapabilities;
import dev.robotgryphon.testmod.common.block.TreeHarvesterBlock;
import dev.robotgryphon.testmod.common.block.TreeHarvesterBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TestModBlocks {
    private TestModBlocks() {}

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TestMod.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TestMod.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TestMod.MOD_ID);

    public static final DeferredBlock<TreeHarvesterBlock> TREE_HARVESTER =
            BLOCKS.registerBlock("tree_harvester", TreeHarvesterBlock::new, properties -> properties
                    .mapColor(MapColor.COLOR_GREEN)
                    .strength(1.5F)
                    .sound(SoundType.GLASS)
                    .noOcclusion());

    public static final DeferredItem<BlockItem> TREE_HARVESTER_ITEM = ITEMS.registerSimpleBlockItem(TREE_HARVESTER);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TreeHarvesterBlockEntity>> TREE_HARVESTER_ENTITY =
            BLOCK_ENTITIES.register("tree_harvester", () -> new BlockEntityType<>(
                    TreeHarvesterBlockEntity::new, TREE_HARVESTER.get()));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(TestModBlocks::addToCreativeTab);
        modBus.addListener(TestModBlocks::registerCapabilities);
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(TREE_HARVESTER_ITEM);
        }
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(ScreenLibCapabilities.CANVAS, TREE_HARVESTER_ENTITY.get(),
                (harvester, context) -> harvester.getCanvas());
    }
}
