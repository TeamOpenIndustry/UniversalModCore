package cam72cam.mod.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.TextFieldWidget;

import java.util.function.Predicate;

/** Base text field */
public class TextField implements IWidget {
    protected final TextFieldWidget internal;
    /** Standard constructor */
    public TextField(IScreenBuilder builder, int x, int y, int width, int height) {
        this(builder,
              //Offset x&y by 1 and minus width&height by 2 to let border be within the range specified
              new TextFieldWidget(Minecraft.getInstance().fontRenderer, builder.getWidth() / 2 + x + 1, builder.getHeight() / 4 + y + 1, width - 2, height - 2, ""));
    }

    /** Internal, can be overridden to support custom GuiTextFields */
    protected TextField(IScreenBuilder builder, TextFieldWidget internal) {
        this.internal = internal;
        this.internal.setMaxStringLength(256);
        builder.addTextField(this);
    }

    TextFieldWidget internal() {
        return internal;
    }

    @Override
    public void setText(String s) {
        internal.setText(s);
    }

    @Override
    public String getText() {
        return internal.getText();
    }

    @Override
    public void setVisible(boolean visible) {
        internal.setVisible(visible);
        internal.setEnabled(visible);
    }

    @Deprecated
    public void setVisible(Boolean visible) {
        this.setVisible(visible.booleanValue());
    }

    @Override
    public boolean isVisible() {
        return internal.getVisible();
    }

    @Override
    public void setEnabled(boolean enabled) {
        internal.setEnabled(enabled);
    }

    @Override
    public boolean isEnabled() {
        return internal.active;
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
