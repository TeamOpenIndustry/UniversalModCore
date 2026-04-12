package cam72cam.mod.gui.screen;

import cam72cam.mod.entity.Player;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.text.StringTextComponent;

import java.util.function.Predicate;

/** Base text field */
public class TextField implements IWidget {
    protected final TextFieldWidget internal;
    /** Standard constructor */
    public TextField(IScreenBuilder builder, int x, int y, int width, int height) {
        this(builder,
              new TextFieldWidget(Minecraft.getInstance().font, builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, width, height,
                                  new StringTextComponent("")));
    }

    /** Internal, can be overridden to support custom GuiTextFields */
    protected TextField(IScreenBuilder builder, TextFieldWidget internal) {
        this.internal = internal;
        this.internal.setMaxLength(256);
        builder.addTextField(this);
    }

    TextFieldWidget internal() {
        return internal;
    }

    @Override
    public void setText(String s) {
        internal.setValue(s);
    }

    @Override
    public String getText() {
        return internal.getValue();
    }

    @Override
    public void setVisible(boolean visible) {
        internal.setVisible(visible);
        internal.setEditable(visible);
    }

    @Deprecated
    public void setVisible(Boolean visible) {
        this.setVisible(visible.booleanValue());
    }

    @Override
    public boolean isVisible() {
        return internal.isVisible();
    }

    @Override
    public void setEnabled(boolean enabled) {
        internal.setEditable(enabled);
    }

    @Override
    public boolean isEnabled() {
        return internal.active;
    }

    /** Validator that can block a string from being entered */
    public void setValidator(Predicate<String> filter) {
        internal().setFilter(filter);
    }

    /** Move cursor to this text field */
    public void setFocused(boolean b) {
        internal().setFocus(b);
    }
}
