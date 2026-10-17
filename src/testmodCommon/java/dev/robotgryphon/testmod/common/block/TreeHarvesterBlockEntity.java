package dev.robotgryphon.testmod.common.block;

import dev.robotgryphon.screenlib.nodes.api.canvas.CanvasSupplier;
import dev.robotgryphon.screenlib.nodes.api.canvas.CanvasDefinition;
import dev.robotgryphon.screenlib.nodes.api.RequiredNodeReference;
import dev.robotgryphon.screenlib.nodes.api.state.CanvasState;
import dev.robotgryphon.screenlib.graph.schema.types.ItemHandlerAccess;
import dev.robotgryphon.testmod.common.TestModBlocks;
import dev.robotgryphon.testmod.common.nodes.TreeCutterUpgradeNode;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.resource.ResourceStack;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TreeHarvesterBlockEntity extends BlockEntity {

    public static final int SCAN_TICKS = 100;
    public static final int MAX_SAPLINGS = 10;
    public static final int SAPLING_BUFFER = 500;
    public static final int RADIUS = 32;

    private static final int SCAN_SIZE = 2 * RADIUS + 1;
    private static final int SCAN_VOLUME = SCAN_SIZE * SCAN_SIZE * SCAN_SIZE;
    private static final int SCAN_PER_TICK = (SCAN_VOLUME + SCAN_TICKS - 1) / SCAN_TICKS;

    private final CanvasDefinition canvas = new CanvasDefinition();

    private final RequiredNodeReference treeCutter = this.canvas.require(TreeCutterUpgradeNode.KEY);

    private final CanvasSupplier canvasSupplier = CanvasSupplier.of(this.canvas)
            .watch(this.treeCutter, TreeCutterUpgradeNode.TOOLS, this::onToolsStorageChanged)
            .watch(this.treeCutter, TreeCutterUpgradeNode.DROPS, this::onDropsStorageChanged)
            .onChanged(this::setChanged);

    private @Nullable BlockCapabilityCache<ResourceHandler<ItemResource>, @Nullable Direction> tools;
    private @Nullable BlockCapabilityCache<ResourceHandler<ItemResource>, @Nullable Direction> drops;

    private final ItemStacksResourceHandler axe = new ItemStacksResourceHandler(1) {
        @Override
        public boolean isValid(int index, ItemResource resource) {
            return isAxe(resource);
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            setChanged();
        }
    };

    private final List<BlockPos> targets = new ArrayList<>();
    private int scanTick;
    private int scanIndex;
    private int planted;

    private static final Codec<ResourceStack<ItemResource>> SAPLING_CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemResource.OPTIONAL_CODEC.fieldOf("resource").forGetter(ResourceStack::resource),
            Codec.INT.optionalFieldOf("amount", 0).forGetter(ResourceStack::amount)
    ).apply(i, ResourceStack::new));

    private final ResourceStacksResourceHandler<ItemResource> saplings =
            new ResourceStacksResourceHandler<>(1, ItemResource.EMPTY, SAPLING_CODEC) {
                @Override
                public boolean isValid(int index, ItemResource resource) {
                    return isSapling(resource);
                }

                @Override
                protected int getCapacity(int index, ItemResource resource) {
                    return SAPLING_BUFFER;
                }

                @Override
                protected void onContentsChanged(int index, ResourceStack<ItemResource> previousContents) {
                    setChanged();
                }
            };

    public TreeHarvesterBlockEntity(BlockPos pos, BlockState state) {
        super(TestModBlocks.TREE_HARVESTER_ENTITY.get(), pos, state);
    }

    public CanvasSupplier getCanvas() {
        return this.canvasSupplier;
    }

    public @Nullable ResourceHandler<ItemResource> getTools() {
        return this.tools == null ? null : this.tools.getCapability();
    }

    public @Nullable ResourceHandler<ItemResource> getDrops() {
        return this.drops == null ? null : this.drops.getCapability();
    }

    public ResourceHandler<ItemResource> getAxe() {
        return this.axe;
    }

    public ResourceHandler<ItemResource> getSaplings() {
        return this.saplings;
    }

    public void serverTick(ServerLevel level) {
        this.scanSlice(level);
        if (++this.scanTick < SCAN_TICKS) {
            return;
        }
        this.harvestAll(level);
        this.targets.clear();
        this.scanTick = 0;
        this.scanIndex = 0;
        this.planted = 0;
    }

    private void harvestAll(ServerLevel level) {
        if (this.targets.isEmpty()) {
            return;
        }
        ItemStack tool = this.axe.getResource(0).toStack(this.axe.getAmountAsInt(0));
        int next = 0;
        while (next < this.targets.size()) {
            if (tool.isEmpty()) {
                this.axe.set(0, ItemResource.EMPTY, 0);
                tool = this.pullAxe();
                if (tool.isEmpty()) {
                    break;
                }
            }
            int budget = durabilityBudget(tool);
            while (budget > 0 && next < this.targets.size() && !tool.isEmpty()) {
                BlockPos pos = this.targets.get(next++);
                if (level.isLoaded(pos) && isHarvestable(level.getBlockState(pos))) {
                    this.harvest(level, pos, tool);
                    budget--;
                }
            }
        }
        this.axe.set(0, tool.isEmpty() ? ItemResource.EMPTY : ItemResource.of(tool), tool.getCount());
    }

    private static int durabilityBudget(ItemStack tool) {
        return tool.isDamageableItem() ? tool.getMaxDamage() - tool.getDamageValue() : Integer.MAX_VALUE;
    }

    private ItemStack pullAxe() {
        var tools = this.getTools();
        if (tools == null) {
            return ItemStack.EMPTY;
        }
        for (int slot = 0; slot < tools.size(); slot++) {
            ItemResource resource = tools.getResource(slot);
            if (resource.isEmpty() || !isAxe(resource)) {
                continue;
            }
            try (var transaction = Transaction.openRoot()) {
                if (tools.extract(slot, resource, 1, transaction) == 1 && this.axe.insert(0, resource, 1, transaction) == 1) {
                    transaction.commit();
                    return resource.toStack(1);
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private void scanSlice(ServerLevel level) {
        int end = Math.min(SCAN_VOLUME, this.scanIndex + SCAN_PER_TICK);
        boolean canPlant = this.planted < MAX_SAPLINGS && this.hasSapling();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (; this.scanIndex < end; this.scanIndex++) {
            int dx = this.scanIndex % SCAN_SIZE - RADIUS;
            int dy = this.scanIndex / SCAN_SIZE % SCAN_SIZE - RADIUS;
            int dz = this.scanIndex / (SCAN_SIZE * SCAN_SIZE) - RADIUS;
            if (dx * dx + dy * dy + dz * dz > RADIUS * RADIUS) {
                continue;
            }
            pos.setWithOffset(this.worldPosition, dx, dy, dz);
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (isHarvestable(state)) {
                this.targets.add(pos.immutable());
            } else if (canPlant && state.isAir() && canGrowAt(level, pos) && this.plantSapling(level, pos)) {
                this.planted++;
                canPlant = this.planted < MAX_SAPLINGS && this.hasSapling();
            }
        }
    }

    private static boolean isHarvestable(BlockState state) {
        if (state.is(BlockTags.LOGS)) {
            return true;
        }
        return state.is(BlockTags.LEAVES)
                && !(state.hasProperty(LeavesBlock.PERSISTENT) && state.getValue(LeavesBlock.PERSISTENT));
    }

    private static boolean canGrowAt(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos.below()).is(BlockTags.SUPPORTS_VEGETATION)) {
            return false;
        }
        for (BlockPos neighbor : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 0, 1))) {
            BlockState state = level.getBlockState(neighbor);
            if (state.is(BlockTags.SAPLINGS) || state.is(BlockTags.LOGS)) {
                return false;
            }
        }
        return level.getBlockState(pos.above()).isAir();
    }

    private boolean hasSapling() {
        if (isSapling(this.saplings.getResource(0))) {
            return true;
        }
        var output = this.getDrops();
        if (output == null) {
            return false;
        }
        for (int slot = 0; slot < output.size(); slot++) {
            if (isSapling(output.getResource(slot))) {
                return true;
            }
        }
        return false;
    }

    private boolean plantSapling(ServerLevel level, BlockPos pos) {
        if (plantFrom(this.saplings, level, pos)) {
            return true;
        }
        var output = this.getDrops();
        return output != null && plantFrom(output, level, pos);
    }

    private static boolean plantFrom(ResourceHandler<ItemResource> source, ServerLevel level, BlockPos pos) {
        for (int slot = 0; slot < source.size(); slot++) {
            ItemResource resource = source.getResource(slot);
            if (!isSapling(resource)) {
                continue;
            }
            BlockState sapling = ((BlockItem) resource.getItem()).getBlock().defaultBlockState();
            if (!sapling.canSurvive(level, pos)) {
                continue;
            }
            try (var transaction = Transaction.openRoot()) {
                if (source.extract(slot, resource, 1, transaction) == 1 && level.setBlock(pos, sapling, Block.UPDATE_ALL)) {
                    transaction.commit();
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isSapling(ItemResource resource) {
        return !resource.isEmpty() && resource.getItem() instanceof BlockItem && resource.toStack().is(ItemTags.SAPLINGS);
    }

    private void harvest(ServerLevel level, BlockPos pos, ItemStack tool) {
        BlockState state = level.getBlockState(pos);
        List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), null, tool);
        if (!level.destroyBlock(pos, false)) {
            return;
        }
        var output = this.getDrops();
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) {
                continue;
            }
            ItemResource resource = ItemResource.of(drop);
            int stored = 0;
            if (isSapling(resource)) {
                stored = insert(this.saplings, resource, drop.getCount());
            }
            if (output != null && stored < drop.getCount()) {
                stored += insert(output, resource, drop.getCount() - stored);
            }
            if (stored < drop.getCount()) {
                Block.popResource(level, pos, drop.copyWithCount(drop.getCount() - stored));
            }
        }
        tool.hurtAndBreak(1, level, (LivingEntity) null, broken -> {});
    }

    private static int insert(ResourceHandler<ItemResource> handler, ItemResource resource, int amount) {
        try (var transaction = Transaction.openRoot()) {
            int inserted = handler.insert(resource, amount, transaction);
            transaction.commit();
            return inserted;
        }
    }

    private static boolean isAxe(ItemResource resource) {
        return resource.toStack().is(ItemTags.AXES);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.axe.deserialize(input.childOrEmpty("axe"));
        this.saplings.deserialize(input.childOrEmpty("saplings"));
        input.read("canvas", CanvasState.CODEC).ifPresent(this.canvas::setState);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.axe.serialize(output.child("axe"));
        this.saplings.serialize(output.child("saplings"));
        output.store("canvas", CanvasState.CODEC, this.canvas.state());
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level == null) {
            return;
        }
        if (!this.axe.getResource(0).isEmpty()) {
            Block.popResource(this.level, pos, this.axe.getResource(0).toStack(this.axe.getAmountAsInt(0)));
        }
        ItemResource sapling = this.saplings.getResource(0);
        int remaining = this.saplings.getAmountAsInt(0);
        while (!sapling.isEmpty() && remaining > 0) {
            int count = Math.min(remaining, sapling.getMaxStackSize());
            Block.popResource(this.level, pos, sapling.toStack(count));
            remaining -= count;
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level instanceof ServerLevel serverLevel) {
            var server = serverLevel.getServer();
            server.schedule(server.wrapRunnable(() -> {
                if (!this.isRemoved()) {
                    this.canvasSupplier.refresh(server);
                }
            }));
        }
    }

    private void onToolsStorageChanged(Optional<ItemHandlerAccess> access) {
        this.tools = access.map(this::cache).orElse(null);
    }

    private void onDropsStorageChanged(Optional<ItemHandlerAccess> access) {
        this.drops = access.map(this::cache).orElse(null);
    }

    private @Nullable BlockCapabilityCache<ResourceHandler<ItemResource>, @Nullable Direction> cache(ItemHandlerAccess access) {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return null;
        }
        var target = serverLevel.getServer().getLevel(access.level());
        if (target == null) {
            return null;
        }
        return BlockCapabilityCache.create(Capabilities.Item.BLOCK, target, access.pos(), access.side());
    }
}
