package dev.robotgryphon.screenlib.ui.api.render;

import org.joml.Vector4f;
import org.joml.Vector4fc;

public record RoundedRect(Vector4fc bounds,
                    Vector4fc bodyColor,
                    Vector4fc titleColor,
                    Vector4fc borderColor,
                    float titleHeight,
                    boolean dropShadow,
                    float cornerRadiusOverride,
                    float borderThicknessOverride,
                    Vector4fc glowColor) {
    private static final Vector4fc NO_GLOW = new Vector4f(0f, 0f, 0f, 0f);

    public RoundedRect(Vector4fc bounds,
                 Vector4fc bodyColor,
                 Vector4fc titleColor,
                 Vector4fc borderColor,
                 float titleHeight,
                 boolean dropShadow) {
        this(bounds, bodyColor, titleColor, borderColor, titleHeight, dropShadow, 0f, 0f, NO_GLOW);
    }

    public RoundedRect(Vector4fc bounds,
                 Vector4fc bodyColor,
                 Vector4fc titleColor,
                 Vector4fc borderColor,
                 float titleHeight,
                 boolean dropShadow,
                 float cornerRadiusOverride,
                 float borderThicknessOverride) {
        this(bounds, bodyColor, titleColor, borderColor, titleHeight, dropShadow,
                cornerRadiusOverride, borderThicknessOverride, NO_GLOW);
    }
}
