package dev.robotgryphon.screenlib.graph;

import org.joml.Matrix3x2fStack;
import org.joml.Vector2d;
import org.joml.Vector2dc;

/**
 * The "how you're looking at it" half of a {@link Canvas}.
 *
 * <p>Holds the user's pan offset and zoom factor, the math for
 * converting between screen-space and canvas-space, and the pose
 * transform that puts the renderer into canvas-space before nodes draw.
 * Pulled out of {@code Canvas} so the canvas itself only has to know
 * about the document — nodes, connections, in-flight drag type — while
 * everything about how it's currently displayed lives here.
 *
 * <p>One viewport is owned by each {@link Canvas} via
 * {@link Canvas#viewport()}. Two users editing the same canvas at the
 * same time would each need their own viewport, but the single-screen
 * UI we have today only ever instantiates one alongside its canvas, so
 * the back-reference is left implicit.
 */
public class CanvasViewport {

    /** Smallest zoom factor scroll-out is allowed to reach. */
    public static final float MIN_ZOOM = 0.25f;
    /** Largest zoom factor scroll-in is allowed to reach. */
    public static final float MAX_ZOOM = 4.0f;
    /** Multiplicative factor per scroll tick. */
    public static final float ZOOM_STEP = 1.1f;

    /** Translation in screen pixels, applied before {@link #zoom}. */
    private float panX = 0f;
    private float panY = 0f;
    /** Uniform scale factor; 1.0 = no zoom. */
    private float zoom = 1f;

    /**
     * Reset the view so the canvas origin is at the screen origin and
     * zoom is 1. Doesn't reset where individual nodes sit — only the
     * view onto them.
     */
    public void resetView() {
        this.panX = 0f;
        this.panY = 0f;
        this.zoom = 1f;
    }

    /**
     * Apply the viewport's pan + zoom to {@code pose}. Pushes a
     * translate followed by a uniform scale so subsequent draws operate
     * in canvas-space and end up on screen at the right place. Caller
     * is responsible for pushing / popping the matrix around this call.
     */
    public void transformPose(Matrix3x2fStack pose) {
        pose.translate(this.panX, this.panY);
        pose.scale(this.zoom, this.zoom);
    }

    /** Translate the view by ({@code x}, {@code y}) screen pixels. */
    public void pan(float x, float y) {
        this.panX += x;
        this.panY += y;
    }

    public float zoom() {
        return this.zoom;
    }

    /** Additive zoom — rarely the right call site; prefer {@link #zoomAround}. */
    public void zoom(float zoom) {
        this.zoom += zoom;
    }

    /**
     * Set the zoom level to {@code newZoom} (clamped to the configured range)
     * while keeping the canvas point currently under {@code (screenX, screenY)}
     * stationary in screen space. Compensates {@link #panX}/{@link #panY} so a
     * scroll-to-zoom over the cursor doesn't drift the view.
     *
     * <p>Derivation: the canvas point under the cursor before the zoom change
     * is {@code (screenX - oldPan) / oldZoom}. We want the same canvas point
     * to remain at {@code (screenX, screenY)} afterward, i.e.,
     * {@code newPan = screenX - canvasPoint * newZoom = screenX - (screenX - oldPan) * (newZoom / oldZoom)}.
     */
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

    // -- Coordinate conversion ----------------------------------------------

    public Vector2dc screenToCanvas(double screenX, double screenY) {
        return screenToCanvas(new Vector2d(screenX, screenY));
    }

    public Vector2dc screenToCanvas(Vector2dc screen) {
        return new Vector2d((screen.x() - this.panX) / this.zoom, (screen.y() - this.panY) / this.zoom);
    }

    /**
     * Maps a canvas-space point to its on-screen position under the
     * current pan / zoom. Inverse of {@link #screenToCanvas} — used by
     * node widgets that need a screen-space anchor for floating UI
     * (e.g., property dropdown popups) without doing the math inline.
     */
    public Vector2dc canvasToScreen(double canvasX, double canvasY) {
        return new Vector2d(canvasX * this.zoom + this.panX, canvasY * this.zoom + this.panY);
    }
}
