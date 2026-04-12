package cam72cam.mod.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.TextComponent;

import java.util.function.Predicate;

/** Base text field */
public class TextField implements IWidget{
    protected final EditBox internal;
    /** Standard constructor */
    public TextField(IScreenBuilder builder, int x, int y, int width, int height) {
        this(builder,
              new EditBox(Minecraft.getInstance().font, builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, width, height,
                          new TextComponent("")));
    }

    /** Internal, can be overridden to support custom GuiTextFields */
    protected TextField(IScreenBuilder builder, EditBox internal) {
        this.internal = internal;
        this.internal.setMaxLength(256);
        builder.addTextField(this);
    }

    EditBox internal() {
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
