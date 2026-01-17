package cam72cam.mod.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.TextFieldWidget;

import java.util.function.Predicate;

/** Base text field */
public class TextField implements IWidget{
    protected final TextFieldWidget textfield;
    /** Standard constructor */
    public TextField(IScreenBuilder builder, int x, int y, int width, int height) {
        this(builder,
              new TextFieldWidget(Minecraft.getInstance().fontRenderer, builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, width, height, ""));
    }

    /** Internal, can be overridden to support custom GuiTextFields */
    protected TextField(IScreenBuilder builder, TextFieldWidget internal) {
        this.textfield = internal;
        builder.addTextField(this);
    }

    TextFieldWidget internal() {
        return textfield;
    }

    @Override
    public void setText(String s) {
        textfield.setText(s);
    }

    @Override
    public String getText() {
        return textfield.getText();
    }

    @Override
    public void setVisible(boolean visible) {
        textfield.setVisible(visible);
        textfield.setEnabled(visible);
    }

    @Deprecated
    public void setVisible(Boolean visible) {
        this.setVisible(visible.booleanValue());
    }

    @Override
    public boolean isVisible() {
        return textfield.getVisible();
    }

    @Override
    public void setEnabled(boolean enabled) {
        textfield.setEnabled(enabled);
    }

    @Override
    public boolean isEnabled() {
        return textfield.active;
    }

    /** Validator that can block a string from being entered */
    public void setValidator(Predicate<String> filter) {
        internal().setValidator(filter::test);
    }

    /** Move cursor to this text field */
    public void setFocused(boolean b) {
        internal().setFocused2(b);
    }
}
