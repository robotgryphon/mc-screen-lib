package dev.robotgryphon.screenlib.graph.context;

import com.mojang.serialization.Codec;
import dev.robotgryphon.screenlib.ScreenLib;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.resources.ResourceKey;

import java.util.Map;

public interface NodeContextSupplier {
    ResourceKey<Registry<NodeContextSupplier>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ScreenLib.id("context_suppliers"));

    Codec<Holder<NodeContextSupplier>> HOLDER_CODEC = RegistryCodecs.holder(REGISTRY_KEY);

    Map<String, Object> supply(NodeCreationContext context);
}
