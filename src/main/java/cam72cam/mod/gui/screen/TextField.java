package cam72cam.mod.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiTextField;

import java.util.function.Predicate;

/** Base text field */
public class TextField {
    protected final GuiTextField textfield;
    protected Predicate<String> validator;

    /** Standard constructor */
    public TextField(IScreenBuilder builder, int x, int y, int width, int height) {
        validator = str -> true;
        textfield = new GuiTextField(Minecraft.getMinecraft().fontRendererObj, builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, width, height) {
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

    /** Validator that can block a string from being entered */
    public void setValidator(Predicate<String> filter) {
        this.validator = filter;
    }

    /** Move cursor to this text field */
    public void setFocused(boolean b) {
        textfield.setFocused(b);
    }

    /** Current text */
    public String getText() {
        return textfield.getText();
    }

    /** Overwrite current text */
    public void setText(String s) {
        textfield.setText(s);
    }

    /** Change visibility */
    public void setVisible(Boolean visible) {
        textfield.setVisible(visible);
        textfield.setEnabled(visible);
    }
}
