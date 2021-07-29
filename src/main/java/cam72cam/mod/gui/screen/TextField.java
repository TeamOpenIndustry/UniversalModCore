package cam72cam.mod.gui.screen;

import cam72cam.mod.entity.Player;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.TextComponent;

import java.util.function.Predicate;

/** Base text field */
public class TextField extends Button {
    /** Standard constructor */
    public TextField(IScreenBuilder builder, int x, int y, int width, int height) {
        super(
                builder,
                new EditBox(Minecraft.getInstance().font, builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, width, height, new TextComponent(""))
        );
    }

    EditBox internal() {
        return (EditBox) button;
    }

    /** Validator that can block a string from being entered */
    public void setValidator(Predicate<String> filter) {
        internal().setFilter(filter::test);
    }

    /** Move cursor to this text field */
    public void setFocused(boolean b) {
        internal().setFocus(b);
    }

    /** Current text */
    public String getText() {
        return internal().getValue();
    }

    /** Overwrite current text */
    public void setText(String s) {
        internal().setValue(s);
    }

    /** Change visibility */
    public void setVisible(Boolean visible) {
        internal().setVisible(visible);
        internal().setEditable(visible);
    }

    @Override
    public void onClick(Player.Hand hand) {

    }
}
