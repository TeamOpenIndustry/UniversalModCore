package cam72cam.mod.gui.screen;

import cam72cam.mod.entity.Player;
import net.minecraft.client.gui.GuiButton;

/** Base interactable GUI element */
public abstract class Button {
    /** Internal MC obj */
    protected final GuiButton button;

    /** Default width/height */
    public Button(IScreenBuilder builder, int x, int y, String text) {
        this(builder, x, y, 200, 20, text);
    }

    /** Custom width/height */
    public Button(IScreenBuilder builder, int x, int y, int width, int height, String text) {
        this(builder, new GuiButton(-1, builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, width, height, text));
    }

    /** Internal ctr */
    protected Button(IScreenBuilder builder, GuiButton button) {
        this.button = button;
        builder.addButton(this);
    }

    /** Currently displayed text */
    public String getText() {
        return button.displayString;
    }

    /** Override current text */
    public void setText(String text) {
        button.displayString = text;
    }

    /** Click handler that must be implemented */
    public abstract void onClick(Player.Hand hand);

    /** Called every screen draw */
    public void onUpdate() {

    }

    /** Override the text color */
    public void setTextColor(int i) {
        button.packedFGColour = i;
    }

    /** Set the button visible or not */
    public void setVisible(boolean b) {
        button.visible = b;
    }

    /** enable or disable the button */
    public void setEnabled(boolean b) {
        button.enabled = b;
    }

    ;
}
