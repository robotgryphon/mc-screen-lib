package dev.robotgryphon.screenlib.client.ui.widget;

import dev.robotgryphon.screenlib.client.NodeRenderer;
import dev.robotgryphon.screenlib.client.PortLayout;
import dev.robotgryphon.screenlib.client.ui.render.uniforms.NodeBackgroundUniform;
import dev.robotgryphon.screenlib.graph.Canvas;
import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.ports.Port;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.core.Holder;
import org.joml.Vector2dc;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;

public class NodeWidget extends AbstractWidget {
    private static final int BACKGROUND_COLOR = 0xCC1F1F23;
    private static final int BACKGROUND_HOVER_COLOR = 0xCC2A2A33;
    private static final int BORDER_COLOR = 0xFF7F7F8C;
    private static final int BORDER_DRAG_COLOR = 0xFFFFD24A;
    private static final int TITLE_BAR_COLOR = 0xFF3A3A45;

    public static final int INVALID_GLOW_COLOR = 0x99FF4D4D;

    private static final int INVALID_BORDER_COLOR = 0xFFA13838;

    static final int NODE_CORNER_RADIUS = 4;

    private static final float MISMATCHED_TYPE_ALPHA = 0.3f;

    private final Node node;
    private final PortLayout layout;
    private final DragState dragState;
    private final NodePortsWidget ports;
    private final NodePropertiesWidget properties;

    private boolean dragging;
    private double grabOffsetX;
    private double grabOffsetY;

    private @Nullable Integer glowColor;


    public NodeWidget(Node node, DragState dragState) {
        this(node, dragState, PortLayout.compile(node.portManager().ports(), Minecraft.getInstance().font));
    }

    private NodeWidget(Node node, DragState dragState, PortLayout layout) {
        super(node.canvas.x(node), node.canvas.y(node), width(node, layout), height(node, layout), node.title);
        this.node = node;
        this.dragState = dragState;
        this.layout = layout;
        this.ports = new NodePortsWidget(this, node.portManager(), layout);
        this.properties = new NodePropertiesWidget(this, node.propertyManager());
    }

    private static int width(Node node, PortLayout layout) {
        return NodeRenderer.computeWidth(Minecraft.getInstance().font, node.title,
                layout, node.propertyManager().properties());
    }

    private static int height(Node node, PortLayout layout) {
        return NodeRenderer.TITLE_BAR_HEIGHT + layout.bandHeight()
                + NodeRenderer.propertyRegionHeight(node.propertyManager().properties().size());
    }

    @Override
    public int getX() {
        return this.node.canvas.x(this.node);
    }

    @Override
    public int getY() {
        return this.node.canvas.y(this.node);
    }

    @Override
    public void setX(int x) {
        this.node.canvas.place(this.node, x, this.getY());
    }

    @Override
    public void setY(int y) {
        this.node.canvas.place(this.node, this.getX(), y);
    }

    PortLayout layout() {
        return this.layout;
    }

    public Node node() {
        return this.node;
    }

    public boolean isDragging() {
        return this.dragging;
    }

    public @Nullable Integer glowColor() {
        return this.glowColor;
    }

    public void setGlowColor(@Nullable Integer color) {
        this.glowColor = color;
    }

    public @Nullable Integer effectiveGlowColor() {
        if (this.glowColor != null) return this.glowColor;
        if (this.node.canvas.isInvalid(this.node)) {
            return INVALID_GLOW_COLOR;
        }
        return null;
    }


    public @Nullable Integer tintColor() {
        return this.node.canvas.tint(this.node);
    }

    public void setTintColor(@Nullable Integer color) {
        this.node.canvas.setTint(this.node, color);
    }

    public ScreenRectangle screenBounds(CanvasViewport viewport) {
        Vector2dc tl = viewport.canvasToScreen(this.getX(), this.getY());
        Vector2dc br = viewport.canvasToScreen(
                this.getX() + this.getWidth(),
                this.getY() + this.getHeight());
        int sx0 = (int) Math.floor(tl.x());
        int sy0 = (int) Math.floor(tl.y());
        int sx1 = (int) Math.ceil(br.x());
        int sy1 = (int) Math.ceil(br.y());
        return new ScreenRectangle(sx0, sy0, sx1 - sx0, sy1 - sy0);
    }

