package dev.robotgryphon.screenlib;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import dev.robotgryphon.screenlib.graph.CanvasState;
import dev.robotgryphon.screenlib.menu.TestScreenMenu;
import dev.robotgryphon.screenlib.menu.TestScreenMenuProvider;
import dev.robotgryphon.screenlib.network.NetworkRegistration;
import dev.robotgryphon.screenlib.types.NodeDefinition;
import dev.robotgryphon.screenlib.types.PropertyType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.slf4j.Logger;

@Mod(ScreenLib.MOD_ID)
public class ScreenLib {

    public static final String MOD_ID = "screenlib";
    public static final Logger LOGGER = LogUtils.getLogger();

    // -- Color constants ----------------------------------------------------
    // Picked to roughly match the target palette: green for spatial values,
    // orange for scalars, light blue for directions, pink for resource handles.
    private static final int COLOR_POSITION   = 0xFF55D784;
    private static final int COLOR_NUMBER     = 0xFFE89E4A;
    private static final int COLOR_BOOLEAN    = 0xFFE8C84A;
    private static final int COLOR_STRING     = 0xFF6FD0E8;
    private static final int COLOR_DIRECTION  = 0xFF7AA8FF;
    private static final int COLOR_RESOURCE   = 0xFFE07ADC;

    // Vanilla-namespaced "core" types so they read like built-ins to datapacks.
    private static final DeferredRegister<PropertyType<?>> CORE_PROPERTY_TYPES =
            DeferredRegister.create(PropertyType.REGISTRY_KEY, "minecraft");

    /** Mod-specific property types live here; datapacks can also extend the registry. */
    public static final DeferredRegister<PropertyType<?>> PROPERTY_TYPES =
            DeferredRegister.create(PropertyType.REGISTRY_KEY, MOD_ID);

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
        PROPERTY_TYPES.makeRegistry(builder -> builder.sync(false));

        // Spatial.
        CORE_PROPERTY_TYPES.register("block_pos", () -> new PropertyType<>(
                BlockPos.CODEC, COLOR_POSITION, Component.translatable("property_type.minecraft.block_pos")));

        CORE_PROPERTY_TYPES.register("direction", () -> new PropertyType<>(
                Direction.CODEC, COLOR_DIRECTION, Component.translatable("property_type.minecraft.direction")));

        // Scalars.
        CORE_PROPERTY_TYPES.register("int", () -> new PropertyType<>(
                Codec.INT, COLOR_NUMBER, Component.translatable("property_type.minecraft.int")));
        CORE_PROPERTY_TYPES.register("float", () -> new PropertyType<>(
                Codec.FLOAT, COLOR_NUMBER, Component.translatable("property_type.minecraft.float")));
        CORE_PROPERTY_TYPES.register("double", () -> new PropertyType<>(
                Codec.DOUBLE, COLOR_NUMBER, Component.translatable("property_type.minecraft.double")));
        CORE_PROPERTY_TYPES.register("string", () -> new PropertyType<>(
                Codec.STRING, COLOR_STRING, Component.translatable("property_type.minecraft.string")));
        CORE_PROPERTY_TYPES.register("bool", () -> new PropertyType<>(
                Codec.BOOL, COLOR_BOOLEAN, Component.translatable("property_type.minecraft.bool")));

        // Mod-specific. "item_handler" is a placeholder — its codec will be
        // refined once the resource-access wiring is in place; the position
        // codec is enough to thread the visual through the editor for now.
        PROPERTY_TYPES.register("item_handler", () -> new PropertyType<>(
                BlockPos.CODEC, COLOR_RESOURCE, Component.translatable("property_type.screenlib.item_handler")));
    }

    public ScreenLib(IEventBus modBus) {
        modBus.addListener(ScreenLib::datapackRegistries);
        modBus.addListener(NetworkRegistration::register);

        NeoForge.EVENT_BUS.addListener(ScreenLib::commands);

        CORE_PROPERTY_TYPES.register(modBus);
        PROPERTY_TYPES.register(modBus);
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

    private static void datapackRegistries(DataPackRegistryEvent.NewRegistry newRegistries) {
        newRegistries.dataPackRegistry(NodeDefinition.REGISTRY_KEY, NodeDefinition.CODEC, NodeDefinition.CODEC);
    }
}
