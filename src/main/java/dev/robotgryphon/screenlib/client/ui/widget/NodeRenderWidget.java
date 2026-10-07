package dev.robotgryphon.screenlib.client.ui.widget;

import dev.robotgryphon.screenlib.client.NodeRenderer;
import dev.robotgryphon.screenlib.client.render.NodeRenderers;
import dev.robotgryphon.screenlib.client.render.NodeStateRenderer;
import dev.robotgryphon.screenlib.graph.nodes.RenderManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;

import java.util.List;

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

    @Override
    @SuppressWarnings("unchecked")
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int y = this.top() + NodeRenderer.RENDER_REGION_PADDING;
        var providers = this.renderManager.providers();
        for (int i = 0; i < providers.size(); i++) {
            var provider = providers.get(i);
            int width = provider.value().width();
            int height = provider.value().height();
            Object state = this.renderManager.state(i);
            NodeStateRenderer<Object> renderer = NodeRenderers.get(provider);
            if (state != null && renderer != null) {
                int x = this.owner.getX() + (this.owner.getWidth() - width) / 2;
                renderer.render(graphics, state, x, y, width, height, this.owner.getAlpha());
            }
            y += height + NodeRenderer.RENDER_REGION_PADDING;
        }
    }
}
