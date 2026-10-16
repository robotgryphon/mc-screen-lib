package dev.robotgryphon.screenlib.client.ui.widget.property;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public record NumericKind<N extends Number>(Class<N> type, N zero, BiFunction<N, Integer, N> step,
                                           Function<String, N> parser) {
    public static final NumericKind<Integer> INTEGER = new NumericKind<>(Integer.class, 0,
            (value, direction) -> value + direction, Integer::parseInt);

    public static final NumericKind<Float> FLOAT = new NumericKind<>(Float.class, 0f,
            (value, direction) -> Math.round((value + direction * 0.1f) * 10f) / 10f, Float::parseFloat);

    public static final NumericKind<Double> DOUBLE = new NumericKind<>(Double.class, 0.0,
            (value, direction) -> Math.round((value + direction * 0.1) * 10.0) / 10.0, Double::parseDouble);

    private static final List<NumericKind<?>> KINDS = List.of(INTEGER, FLOAT, DOUBLE);

    public static Optional<NumericKind<?>> of(Class<?> type) {
        return KINDS.stream().filter(kind -> kind.type() == type).findFirst();
    }

    public Optional<N> parse(String text) {
        try {
            return Optional.of(this.parser.apply(text));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }
}
