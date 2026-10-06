package dev.robotgryphon.screenlib;

import com.mojang.logging.LogUtils;
import dev.robotgryphon.screenlib.graph.CanvasState;
import dev.robotgryphon.screenlib.graph.GraphTheme;
import dev.robotgryphon.screenlib.menu.TestScreenMenu;
import dev.robotgryphon.screenlib.menu.TestScreenMenuProvider;
import dev.robotgryphon.screenlib.network.NetworkRegistration;
import dev.robotgryphon.screenlib.graph.types.*;
import dev.robotgryphon.screenlib.types.NodeDefinition;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.NewDatapackRegistryEvent;
import org.slf4j.Logger;

@Mod(ScreenLib.MOD_ID)
public class ScreenLib {

    public static final String MOD_ID = "screenlib";
    public static final Logger LOGGER = LogUtils.getLogger();

    // Common-namespaced "core" definitions so they read like built-ins to datapacks.
    private static final DeferredRegister<GraphDataType<?>> CORE_PROPERTY_DEFINITIONS =
            DeferredRegister.create(GraphDataType.REGISTRY_KEY, "c");

    /**
     * Mod-specific property definitions live here; datapacks can also extend
     * the registry. Includes both the generic types used by ports and the
     * sampler-specific definitions whose defaults seed the demo node's rows.
     */
    public static final DeferredRegister<GraphDataType<?>> PROPERTY_DEFINITIONS =
            DeferredRegister.create(GraphDataType.REGISTRY_KEY, MOD_ID);

    /**
     * NeoForge attachment registry — anything attached to an
     * {@link net.neoforged.neoforge.attachment.IAttachmentHolder}
     * (Level, Entity, ItemStack, etc.) must be registered here first.
     */
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MOD_ID);

    /**
     * Attachment for the demo {@link dev.robotgryphon.screenlib.client.ui.TestScreen}'s
     * canvas state. Lives on the {@link net.minecraft.world.level.Level} so the
     * placed nodes and connections survive closing and re-opening the screen
     * within a session. Serialization rides {@link CanvasState#MAP_CODEC}.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CanvasState>> TEST_SCREEN_ATTACHMENT =
            ATTACHMENT_TYPES.register("test_screen", () -> AttachmentType
                    .builder(() -> CanvasState.EMPTY)
                    .serialize(CanvasState.MAP_CODEC)
                    .build());

    /**
     * Menu registry — required for {@link net.minecraft.world.inventory.AbstractContainerMenu}
     * subclasses to round-trip through the vanilla open-container protocol.
     */
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, MOD_ID);

    /**
     * MenuType for the demo {@link dev.robotgryphon.screenlib.client.ui.TestScreen}.
     * Built via {@link IMenuTypeExtension#create(net.neoforged.neoforge.network.IContainerFactory)}
     * so the client-side factory can read the extra-data buffer that the
     * server's {@link TestScreenMenuProvider#writeClientSideData} wrote.
     */
    public static final DeferredHolder<MenuType<?>, MenuType<TestScreenMenu>> TEST_SCREEN_MENU =
            MENU_TYPES.register("test_screen", () -> IMenuTypeExtension.create(TestScreenMenu::new));

    static {
        // The registry itself must exist before either DeferredRegister can
        // populate it. Only one DeferredRegister can own the makeRegistry call —
        // the other simply attaches its entries to the same registry id.
        // Without this, RegistryFixedCodec fails with "Can't access registry"
        // because the registry never makes it into the HolderLookup.Provider
        // that data gen and the runtime registry-loader hand to codec ops.
        PROPERTY_DEFINITIONS.makeRegistry(builder -> builder.sync(true));

        // -- Generic types — no default, intended for wire (port) typing -----

        CORE_PROPERTY_DEFINITIONS.register("block_pos", BlockPositionGraphDataType::new);
        CORE_PROPERTY_DEFINITIONS.register("direction", DirectionGraphDataType::new);
        CORE_PROPERTY_DEFINITIONS.register("int", IntGraphDataType::new);
        CORE_PROPERTY_DEFINITIONS.register("float", FloatGraphDataType::new);
        CORE_PROPERTY_DEFINITIONS.register("double", DoubleGraphDataType::new);
        CORE_PROPERTY_DEFINITIONS.register("string", StringGraphDataType::new);
        CORE_PROPERTY_DEFINITIONS.register("bool", BooleanGraphDataType::new);

        // Mod-specific. "item_handler" is a placeholder — its codec will be
        // refined once the resource-access wiring is in place; the position
        // codec is enough to thread the visual through the editor for now.
        PROPERTY_DEFINITIONS.register("item_handler", () -> new SimpleGraphDataType<>(BlockPos.class,
                BlockPos.CODEC, GraphTheme.COLOR_RESOURCE));
    }

    public ScreenLib(IEventBus modBus) {
        modBus.addListener(ScreenLib::datapackRegistries);
        modBus.addListener(NetworkRegistration::register);

        NeoForge.EVENT_BUS.addListener(ScreenLib::commands);

        CORE_PROPERTY_DEFINITIONS.register(modBus);
        PROPERTY_DEFINITIONS.register(modBus);
        ATTACHMENT_TYPES.register(modBus);
        MENU_TYPES.register(modBus);
    }

    private static void commands(RegisterCommandsEvent cmd) {
        final var dispatcher = cmd.getDispatcher();
        final var root = Commands.literal(MOD_ID);

        final var test = Commands.literal("test")
                .requires(CommandSourceStack::isPlayer)
                .executes(ctx -> {
                    final var player = ctx.getSource().getPlayerOrException();
                    // Routing through openMenu (instead of a one-shot packet) means
                    // the server keeps an authoritative TestScreenMenu instance for
                    // the duration of the edit session — the client's state updates
                    // land on it via UpdateCanvasStatePayload and are persisted onto
                    // the level attachment from there.
                    player.openMenu(new TestScreenMenuProvider(player.level()));
                    return 0;
                });

        root.then(test);
        dispatcher.register(root);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    private static void datapackRegistries(NewDatapackRegistryEvent newRegistries) {
        // worldRegistry (not reloadableRegistry) so the entries sync to clients;
        // the add-node dialog reads them from the client-side registry access.
        newRegistries.worldRegistry(NodeDefinition.REGISTRY_KEY, NodeDefinition.CODEC, NodeDefinition.CODEC);
    }
}
