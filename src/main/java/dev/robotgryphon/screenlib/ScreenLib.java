package dev.robotgryphon.screenlib;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import dev.robotgryphon.screenlib.graph.CanvasState;
import dev.robotgryphon.screenlib.menu.TestScreenMenu;
import dev.robotgryphon.screenlib.menu.TestScreenMenuProvider;
import dev.robotgryphon.screenlib.network.NetworkRegistration;
import dev.robotgryphon.screenlib.types.NodeDefinition;
import dev.robotgryphon.screenlib.types.PropertyDefinition;
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

import java.util.List;
import java.util.Optional;

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

    // Vanilla-namespaced "core" definitions so they read like built-ins to datapacks.
    private static final DeferredRegister<PropertyDefinition<?>> CORE_PROPERTY_DEFINITIONS =
            DeferredRegister.create(PropertyDefinition.REGISTRY_KEY, "minecraft");

    /**
     * Mod-specific property definitions live here; datapacks can also extend
     * the registry. Includes both the generic types used by ports and the
     * sampler-specific definitions whose defaults seed the demo node's rows.
     */
    public static final DeferredRegister<PropertyDefinition<?>> PROPERTY_DEFINITIONS =
            DeferredRegister.create(PropertyDefinition.REGISTRY_KEY, MOD_ID);

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
        PROPERTY_DEFINITIONS.makeRegistry(builder -> builder.sync(false));

        // -- Generic types — no default, intended for wire (port) typing -----

        CORE_PROPERTY_DEFINITIONS.register("block_pos", () -> new PropertyDefinition<>(
                BlockPos.CODEC, COLOR_POSITION,
                Component.translatable("property_type.minecraft.block_pos"),
                Optional.empty()));

        CORE_PROPERTY_DEFINITIONS.register("direction", () -> new PropertyDefinition<>(
                Direction.CODEC, COLOR_DIRECTION,
                Component.translatable("property_type.minecraft.direction"),
                Optional.empty()));

        CORE_PROPERTY_DEFINITIONS.register("int", () -> new PropertyDefinition<>(
                Codec.INT, COLOR_NUMBER,
                Component.translatable("property_type.minecraft.int"),
                Optional.empty()));
        CORE_PROPERTY_DEFINITIONS.register("float", () -> new PropertyDefinition<>(
                Codec.FLOAT, COLOR_NUMBER,
                Component.translatable("property_type.minecraft.float"),
                Optional.empty()));
        CORE_PROPERTY_DEFINITIONS.register("double", () -> new PropertyDefinition<>(
                Codec.DOUBLE, COLOR_NUMBER,
                Component.translatable("property_type.minecraft.double"),
                Optional.empty()));
        CORE_PROPERTY_DEFINITIONS.register("string", () -> new PropertyDefinition<>(
                Codec.STRING, COLOR_STRING,
                Component.translatable("property_type.minecraft.string"),
                Optional.empty()));
        CORE_PROPERTY_DEFINITIONS.register("bool", () -> new PropertyDefinition<>(
                Codec.BOOL, COLOR_BOOLEAN,
                Component.translatable("property_type.minecraft.bool"),
                Optional.empty()));

        // Mod-specific. "item_handler" is a placeholder — its codec will be
        // refined once the resource-access wiring is in place; the position
        // codec is enough to thread the visual through the editor for now.
        PROPERTY_DEFINITIONS.register("item_handler", () -> new PropertyDefinition<>(
                BlockPos.CODEC, COLOR_RESOURCE,
                Component.translatable("property_type.screenlib.item_handler"),
                Optional.empty()));

        // -- Sampler-specific definitions — defaults baked in ---------------
        // Each property of the sampler node references one of these. The
        // defaults match the reference screenshot so a freshly-spawned
        // sampler reads as the canonical KSampler starting state.
        //
        // These could equally well live alongside the NodeDefinition data,
        // but because there's no datapack-loaded codec for PropertyDefinition
        // (codecs can't be serialized to JSON without a dispatch scheme),
        // they're registered through code like the generic types are.

        // Typed defaults — PropertyDefinition's T parameter tracks the
        // codec's value type so the Optional<T> lands as the typed object
        // directly, no Dynamic round-trip needed at registration time.
        // Persistence still goes through the codec (CanvasStateManager.encode/applyPropertyValues),
        // so the codec-typed Optional and the persistence path stay in sync.
        PROPERTY_DEFINITIONS.register("sampler/seed", () -> new PropertyDefinition<>(
                Codec.INT, COLOR_NUMBER,
                Component.translatable("property_type.minecraft.int"),
                Optional.of(156680208)));
        PROPERTY_DEFINITIONS.register("sampler/steps", () -> new PropertyDefinition<>(
                Codec.INT, COLOR_NUMBER,
                Component.translatable("property_type.minecraft.int"),
                Optional.of(20)));
        PROPERTY_DEFINITIONS.register("sampler/cfg", () -> new PropertyDefinition<>(
                Codec.FLOAT, COLOR_NUMBER,
                Component.translatable("property_type.minecraft.float"),
                Optional.of(8.0f)));
        // The two string properties on the sampler are picks from a fixed
        // option set rather than free-form text. Registering the allowed
        // values here is what lights up the dropdown editor in NodeWidget —
        // a String PropertyDefinition with an empty allowedValues would
        // still render as a plain text slot. Option order is preserved in
        // the popup, so list them in the order the user will most often
        // want to scan.
        PROPERTY_DEFINITIONS.register("sampler/sampler_name", () -> new PropertyDefinition<>(
                Codec.STRING, COLOR_STRING,
                Component.translatable("property_type.minecraft.string"),
                Optional.of("euler"),
                Optional.of(List.of(
                        "euler", "euler_ancestral", "heun", "dpm_2",
                        "lms", "ddim", "ddpm", "uni_pc"))));
        PROPERTY_DEFINITIONS.register("sampler/scheduler", () -> new PropertyDefinition<>(
                Codec.STRING, COLOR_STRING,
                Component.translatable("property_type.minecraft.string"),
                Optional.of("normal"),
                Optional.of(List.of(
                        "normal", "karras", "exponential",
                        "sgm_uniform", "simple"))));
        PROPERTY_DEFINITIONS.register("sampler/denoise", () -> new PropertyDefinition<>(
                Codec.FLOAT, COLOR_NUMBER,
                Component.translatable("property_type.minecraft.float"),
                Optional.of(1.0f)));
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

    private static void datapackRegistries(DataPackRegistryEvent.NewRegistry newRegistries) {
        newRegistries.dataPackRegistry(NodeDefinition.REGISTRY_KEY, NodeDefinition.CODEC, NodeDefinition.CODEC);
    }
}
