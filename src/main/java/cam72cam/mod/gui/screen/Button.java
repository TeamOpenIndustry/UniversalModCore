package cam72cam.mod.gui.screen;

import cam72cam.mod.entity.Player;
import net.minecraft.client.gui.GuiButton;

import java.util.function.BiConsumer;

/** Base interactable GUI element */
public class Button implements IWidget{
    /**
     * Handler consumer, called upon clicked
     * Hand -> PRIMARY is a left-click, SECONDARY is a right-click
     * Button -> Reference of self, as it may not be fully constructed
     */
    protected BiConsumer<Player.Hand, Button> handler;

    /** Internal MC obj */
    protected final GuiButton button;

    /** Default width/height */
    public Button(IScreenBuilder builder, int x, int y, String text, BiConsumer<Player.Hand, Button> handler) {
        this(builder, x, y, 200, 20, text, handler);
    }

    /** Custom width/height */
    public Button(IScreenBuilder builder, int x, int y, int width, int height, String text, BiConsumer<Player.Hand, Button> handler) {
        this(builder,
             new GuiButton(-1, builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, width, height, text),
             handler);
    }

    /** Internal ctr */
    protected Button(IScreenBuilder builder, GuiButton button, BiConsumer<Player.Hand, Button> handler) {
        this.button = button;
        builder.addButton(this);
        this.handler = handler;
    }

    @Override
    public String getText() {
        return button.displayString;
    }

    @Override
    public void setText(String text) {
        button.displayString = text;
    }

    @Override
    public void setVisible(boolean b) {
        button.visible = b;
    }

    @Override
    public void setEnabled(boolean b) {
        button.enabled = b;
    }

    /** Internal click handler*/
    public void onClick(Player.Hand hand) {
        this.handler.accept(hand, this);
    }

    /** Called every screen draw */
    public void onUpdate() {

    }

    /** Override the text color */
    public void setTextColor(int i) {
        button.packedFGColour = i;
    }
}
