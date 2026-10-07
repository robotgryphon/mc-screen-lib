package dev.robotgryphon.screenlib.client.ui.widget;

import dev.robotgryphon.screenlib.graph.ViewportState;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2d;
import org.joml.Vector2dc;

public class CanvasViewport {
    public static final float MIN_ZOOM = 0.25f;
    public static final float MAX_ZOOM = 4.0f;
    public static final float ZOOM_STEP = 1.1f;

    private float panX = 0f;
    private float panY = 0f;
    private float zoom = 1f;

    public void transformPose(Matrix3x2fStack pose) {
        pose.translate(this.panX, this.panY);
        pose.scale(this.zoom, this.zoom);
    }

    public void pan(float x, float y) {
        this.panX += x;
        this.panY += y;
    }

    public ViewportState state(int screenWidth, int screenHeight) {
        Vector2dc center = this.screenToCanvas(screenWidth / 2.0, screenHeight / 2.0);
        return new ViewportState(center.x(), center.y(), this.zoom);
    }

    public void apply(ViewportState state, int screenWidth, int screenHeight) {
        this.zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, state.zoom()));
        this.panX = (float) (screenWidth / 2.0 - state.centerX() * this.zoom);
        this.panY = (float) (screenHeight / 2.0 - state.centerY() * this.zoom);
    }

    public float zoom() {
        return this.zoom;
    }

    public void zoom(float zoom) {
        this.zoom += zoom;
    }

    public void zoomAround(float newZoom, double screenX, double screenY) {
        newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoom));
        if (newZoom == this.zoom) {
            return;
        }
        float ratio = newZoom / this.zoom;
        this.panX = (float) (screenX - (screenX - this.panX) * ratio);
        this.panY = (float) (screenY - (screenY - this.panY) * ratio);
        this.zoom = newZoom;
    }

    public Vector2dc screenToCanvas(double screenX, double screenY) {
        return screenToCanvas(new Vector2d(screenX, screenY));
    }

    public Vector2dc screenToCanvas(Vector2dc screen) {
        return new Vector2d((screen.x() - this.panX) / this.zoom, (screen.y() - this.panY) / this.zoom);
    }

    public Vector2dc canvasToScreen(double canvasX, double canvasY) {
        return new Vector2d(canvasX * this.zoom + this.panX, canvasY * this.zoom + this.panY);
    }
}
