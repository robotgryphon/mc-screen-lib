package dev.robotgryphon.screenlib;

import com.mojang.logging.LogUtils;
import dev.robotgryphon.screenlib.graph.CanvasState;
import dev.robotgryphon.screenlib.menu.TestScreenMenu;
import dev.robotgryphon.screenlib.menu.TestScreenMenuProvider;
import dev.robotgryphon.screenlib.network.NetworkRegistration;
import dev.robotgryphon.screenlib.graph.types.*;
import dev.robotgryphon.screenlib.graph.nodes.NodeDefinition;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.state.BlockState;
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

    private static final DeferredRegister<GraphDataType<?>> CORE_DATA_TYPES =
            DeferredRegister.create(GraphDataType.REGISTRY_KEY, "c");

    public static final DeferredRegister<GraphDataType<?>> LIB_DATA_TYPES =
            DeferredRegister.create(GraphDataType.REGISTRY_KEY, MOD_ID);

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CanvasState>> TEST_SCREEN_ATTACHMENT =
            ATTACHMENT_TYPES.register("test_screen", () -> AttachmentType
                    .builder(() -> CanvasState.EMPTY)
                    .serialize(CanvasState.MAP_CODEC)
                    .build());

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<TestScreenMenu>> TEST_SCREEN_MENU =
            MENU_TYPES.register("test_screen", () -> IMenuTypeExtension.create(TestScreenMenu::new));

    static {
        LIB_DATA_TYPES.makeRegistry(builder -> builder.sync(true));
    }

    public static final Holder<GraphDataType<?>> BLOCK_POS_DATA_TYPE = CORE_DATA_TYPES.register("block_position", BlockPositionGraphDataType::new);
    public static final Holder<GraphDataType<?>> DIRECTION_DATA_TYPE = CORE_DATA_TYPES.register("direction", DirectionGraphDataType::new);
    public static final Holder<GraphDataType<?>> INTEGER_DATA_TYPE = CORE_DATA_TYPES.register("integer", IntGraphDataType::new);
    public static final Holder<GraphDataType<?>> FLOAT_DATA_TYPE = CORE_DATA_TYPES.register("float", FloatGraphDataType::new);
    public static final Holder<GraphDataType<?>> DOUBLE_DATA_TYPE = CORE_DATA_TYPES.register("double", DoubleGraphDataType::new);
    public static final Holder<GraphDataType<?>> STRING_DATA_TYPE = CORE_DATA_TYPES.register("string", StringGraphDataType::new);
    public static final Holder<GraphDataType<?>> BOOLEAN_DATA_TYPE = CORE_DATA_TYPES.register("boolean", BooleanGraphDataType::new);

    public static final Holder<GraphDataType<?>> ITEM_HANDLER_DATA_TYPE = LIB_DATA_TYPES.register("item_handler", ()
            -> new SimpleGraphDataType<>(BlockPos.class, BlockPos.CODEC));

    public static final Holder<GraphDataType<?>> LEVEL_DATA_TYPE = LIB_DATA_TYPES.register("level", LevelGraphDataType::new);

    public static final Holder<GraphDataType<?>> BLOCKSTATE_DATA_TYPE = LIB_DATA_TYPES.register("blockstate", ()
            -> new SimpleGraphDataType<>(BlockState.class, BlockState.CODEC));

    public ScreenLib(IEventBus modBus) {
        modBus.addListener(ScreenLib::datapackRegistries);
        modBus.addListener(NetworkRegistration::register);

        NeoForge.EVENT_BUS.addListener(ScreenLib::commands);

        CORE_DATA_TYPES.register(modBus);
        LIB_DATA_TYPES.register(modBus);
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
        newRegistries.worldRegistry(NodeDefinition.REGISTRY_KEY, NodeDefinition.CODEC, NodeDefinition.CODEC);
    }
}
