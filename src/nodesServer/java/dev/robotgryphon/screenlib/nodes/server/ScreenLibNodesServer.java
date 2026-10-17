package dev.robotgryphon.screenlib.nodes.server;

import dev.robotgryphon.screenlib.core.ScreenLib;
import dev.robotgryphon.screenlib.nodes.api.ScreenLibCapabilities;
import dev.robotgryphon.screenlib.nodes.api.spi.NodesProvider;
import dev.robotgryphon.screenlib.nodes.server.network.ServerNetworkRegistration;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@Mod(ScreenLib.MOD_ID)
public class ScreenLibNodesServer {
    public ScreenLibNodesServer(IEventBus modBus) {
        NodesProvider.SLOT.install(new ServerNodesProvider());
        modBus.addListener(ServerNetworkRegistration::register);
        modBus.addListener(EventPriority.HIGH, (RegisterCapabilitiesEvent event) -> event.setNonProxyable(ScreenLibCapabilities.CANVAS));
    }
}
