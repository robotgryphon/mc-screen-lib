package dev.robotgryphon.screenlib.client.ui.widget;

import dev.robotgryphon.screenlib.client.NodeRenderer;
import dev.robotgryphon.screenlib.client.render.NodeWidgets;
import dev.robotgryphon.screenlib.client.render.NodeStateRenderer;
import dev.robotgryphon.screenlib.graph.nodes.RenderManager;
import dev.robotgryphon.screenlib.graph.schema.node.WidgetDefinition;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class NodeRenderWidget implements Renderable {
    private final NodeWidget owner;
    private final RenderManager renderManager;

    public NodeRenderWidget(NodeWidget owner, RenderManager renderManager) {
        this.owner = owner;
        this.renderManager = renderManager;
    }

    private static @Nullable NodeStateRenderer<Object> renderer(WidgetDefinition widget) {
        return NodeWidgets.get(widget.type());
    }

    public static List<Integer> heights(RenderManager renderManager) {
        return renderManager.widgets().stream()
                .map(NodeRenderWidget::renderer)
                .filter(Objects::nonNull)
                .map(NodeStateRenderer::height)
                .toList();
    }

    public static int maxWidth(RenderManager renderManager) {
        return renderManager.widgets().stream()
                .map(NodeRenderWidget::renderer)
                .filter(Objects::nonNull)
                .mapToInt(NodeStateRenderer::width)
                .max().orElse(0);
    }

    public int top() {
        return this.owner.properties().top() + this.owner.properties().height();
    }

    private ScreenRectangle area(int entryIndex) {
        int y = this.top() + NodeRenderer.RENDER_REGION_PADDING;
        var widgets = this.renderManager.widgets();
        for (int i = 0; i < entryIndex; i++) {
            var previous = renderer(widgets.get(i));
            if (previous != null) {
                y += previous.height() + NodeRenderer.RENDER_REGION_PADDING;
            }
        }
        var renderer = renderer(widgets.get(entryIndex));
        int width = renderer == null ? 0 : renderer.width();
        int height = renderer == null ? 0 : renderer.height();
        return new ScreenRectangle(this.owner.getX() + (this.owner.getWidth() - width) / 2, y, width, height);
    }

    @FunctionalInterface
    private interface Visitor<R> {
        R visit(NodeStateRenderer<Object> renderer, Object state, ScreenRectangle area);
    }

    private <R> Optional<R> firstResult(Visitor<Optional<R>> visitor) {
        var widgets = this.renderManager.widgets();
        for (int i = 0; i < widgets.size(); i++) {
            Object state = this.renderManager.state(widgets.get(i).property());
            NodeStateRenderer<Object> renderer = renderer(widgets.get(i));
            if (state == null || renderer == null) {
                continue;
            }
            Optional<R> result = visitor.visit(renderer, state, this.area(i));
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        var widgets = this.renderManager.widgets();
        for (int i = 0; i < widgets.size(); i++) {
            Object state = this.renderManager.state(widgets.get(i).property());
            NodeStateRenderer<Object> renderer = renderer(widgets.get(i));
            if (state != null && renderer != null) {
                renderer.render(graphics, state, this.area(i), mouseX, mouseY, this.owner.getAlpha());
            }
        }
    }

    public void mouseMoved(double mouseX, double mouseY) {
        this.firstResult((renderer, state, area) -> {
            renderer.mouseMoved(state, area, mouseX, mouseY);
            return Optional.<Boolean>empty();
        });
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return this.firstResult((renderer, state, area) ->
                contains(area, event.x(), event.y()) && renderer.mouseClicked(state, area, event, doubleClick)
                        ? Optional.of(true) : Optional.<Boolean>empty()).isPresent();
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return this.firstResult((renderer, state, area) ->
                contains(area, mouseX, mouseY) && renderer.mouseScrolled(state, area, mouseX, mouseY, scrollX, scrollY)
                        ? Optional.of(true) : Optional.<Boolean>empty()).isPresent();
    }

    public void tooltip(GuiGraphicsExtractor graphics, double mouseX, double mouseY, int screenX, int screenY) {
        this.firstResult((renderer, state, area) -> {
            if (!contains(area, mouseX, mouseY)) {
                return Optional.<Boolean>empty();
            }
            renderer.tooltip(graphics, state, area, Mth.floor(mouseX), Mth.floor(mouseY), screenX, screenY);
            return Optional.of(true);
        });
    }

    private static boolean contains(ScreenRectangle area, double x, double y) {
        return area.containsPoint(Mth.floor(x), Mth.floor(y));
    }
}
