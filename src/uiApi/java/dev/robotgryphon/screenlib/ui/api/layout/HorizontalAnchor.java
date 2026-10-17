package dev.robotgryphon.screenlib.ui.api.layout;

public enum HorizontalAnchor {
    LEFT,
    CENTER,
    RIGHT;

    public int position(int start, int available, int size, int offset) {
        return switch (this) {
            case LEFT -> start + offset;
            case CENTER -> start + (available - size) / 2 + offset;
            case RIGHT -> start + available - size - offset;
        };
    }
}
