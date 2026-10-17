package dev.robotgryphon.screenlib.ui.api.layout;

public enum VerticalAnchor {
    TOP,
    CENTER,
    BOTTOM;

    public int position(int start, int available, int size, int offset) {
        return switch (this) {
            case TOP -> start + offset;
            case CENTER -> start + (available - size) / 2 + offset;
            case BOTTOM -> start + available - size - offset;
        };
    }
}
