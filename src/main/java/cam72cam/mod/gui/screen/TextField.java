package cam72cam.mod.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiTextField;

import java.util.function.Predicate;

/** Base text field */
public class TextField implements IWidget{
    protected final GuiTextField textfield;

    /** Standard constructor */
    public TextField(IScreenBuilder builder, int x, int y, int width, int height) {
        this(builder,
             new GuiTextField(-1, Minecraft.getMinecraft().fontRenderer, builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, width, height));
    }

    /** Internal, can be overridden to support custom GuiTextFields */
    protected TextField(IScreenBuilder builder, GuiTextField internal) {
        this.textfield = internal;
        builder.addTextField(this);
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
        return textfield.isEnabled;
    }

    /** Validator that can block a string from being entered */
    public void setValidator(Predicate<String> filter) {
        textfield.setValidator(filter::test);
    }

    /** Move cursor to this text field */
    public void setFocused(boolean b) {
        textfield.setFocused(b);
    }
}
