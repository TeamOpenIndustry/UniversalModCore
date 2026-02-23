package cam72cam.mod.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiTextField;

import java.util.function.Predicate;

/** Base text field */
public class TextField implements IWidget {
    protected final GuiTextField textfield;
    protected Predicate<String> validator;

    /** Standard constructor */
    public TextField(IScreenBuilder builder, int x, int y, int width, int height) {
        //Have to do here as we can't call anything before constructor
        validator = str -> true;
        textfield = new GuiTextField(Minecraft.getMinecraft().fontRendererObj, builder.getWidth() / 2 + x + 1, builder.getHeight() / 4 + y + 1, width - 2, height - 2) {
            @Override
            public void setText(String text) {
                if (validator.test(text)) {
                    super.setText(text);
                }
            }

            @Override
            public void writeText(String p_writeText_1_) {
                String orig = this.getText();
                super.writeText(p_writeText_1_);
                if (!validator.test(getText())) {
                    setText(orig);
                }
            }
        };
        builder.addTextField(this);
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
        return textfield.isEnabled;
    }

    /** Validator that can block a string from being entered */
    public void setValidator(Predicate<String> filter) {
        this.validator = filter;
    }

    /** Move cursor to this text field */
    public void setFocused(boolean b) {
        textfield.setFocused(b);
    }
}
