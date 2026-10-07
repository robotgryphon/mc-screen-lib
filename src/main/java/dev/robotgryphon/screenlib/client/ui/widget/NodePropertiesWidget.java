package dev.robotgryphon.screenlib.client.ui.widget;

import dev.robotgryphon.screenlib.graph.schema.node.ComputedPropertyDefinition;
import dev.robotgryphon.screenlib.graph.Connection;
import com.mojang.serialization.Codec;
import dev.robotgryphon.screenlib.client.NodeRenderer;
import dev.robotgryphon.screenlib.client.ui.widget.property.BooleanPropertyEditor;
import dev.robotgryphon.screenlib.client.ui.widget.property.DropdownEditor;
import dev.robotgryphon.screenlib.client.ui.widget.property.DropdownPopup;
import dev.robotgryphon.screenlib.client.ui.widget.property.NumericPropertyEditor;
import dev.robotgryphon.screenlib.client.ui.widget.property.PropertyEditor;
import dev.robotgryphon.screenlib.client.ui.widget.property.PropertyValues;
import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.schema.node.StoredPropertyDefinition;
import dev.robotgryphon.screenlib.graph.nodes.PropertyManager;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.layouts.EqualSpacingLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import org.joml.Vector2dc;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class NodePropertiesWidget implements Renderable {
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

    public NodePropertiesWidget(NodeWidget owner, PropertyManager propertyManager) {
        this.owner = owner;
        this.node = owner.node();
        this.propertyManager = propertyManager;

        if (rowCount(this.propertyManager) == 0) {
            this.propertiesLayout = null;
            this.propertyRows = List.of();
        } else {
            int n = rowCount(this.propertyManager);
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

    public static List<ComputedPropertyDefinition> visibleComputed(PropertyManager propertyManager) {
        return propertyManager.computedProperties().stream()
                .filter(definition -> !definition.hideLabel())
                .toList();
    }

    public static int rowCount(PropertyManager propertyManager) {
        return propertyManager.stored().size() + visibleComputed(propertyManager).size();
    }

    public int width() {
        return this.owner.getWidth();
    }

    public int height() {
        return NodeRenderer.propertyRegionHeight(rowCount(this.propertyManager));
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
            if (this.findInputConnection(prop.name()) != null) continue;

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
        this.propertyManager.setFocusedName(null);
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
        String name = this.propertyManager.focusedName();
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
                    this.propertyManager.setFocusedName(null);
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

        Connection inputConn = this.findInputConnection(prop.name());
        boolean driven = inputConn != null;
        Object upstreamValue = driven ? this.resolveUpstreamValue(inputConn) : null;
        boolean undriven = driven && upstreamValue == null;

        int labelColor = undriven
                ? PROPERTY_LABEL_UNDRIVEN_COLOR
                : (driven ? PROPERTY_LABEL_DRIVEN_COLOR : PROPERTY_LABEL_COLOR);
        int valueColor = driven ? PROPERTY_VALUE_DRIVEN_COLOR : PROPERTY_VALUE_COLOR;

        float alpha = this.owner.effectiveAlpha(prop.anyType());

        int textY = rowTop + (rowHeight - font.lineHeight) / 2 + 1;

        Component label = NodeRenderer.propertyLabel(this.node, prop.name());
        graphics.text(font, label,
                rowLeft + NodeRenderer.PROPERTY_PADDING_X,
                textY,
                ARGB.multiply(labelColor, ARGB.white(alpha)),
                false);

        PropertyRow row = this.propertyRows.get(rowIndex);
        PropertyEditor editor = row.editor();

        if (!driven && editor != null) {
            row.syncEditorValueFromNode();
            editor.setAlpha(alpha);
            editor.extractRenderState(graphics, mouseX, mouseY, 0f);
        } else {
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
        var definition = visibleComputed(this.propertyManager)
                .get(rowIndex - this.propertyManager.stored().size());
        float alpha = this.owner.effectiveAlpha(definition.type());
        int textY = rowTop + (rowHeight - font.lineHeight) / 2 + 1;

        graphics.text(font, NodeRenderer.propertyLabel(this.node, definition.name()),
                rowLeft + NodeRenderer.PROPERTY_PADDING_X, textY,
                ARGB.multiply(PROPERTY_LABEL_DRIVEN_COLOR, ARGB.white(alpha)), false);

        String valueText = formatPropertyValue(this.propertyManager.computedValue(definition.name()));
        graphics.text(font, Component.literal(valueText),
                rowLeft + rowWidth - NodeRenderer.PROPERTY_PADDING_X - font.width(valueText), textY,
                ARGB.multiply(PROPERTY_VALUE_DRIVEN_COLOR, ARGB.white(alpha)), false);
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
            this.width = NodePropertiesWidget.this.owner.getWidth();
            this.height = NodeRenderer.PROPERTY_PITCH;

            this.editor = buildEditor(rowIndex);
        }

        private @Nullable PropertyEditor buildEditor(int rowIndex) {
            if (rowIndex >= NodePropertiesWidget.this.propertyManager.stored().size()) {
                return null;
            }
            StoredPropertyDefinition<?> prop = NodePropertiesWidget.this.propertyManager.stored().get(rowIndex);
            Codec<?> codec = prop.type().value().codec();
            final String propName = prop.name();
            int editorW = NodePropertiesWidget.this.owner.valueWidth(rowIndex);
            int editorH = NodeRenderer.PROPERTY_PITCH;
            if (isNumericCodec(codec)) {
                Number seed = NodePropertiesWidget.this.currentNumericValue(prop);
                return new NumericPropertyEditor(0, 0, editorW, editorH,
                        seed, codec,
                        next -> NodePropertiesWidget.this.propertyManager.set(propName, next));
            }
            if (isDropdownProperty(prop)) {
                Object current = NodePropertiesWidget.this.propertyManager.value(propName);
                return new DropdownEditor(0, 0, editorW, editorH, current,
                        () -> NodePropertiesWidget.this.propertyManager.setFocusedName(propName));
            }
            if (codec == Codec.BOOL) {
                Object current = NodePropertiesWidget.this.propertyManager.value(propName);
                boolean seed = current instanceof Boolean b && b;
                return new BooleanPropertyEditor(0, 0, editorW, editorH, seed,
                        next -> NodePropertiesWidget.this.propertyManager.set(propName, next));
            }
            return null;
        }

        @Nullable PropertyEditor editor() {
            return this.editor;
        }

        void syncEditorValueFromNode() {
            if (this.editor == null) return;
            StoredPropertyDefinition<?> prop = NodePropertiesWidget.this.propertyManager.stored().get(this.rowIndex);
            Object current = NodePropertiesWidget.this.propertyManager.value(prop.name());
            if (this.editor instanceof NumericPropertyEditor n) {
                n.setValue(NodePropertiesWidget.this.currentNumericValue(prop));
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
            NodePropertiesWidget.this.renderPropertyRow(graphics, font,
                    this.rowIndex, this.x, this.y, this.width, this.height,
                    mouseX, mouseY);
        }
    }

    private @Nullable Connection findInputConnection(String propertyName) {
        return this.node.portManager().inboundForProperty(propertyName);
    }

    private @Nullable Object resolveUpstreamValue(Connection connection) {
        Object value = this.owner.canvas().resolveUpstreamValue(connection);
        if (value != null) {
            return value;
        }
        Codec<?> codec = connection.sourcePort().type().value().codec();
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
