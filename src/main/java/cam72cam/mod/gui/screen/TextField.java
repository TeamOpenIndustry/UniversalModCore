package cam72cam.mod.gui.screen;

import cam72cam.mod.entity.Player;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.text.StringTextComponent;

import java.util.function.Predicate;

/** Base text field */
public class TextField extends Button {
    /** Standard constructor */
    public TextField(IScreenBuilder builder, int x, int y, int width, int height) {
        super(
                builder,
                new TextFieldWidget(Minecraft.getInstance().fontRenderer, builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, width, height, new StringTextComponent(""))
        );
    }

    TextFieldWidget internal() {
        return (TextFieldWidget) button;
    }

    /** Validator that can block a string from being entered */
    public void setValidator(Predicate<String> filter) {
        internal().setValidator(filter::test);
    }

    /** Move cursor to this text field */
    public void setFocused(boolean b) {
        internal().setFocused2(b);
    }

    /** Current text */
    public String getText() {
        return internal().getText();
    }

    /** Overwrite current text */
    public void setText(String s) {
        internal().setText(s);
    }

    /** Change visibility */
    public void setVisible(Boolean visible) {
        internal().setVisible(visible);
        internal().setEnabled(visible);
    }

    @Override
    public void onClick(Player.Hand hand) {

    }
}
