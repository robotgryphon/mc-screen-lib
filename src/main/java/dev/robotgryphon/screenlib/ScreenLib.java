package dev.robotgryphon.screenlib;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import dev.robotgryphon.screenlib.types.NodeDefinition;
import dev.robotgryphon.screenlib.types.PropertyType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;
import org.slf4j.Logger;

@Mod(ScreenLib.MOD_ID)
public class ScreenLib {

    public static final String MOD_ID = "screenlib";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final DeferredRegister<PropertyType<?>> CORE_PROPERTY_TYPES = DeferredRegister.create(id("property_types"), "minecraft");
    public static final DeferredRegister<PropertyType<?>> PROPERTY_TYPES = DeferredRegister.create(id("property_types"), MOD_ID);

    static {
        CORE_PROPERTY_TYPES.register("block_pos", () -> new PropertyType<>(BlockPos.CODEC));

        CORE_PROPERTY_TYPES.register("int", () -> new PropertyType<>(Codec.INT));
        CORE_PROPERTY_TYPES.register("float", () -> new PropertyType<>(Codec.FLOAT));
        CORE_PROPERTY_TYPES.register("double", () -> new PropertyType<>(Codec.DOUBLE));
        CORE_PROPERTY_TYPES.register("string", () -> new PropertyType<>(Codec.STRING));
        CORE_PROPERTY_TYPES.register("bool", () -> new PropertyType<>(Codec.BOOL));
    }

    public ScreenLib(IEventBus modBus) {
        modBus.addListener(ScreenLib::datapackRegistries);

        CORE_PROPERTY_TYPES.register(modBus);
        PROPERTY_TYPES.register(modBus);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    private static void datapackRegistries(DataPackRegistryEvent.NewRegistry newRegistries) {
        newRegistries.dataPackRegistry(NodeDefinition.REGISTRY_KEY, NodeDefinition.CODEC, NodeDefinition.CODEC);
    }
}
