package dev.robotgryphon.screenlib.graph.types;

import com.mojang.serialization.Codec;

public class ItemHandlerGraphDataType implements GraphDataType<ItemHandlerAccess> {
    @Override
    public Class<ItemHandlerAccess> dataType() {
        return ItemHandlerAccess.class;
    }

    @Override
    public Codec<ItemHandlerAccess> codec() {
        return ItemHandlerAccess.CODEC;
    }
}
