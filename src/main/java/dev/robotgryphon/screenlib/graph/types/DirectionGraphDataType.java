package dev.robotgryphon.screenlib.graph.types;

import com.mojang.serialization.Codec;
import dev.robotgryphon.screenlib.graph.GraphTheme;
import net.minecraft.core.Direction;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public class DirectionGraphDataType implements GraphDataType<Direction> {

    private static final Set<Direction> ALLOWED = Set.of(Direction.values());

    @Override
    public Class<Direction> dataType() {
        return Direction.class;
    }

    @Override
    public int color() {
        return GraphTheme.COLOR_DIRECTION;
    }

    @Override
    public Codec<Direction> codec() {
        return Direction.CODEC;
    }

    @Override
    public Optional<Collection<Direction>> allowedValues() {
        return Optional.of(ALLOWED);
    }
}
