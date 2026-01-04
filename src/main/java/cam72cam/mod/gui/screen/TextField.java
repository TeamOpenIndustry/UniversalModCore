package cam72cam.mod.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.TextComponent;

import java.util.function.Predicate;

/** Base text field */
public class TextField implements IWidget{
    protected final EditBox textfield;
    /** Standard constructor */
    public TextField(IScreenBuilder builder, int x, int y, int width, int height) {
        this(builder,
              new EditBox(Minecraft.getInstance().font, builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, width, height,
                          new TextComponent("")));
    }

    /** Internal, can be overridden to support custom GuiTextFields */
    protected TextField(IScreenBuilder builder, EditBox internal) {
        this.textfield = internal;
        builder.addTextField(this);
    }

    EditBox internal() {
        return textfield;
    }

    @Override
    public void setText(String s) {
        textfield.setValue(s);
    }

    @Override
    public String getText() {
        return textfield.getValue();
    }

    @Override
    public void setVisible(boolean visible) {
        textfield.setVisible(visible);
        textfield.setEditable(visible);
    }

    @Deprecated
    public void setVisible(Boolean visible) {
        this.setVisible(visible.booleanValue());
    }

    @Override
    public boolean isVisible() {
        return textfield.isVisible();
    }

    @Override
    public void setEnabled(boolean enabled) {
        textfield.setEditable(enabled);
    }

    @Override
    public boolean isEnabled() {
        return textfield.active;
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
