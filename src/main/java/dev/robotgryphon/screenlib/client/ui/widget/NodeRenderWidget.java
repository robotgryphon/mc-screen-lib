package dev.robotgryphon.screenlib.client.ui.widget;

import dev.robotgryphon.screenlib.client.NodeRenderer;
import dev.robotgryphon.screenlib.client.render.NodeRenderers;
import dev.robotgryphon.screenlib.client.render.NodeStateRenderer;
import dev.robotgryphon.screenlib.client.render.NodeTooltip;
import dev.robotgryphon.screenlib.client.render.RenderArea;
import dev.robotgryphon.screenlib.graph.nodes.RenderManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.List;
import java.util.Optional;

public class NodeRenderWidget implements Renderable {
    private final NodeWidget owner;
    private final RenderManager renderManager;

    public NodeRenderWidget(NodeWidget owner, RenderManager renderManager) {
        this.owner = owner;
        this.renderManager = renderManager;
    }

    public static List<Integer> heights(RenderManager renderManager) {
        return renderManager.providers().stream().map(provider -> provider.value().height()).toList();
    }

    public static int maxWidth(RenderManager renderManager) {
        return renderManager.providers().stream().mapToInt(provider -> provider.value().width()).max().orElse(0);
    }

    public int top() {
        return this.owner.properties().top() + this.owner.properties().height();
    }

    private RenderArea area(int providerIndex) {
        int y = this.top() + NodeRenderer.RENDER_REGION_PADDING;
        var providers = this.renderManager.providers();
        for (int i = 0; i < providerIndex; i++) {
            y += providers.get(i).value().height() + NodeRenderer.RENDER_REGION_PADDING;
        }
        int width = providers.get(providerIndex).value().width();
        int height = providers.get(providerIndex).value().height();
        return new RenderArea(this.owner.getX() + (this.owner.getWidth() - width) / 2, y, width, height);
    }

    @FunctionalInterface
    private interface Visitor<R> {
        R visit(NodeStateRenderer<Object> renderer, Object state, RenderArea area);
    }

    private <R> Optional<R> firstResult(Visitor<Optional<R>> visitor) {
        var providers = this.renderManager.providers();
        for (int i = 0; i < providers.size(); i++) {
            Object state = this.renderManager.state(i);
            NodeStateRenderer<Object> renderer = NodeRenderers.get(providers.get(i));
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
        var providers = this.renderManager.providers();
        for (int i = 0; i < providers.size(); i++) {
            Object state = this.renderManager.state(i);
            NodeStateRenderer<Object> renderer = NodeRenderers.get(providers.get(i));
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
                area.contains(event.x(), event.y()) && renderer.mouseClicked(state, area, event, doubleClick)
                        ? Optional.of(true) : Optional.<Boolean>empty()).isPresent();
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return this.firstResult((renderer, state, area) ->
                area.contains(mouseX, mouseY) && renderer.mouseScrolled(state, area, mouseX, mouseY, scrollX, scrollY)
                        ? Optional.of(true) : Optional.<Boolean>empty()).isPresent();
    }

    public Optional<NodeTooltip> tooltip(double mouseX, double mouseY) {
        return this.firstResult((renderer, state, area) ->
                area.contains(mouseX, mouseY) ? renderer.tooltip(state, area, mouseX, mouseY) : Optional.<NodeTooltip>empty());
    }
}
