package dev.robotgryphon.screenlib.client.ui.widget.node;

import dev.robotgryphon.screenlib.client.appearance.NodeAppearance;
import dev.robotgryphon.screenlib.client.appearance.NodeAppearances;

import dev.robotgryphon.screenlib.graph.canvas.client.CanvasViewport;

import dev.robotgryphon.screenlib.graph.nodes.properties.schema.ComputedPropertyDefinition;
import com.mojang.serialization.Codec;
import dev.robotgryphon.screenlib.client.NodeRenderer;
import dev.robotgryphon.screenlib.client.render.PropertyRenderers;
import dev.robotgryphon.screenlib.client.render.PropertyValueRenderer;
import dev.robotgryphon.screenlib.client.ui.widget.property.BooleanPropertyEditor;
import dev.robotgryphon.screenlib.client.ui.widget.property.DropdownEditor;
import dev.robotgryphon.screenlib.client.ui.widget.property.DropdownPopup;
import dev.robotgryphon.screenlib.client.ui.widget.property.NumericPropertyEditor;
import dev.robotgryphon.screenlib.client.ui.widget.property.PropertyEditor;
import dev.robotgryphon.screenlib.client.ui.widget.property.PropertyValues;
import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.nodes.properties.schema.PropertyDefinition;
import dev.robotgryphon.screenlib.graph.nodes.properties.schema.StoredPropertyDefinition;
import dev.robotgryphon.screenlib.graph.nodes.properties.PropertyManager;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.layouts.EqualSpacingLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.joml.Vector2dc;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class NodePropertiesRenderer implements Renderable {
    private static final int PROPERTY_LABEL_COLOR = 0xFFA0A4B3;
    private static final int PROPERTY_LABEL_DRIVEN_COLOR = 0xFF6E7180;
    private static final int PROPERTY_LABEL_UNDRIVEN_COLOR = 0xFFE26060;
    private static final int PROPERTY_VALUE_COLOR = 0xFFFFFFFF;
    private static final int PROPERTY_VALUE_DRIVEN_COLOR = 0xFFC2C5CE;
    private static final String PROPERTY_UNSET_PLACEHOLDER = "—";

    private final NodeWidget owner;
    private final Node node;
    private final PropertyManager propertyManager;

    private final @Nullable EqualSpacingLayout propertiesLayout;

    private final List<PropertyRow> propertyRows;

    public NodePropertiesRenderer(NodeWidget owner) {
        this.owner = owner;
        this.node = owner.node();
        this.propertyManager = node.propertyManager();

        if (rowCount(this.node) == 0) {
            this.propertiesLayout = null;
            this.propertyRows = List.of();
        } else {
            int n = rowCount(this.node);
            int contentHeight = n * NodeRenderer.PROPERTY_PITCH
                    + Math.max(0, n - 1) * NodeRenderer.PROPERTY_ROW_GAP;
            EqualSpacingLayout layout = new EqualSpacingLayout(
                    this.owner.getWidth(), contentHeight,
                    EqualSpacingLayout.Orientation.VERTICAL);
            List<PropertyRow> rows = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                PropertyRow row = new PropertyRow(i);
                rows.add(row);
                layout.addChild(row);
            }
            this.propertiesLayout = layout;
            this.propertyRows = List.copyOf(rows);
        }
    }

    public static List<ComputedPropertyDefinition> visibleComputed(Node node) {
        NodeAppearance appearance = NodeAppearances.get(node.type());
        return node.propertyManager().computedProperties().stream()
                .filter(definition -> !appearance.property(definition.name()).hideLabel())
                .toList();
    }

    public static Optional<PropertyValueRenderer<Object>> valueRenderer(Node node, PropertyDefinition definition) {
        return NodeAppearances.get(node.type()).property(definition.name()).renderer().map(PropertyRenderers::get);
    }

    public static int rowCount(Node node) {
        return node.propertyManager().stored().size() + visibleComputed(node).size();
    }

    public int width() {
        return this.owner.getWidth();
    }

    public int height() {
        return NodeRenderer.propertyRegionHeight(rowCount(this.node));
    }

    public int top() {
        return this.owner.getY() + NodeRenderer.TITLE_BAR_HEIGHT + this.owner.layout().bandHeight();
    }

    public int rowTop(int index) {
        return this.top() + NodeRenderer.PROPERTY_REGION_PADDING_Y
                + index * (NodeRenderer.PROPERTY_PITCH + NodeRenderer.PROPERTY_ROW_GAP);
    }

    public boolean handleEditorClick(MouseButtonEvent event, boolean doubleClick) {
        List<StoredPropertyDefinition<?>> properties = this.propertyManager.stored();
        if (properties.isEmpty()) {
            return false;
        }

        for (int i = 0; i < properties.size(); i++) {
            StoredPropertyDefinition<?> prop = properties.get(i);
            if (this.propertyManager.isDriven(prop.name())) continue;

            PropertyEditor editor = this.propertyRows.get(i).editor();
            if (editor == null) continue;

            this.propertyRows.get(i).syncEditorValueFromNode();

            if (editor.mouseClicked(event, doubleClick)) {
                return true;
            }
        }
        return false;
    }

    private boolean isDropdownProperty(StoredPropertyDefinition<?> prop) {
        return this.node.canvas.allowedValues(prop.anyType()).isPresent();
    }

    public boolean hasFocusedPropertyPopup() {
        return resolveFocusedProperty() != null;
    }

    public void clearFocusedProperty() {
        this.owner.state().setFocusedProperty(this.node, null);
    }

    public @Nullable NumericPropertyEditor activeNumericEditor() {
        for (PropertyRow row : this.propertyRows) {
            if (row.editor() instanceof NumericPropertyEditor editor && editor.isEditing()) {
                return editor;
            }
        }
        return null;
    }

    public void renderFocusedPropertyPopup(GuiGraphicsExtractor graphics, CanvasViewport viewport,
                                           int mouseX, int mouseY) {
        DropdownPopup popup = buildFocusedPropertyPopup(viewport);
        if (popup != null) {
            popup.render(graphics, mouseX, mouseY);
        }
    }

    public boolean handleFocusedPropertyPopupClick(CanvasViewport viewport,
                                                   double screenMouseX, double screenMouseY) {
        DropdownPopup popup = buildFocusedPropertyPopup(viewport);
        if (popup == null) return false;
        return popup.mouseClicked(screenMouseX, screenMouseY);
    }

    private @Nullable FocusedRow resolveFocusedProperty() {
        String name = this.owner.state().focusedProperty(this.node);
        if (name == null) {
            return null;
        }
        List<StoredPropertyDefinition<?>> properties = this.propertyManager.stored();
        for (int i = 0; i < properties.size(); i++) {
            StoredPropertyDefinition<?> prop = properties.get(i);
            if (prop.name().equals(name) && isDropdownProperty(prop)) {
                return new FocusedRow(prop, i);
            }
        }
        return null;
    }

    private record FocusedRow(StoredPropertyDefinition<?> prop, int rowIndex) {}

    private @Nullable DropdownPopup buildFocusedPropertyPopup(CanvasViewport viewport) {
        FocusedRow focused = resolveFocusedProperty();
        if (focused == null) {
            return null;
        }
        StoredPropertyDefinition<?> prop = focused.prop();

        GraphDataType<?> def = prop.type().value();
        List<Object> options = new ArrayList<>();
        options.add(PropertyValues.CLEAR);
        options.addAll(this.node.canvas.allowedValues(prop.anyType()).orElseThrow());

        String name = prop.name();
        Object currentValue = this.propertyManager.value(name);

        int editorWidth = this.owner.valueWidth(focused.rowIndex());
        int editorHeight = NodeRenderer.PROPERTY_PITCH;
        int editorX = this.owner.getX() + this.owner.getWidth()
                - NodeRenderer.PROPERTY_PADDING_X - editorWidth;
        int editorY = this.rowTop(focused.rowIndex());

        Vector2dc screenAnchor = viewport.canvasToScreen(editorX, editorY + editorHeight);
        int popupX = (int) screenAnchor.x();
        int popupY = (int) screenAnchor.y();
        int popupWidth = Math.max(editorWidth, (int) (editorWidth * viewport.zoom()));

        final String propName = name;
        return new DropdownPopup(popupX, popupY, popupWidth,
                options, currentValue, picked -> {
                    this.propertyManager.set(propName, picked == PropertyValues.CLEAR ? null : picked);
                    this.owner.state().setFocusedProperty(this.node, null);
                });
    }

    private static boolean isNumericCodec(Codec<?> codec) {
        return codec == Codec.INT || codec == Codec.FLOAT || codec == Codec.DOUBLE;
    }

    private Number currentNumericValue(StoredPropertyDefinition<?> prop) {
        Object current = this.propertyManager.value(prop.name());
        Codec<?> codec = prop.type().value().codec();
        if (codec == Codec.INT) return current instanceof Integer iv ? iv : 0;
        if (codec == Codec.FLOAT) return current instanceof Float fv ? fv : 0f;
        if (codec == Codec.DOUBLE) return current instanceof Double dv ? dv : 0.0;
        return 0;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (this.propertiesLayout == null) {
            return;
        }
        this.propertiesLayout.setX(this.owner.getX());
        this.propertiesLayout.setY(this.top() + NodeRenderer.PROPERTY_REGION_PADDING_Y);
        this.propertiesLayout.arrangeElements();
        for (PropertyRow row : this.propertyRows) {
            row.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void renderPropertyRow(GuiGraphicsExtractor graphics, Font font,
                                   int rowIndex, int rowLeft, int rowTop,
                                   int rowWidth, int rowHeight,
                                   int mouseX, int mouseY) {
        if (rowIndex >= this.propertyManager.stored().size()) {
            this.renderComputedRow(graphics, font, rowIndex, rowLeft, rowTop, rowWidth, rowHeight);
            return;
        }
        StoredPropertyDefinition<?> prop = this.propertyManager.stored().get(rowIndex);

        boolean driven = this.propertyManager.isDriven(prop.name());
        Object upstreamValue = driven ? this.resolveDrivenValue(prop) : null;
        boolean undriven = driven && upstreamValue == null;

        int labelColor = undriven
                ? PROPERTY_LABEL_UNDRIVEN_COLOR
                : (driven ? PROPERTY_LABEL_DRIVEN_COLOR : PROPERTY_LABEL_COLOR);
        int valueColor = driven ? PROPERTY_VALUE_DRIVEN_COLOR : PROPERTY_VALUE_COLOR;

        float alpha = this.owner.effectiveAlpha(prop.anyType());

        int textY = rowTop + (rowHeight - font.lineHeight) / 2 + 1;

        if (!NodeAppearances.get(this.node.type()).property(prop.name()).hideLabel()) {
            graphics.text(font, NodeRenderer.propertyLabel(this.node, prop.name()),
                    rowLeft + NodeRenderer.PROPERTY_PADDING_X,
                    textY,
                    ARGB.multiply(labelColor, ARGB.white(alpha)),
                    false);
        }

        PropertyRow row = this.propertyRows.get(rowIndex);
        PropertyEditor editor = row.editor();

        if (!driven && editor != null) {
            row.syncEditorValueFromNode();
            editor.setAlpha(alpha);
            editor.extractRenderState(graphics, mouseX, mouseY, 0f);
        } else if (!this.renderValue(graphics, prop, undriven ? null : (driven ? upstreamValue : this.propertyManager.value(prop.name())),
                rowLeft, rowTop, rowWidth, rowHeight, mouseX, mouseY, alpha)) {
            String valueText;
            if (undriven) {
                valueText = "";
            } else if (driven) {
                valueText = formatPropertyValue(upstreamValue);
            } else {
                valueText = formatPropertyValue(this.propertyManager.value(prop.name()));
            }

            if (!valueText.isEmpty()) {
                int valueWidth = font.width(valueText);
                graphics.text(font, Component.literal(valueText),
                        rowLeft + rowWidth - NodeRenderer.PROPERTY_PADDING_X - valueWidth,
                        textY,
                        ARGB.multiply(valueColor, ARGB.white(alpha)),
                        false);
            }
        }
    }

    private void renderComputedRow(GuiGraphicsExtractor graphics, Font font,
                                   int rowIndex, int rowLeft, int rowTop, int rowWidth, int rowHeight) {
        var definition = visibleComputed(this.node)
                .get(rowIndex - this.propertyManager.stored().size());
        float alpha = this.owner.effectiveAlpha(definition.type());
        int textY = rowTop + (rowHeight - font.lineHeight) / 2 + 1;

        graphics.text(font, NodeRenderer.propertyLabel(this.node, definition.name()),
                rowLeft + NodeRenderer.PROPERTY_PADDING_X, textY,
                ARGB.multiply(PROPERTY_LABEL_DRIVEN_COLOR, ARGB.white(alpha)), false);

        Object value = this.propertyManager.computedValue(definition.name());
        if (this.renderValue(graphics, definition, value, rowLeft, rowTop, rowWidth, rowHeight, -1, -1, alpha)) {
            return;
        }
        String valueText = formatPropertyValue(value);
        graphics.text(font, Component.literal(valueText),
                rowLeft + rowWidth - NodeRenderer.PROPERTY_PADDING_X - font.width(valueText), textY,
                ARGB.multiply(PROPERTY_VALUE_DRIVEN_COLOR, ARGB.white(alpha)), false);
    }

    private boolean renderValue(GuiGraphicsExtractor graphics, PropertyDefinition definition, @Nullable Object value,
                                int rowLeft, int rowTop, int rowWidth, int rowHeight,
                                int mouseX, int mouseY, float alpha) {
        var renderer = valueRenderer(this.node, definition).orElse(null);
        if (renderer == null || !renderer.valueType().isInstance(value)) {
            return false;
        }
        return renderer.render(graphics, value, valueArea(renderer, rowLeft, rowTop, rowWidth, rowHeight),
                mouseX, mouseY, alpha);
    }

    private static ScreenRectangle valueArea(PropertyValueRenderer<?> renderer,
                                             int rowLeft, int rowTop, int rowWidth, int rowHeight) {
        return new ScreenRectangle(rowLeft + rowWidth - NodeRenderer.PROPERTY_PADDING_X - renderer.width(),
                rowTop, renderer.width(), rowHeight);
    }

    private @Nullable Object displayedValue(int rowIndex) {
        List<StoredPropertyDefinition<?>> stored = this.propertyManager.stored();
        if (rowIndex >= stored.size()) {
            var definition = visibleComputed(this.node).get(rowIndex - stored.size());
            return this.propertyManager.computedValue(definition.name());
        }
        StoredPropertyDefinition<?> prop = stored.get(rowIndex);
        if (this.propertyManager.isDriven(prop.name())) {
            return this.resolveDrivenValue(prop);
        }
        return this.propertyRows.get(rowIndex).editor() == null ? this.propertyManager.value(prop.name()) : null;
    }

    private PropertyDefinition definition(int rowIndex) {
        List<StoredPropertyDefinition<?>> stored = this.propertyManager.stored();
        return rowIndex < stored.size()
                ? stored.get(rowIndex)
                : visibleComputed(this.node).get(rowIndex - stored.size());
    }

    public boolean tooltip(GuiGraphicsExtractor graphics, double mouseX, double mouseY, int screenX, int screenY) {
        int x = Mth.floor(mouseX);
        int y = Mth.floor(mouseY);
        for (PropertyRow row : this.propertyRows) {
            var renderer = valueRenderer(this.node, this.definition(row.rowIndex)).orElse(null);
            if (renderer == null) {
                continue;
            }
            ScreenRectangle area = valueArea(renderer, row.getX(), row.getY(), row.getWidth(), row.getHeight());
            if (!area.containsPoint(x, y)) {
                continue;
            }
            Object value = this.displayedValue(row.rowIndex);
            return renderer.valueType().isInstance(value)
                    && renderer.tooltip(graphics, value, area, x, y, screenX, screenY);
        }
        return false;
    }

    private final class PropertyRow implements LayoutElement, Renderable {
        private final int rowIndex;

        private final @Nullable PropertyEditor editor;

        private int x;
        private int y;
        private int width;
        private int height;

        PropertyRow(int rowIndex) {
            this.rowIndex = rowIndex;
            this.x = 0;
            this.y = 0;
            this.width = NodePropertiesRenderer.this.owner.getWidth();
            this.height = NodeRenderer.PROPERTY_PITCH;

            this.editor = buildEditor(rowIndex);
        }

        private @Nullable PropertyEditor buildEditor(int rowIndex) {
            if (rowIndex >= NodePropertiesRenderer.this.propertyManager.stored().size()) {
                return null;
            }
            StoredPropertyDefinition<?> prop = NodePropertiesRenderer.this.propertyManager.stored().get(rowIndex);
            Codec<?> codec = prop.type().value().codec();
            final String propName = prop.name();
            int editorW = NodePropertiesRenderer.this.owner.valueWidth(rowIndex);
            int editorH = NodeRenderer.PROPERTY_PITCH;
            if (isNumericCodec(codec)) {
                Number seed = NodePropertiesRenderer.this.currentNumericValue(prop);
                return new NumericPropertyEditor(0, 0, editorW, editorH,
                        seed, codec,
                        next -> NodePropertiesRenderer.this.propertyManager.set(propName, next));
            }
            if (isDropdownProperty(prop)) {
                Object current = NodePropertiesRenderer.this.propertyManager.value(propName);
                return new DropdownEditor(0, 0, editorW, editorH, current,
                        () -> NodePropertiesRenderer.this.owner.state().setFocusedProperty(NodePropertiesRenderer.this.node, propName));
            }
            if (codec == Codec.BOOL) {
                Object current = NodePropertiesRenderer.this.propertyManager.value(propName);
                boolean seed = current instanceof Boolean b && b;
                return new BooleanPropertyEditor(0, 0, editorW, editorH, seed,
                        next -> NodePropertiesRenderer.this.propertyManager.set(propName, next));
            }
            return null;
        }

        @Nullable PropertyEditor editor() {
            return this.editor;
        }

        void syncEditorValueFromNode() {
            if (this.editor == null) return;
            StoredPropertyDefinition<?> prop = NodePropertiesRenderer.this.propertyManager.stored().get(this.rowIndex);
            Object current = NodePropertiesRenderer.this.propertyManager.value(prop.name());
            if (this.editor instanceof NumericPropertyEditor n) {
                n.setValue(NodePropertiesRenderer.this.currentNumericValue(prop));
            } else if (this.editor instanceof DropdownEditor d) {
                d.setValue(current);
            } else if (this.editor instanceof BooleanPropertyEditor b) {
                b.setValue(current instanceof Boolean v && v);
            }
        }

        @Override public int getX() { return this.x; }
        @Override public int getY() { return this.y; }
        @Override public int getWidth() { return this.width; }
        @Override public int getHeight() { return this.height; }

        @Override
        public void setX(int x) {
            this.x = x;
            if (this.editor != null) {
                this.editor.setX(x + this.width
                        - NodeRenderer.PROPERTY_PADDING_X - this.editor.getWidth());
            }
        }

        @Override
        public void setY(int y) {
            this.y = y;
            if (this.editor != null) {
                this.editor.setY(y);
            }
        }

        @Override
        public void visitWidgets(Consumer<AbstractWidget> visitor) {
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics,
                                       int mouseX, int mouseY, float partialTick) {
            Font font = Minecraft.getInstance().font;
            NodePropertiesRenderer.this.renderPropertyRow(graphics, font,
                    this.rowIndex, this.x, this.y, this.width, this.height,
                    mouseX, mouseY);
        }
    }

    private @Nullable Object resolveDrivenValue(StoredPropertyDefinition<?> property) {
        Object value = this.propertyManager.resolve(property.name()).orElse(null);
        if (value != null) {
            return value;
        }
        Codec<?> codec = property.anyType().value().codec();
        if (codec == Codec.INT) return 0;
        if (codec == Codec.FLOAT) return 0f;
        if (codec == Codec.DOUBLE) return 0.0;
        return null;
    }

    private static String formatPropertyValue(@Nullable Object value) {
        if (value == null) {
            return PROPERTY_UNSET_PLACEHOLDER;
        }
        return PropertyValues.display(value);
    }
}