    public NodeBackgroundUniform.Entry buildBackgroundEntry(CanvasViewport viewport,
                                                            double textureOriginX,
                                                            double textureOriginY,
                                                            float guiScale,
                                                            boolean dropShadow) {
        Vector2dc tl = viewport.canvasToScreen(this.getX(), this.getY());
        Vector2dc br = viewport.canvasToScreen(
                this.getX() + this.getWidth(),
                this.getY() + this.getHeight());

        float relX = (float) ((tl.x() - textureOriginX) * guiScale);
        float relY = (float) ((tl.y() - textureOriginY) * guiScale);
        float w = (float) ((br.x() - tl.x()) * guiScale);
        float h = (float) ((br.y() - tl.y()) * guiScale);

        boolean hovered = this.isHovered() || this.dragging;
        int bodyArgb = hovered ? BACKGROUND_HOVER_COLOR : BACKGROUND_COLOR;
        int titleArgb = TITLE_BAR_COLOR;
        int borderArgb;
        if (this.dragging) {
            borderArgb = BORDER_DRAG_COLOR;
        } else if (this.node.canvas.isInvalid(this.node)) {
            borderArgb = INVALID_BORDER_COLOR;
        } else {
            borderArgb = BORDER_COLOR;
        }

        Integer tint = this.tintColor();
        if (tint != null) {
            bodyArgb = blendTint(bodyArgb, tint);
            titleArgb = blendTint(titleArgb, tint);
        }

        float titleHeight = NodeRenderer.TITLE_BAR_HEIGHT * viewport.zoom() * guiScale;

        Integer effectiveGlow = this.effectiveGlowColor();
        Vector4fc glowVec = effectiveGlow != null
                ? argbToVec(effectiveGlow)
                : new Vector4f(0f, 0f, 0f, 0f);

        return new NodeBackgroundUniform.Entry(
                new Vector4f(relX, relY, w, h),
                argbToVec(bodyArgb),
                argbToVec(titleArgb),
                argbToVec(borderArgb),
                titleHeight,
                dropShadow,
                0f, 0f,
                glowVec);
    }

    public boolean hasVisibleGlow() {
        Integer color = this.effectiveGlowColor();
        return color != null && ((color >>> 24) & 0xFF) > 0;
    }

    private static final float TINT_STRENGTH = 0.35f;

    private static int blendTint(int base, int tint) {
        int br = (base >> 16) & 0xFF;
        int bg = (base >> 8) & 0xFF;
        int bb = base & 0xFF;
        int ba = (base >>> 24) & 0xFF;
        int tr = (tint >> 16) & 0xFF;
        int tg = (tint >> 8) & 0xFF;
        int tb = tint & 0xFF;
        float s = TINT_STRENGTH;
        int r = Math.round(br * (1f - s) + tr * s);
        int g = Math.round(bg * (1f - s) + tg * s);
        int b = Math.round(bb * (1f - s) + tb * s);
        return (ba << 24) | (r << 16) | (g << 8) | b;
    }

    private static Vector4fc argbToVec(int argb) {
        return new Vector4f(
                ARGB.redFloat(argb),
                ARGB.greenFloat(argb),
                ARGB.blueFloat(argb),
                ARGB.alphaFloat(argb));
    }

    public @Nullable Port portAt(double mouseX, double mouseY) {
        return this.ports.portAt(mouseX, mouseY);
    }

    public NodePropertiesWidget properties() {
        return this.properties;
    }

    public NodePortsWidget ports() {
        return this.ports;
    }

    Canvas canvas() {
        return this.node.canvas;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.isValidClickButton(event.buttonInfo())) {
            if (this.portAt(event.x(), event.y()) != null) {
                return false;
            }
            if (this.properties.handleEditorClick(event, doubleClick)) {
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        super.onClick(event, doubleClick);
        this.dragging = true;
        this.grabOffsetX = event.x() - this.getX();
        this.grabOffsetY = event.y() - this.getY();
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dx, double dy) {
        if (!this.dragging) {
            return;
        }
        int newX = (int) Math.round(event.x() - this.grabOffsetX);
        int newY = (int) Math.round(event.y() - this.grabOffsetY);
        this.node.canvas.place(this.node, newX, newY);
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        super.onRelease(event);
        this.dragging = false;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int left = this.getX();
        int top = this.getY();

        Font font = Minecraft.getInstance().font;
        int titleColor = ARGB.color((int) (255 * this.getAlpha()), 0xFF, 0xFF, 0xFF);
        graphics.centeredText(font, this.node.title, left + this.getWidth() / 2, top + 2, titleColor);

        this.properties.extractRenderState(graphics, mouseX, mouseY, partialTick);

        this.ports.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    float effectiveAlpha(Holder<GraphDataType<?>> type) {
        float base = this.getAlpha();
        Holder<GraphDataType<?>> active = this.dragState.type();
        if (active == null) {
            return base;
        }
        return type.value() == active.value() ? base : base * MISMATCHED_TYPE_ALPHA;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
