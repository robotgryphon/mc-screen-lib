package dev.robotgryphon.testmod.server;

import dev.robotgryphon.testmod.common.TestMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(TestMod.MOD_ID)
public class TestModServer {
    public TestModServer() {
        NeoForge.EVENT_BUS.addListener(TestModServer::registerCommands);
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(TestMod.MOD_ID)
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
                        })));
    }
}
