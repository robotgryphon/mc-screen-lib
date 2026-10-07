package dev.robotgryphon.testmod;

import com.mojang.logging.LogUtils;
import dev.robotgryphon.testmod.menu.LevelCanvasSource;
import dev.robotgryphon.testmod.menu.TestScreenMenuProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod(TestMod.MOD_ID)
public class TestMod {
    public static final String MOD_ID = "testmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TestMod(IEventBus modBus) {
        LOGGER.info("{} loaded", MOD_ID);
        TestModBlocks.register(modBus);
        TestModMenus.register(modBus);
        NeoForge.EVENT_BUS.addListener(TestMod::registerCommands);
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(MOD_ID)
                .then(Commands.literal("chest")
                        .requires(CommandSourceStack::isPlayer)
                        .executes(context -> {
                            var player = context.getSource().getPlayerOrException();
                            var level = context.getSource().getLevel();
                            BlockPos pos = player.blockPosition().relative(player.getDirection(), 2);
                            ChestFiller.fill(level, pos, level.getRandom());
                            context.getSource().sendSuccess(() -> Component.literal(
                                    "Filled chest at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()), false);
                            return 1;
                        }))
                .then(Commands.literal("screen")
                        .requires(CommandSourceStack::isPlayer)
                        .executes(context -> {
                            var player = context.getSource().getPlayerOrException();
                            player.openMenu(new TestScreenMenuProvider(new LevelCanvasSource(player.level())));
                            return 1;
                        })));
    }
}
