package cam72cam.umc.api.gui.screen;

import cam72cam.umc.api.entity.Player;
import net.minecraft.client.gui.GuiButton;

import java.util.List;
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

    protected List<String> tooltips;

    /** Default width/height */
    public Button(IScreenBuilder builder, int x, int y, String text, BiConsumer<Player.Hand, Button> handler) {
            this(builder, x, y, 200, 20, text, handler);
        }

    @Deprecated
    public Button(IScreenBuilder builder, int x, int y, String text) {
            this(builder, x, y, 200, 20, text, (hand, button1) -> {});
    }

    /** Custom width/height */
    public Button(IScreenBuilder builder, int x, int y, int width, int height, String text, BiConsumer<Player.Hand, Button> handler) {
        this(builder,
             new GuiButton(-1, builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, width, height, text),
             handler);
    }

    @Deprecated
    public Button(IScreenBuilder builder, int x, int y, int width, int height, String text) {
        this(builder,
             new GuiButton(-1, builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, width, height, text),
             (hand, button1) -> {});
    }

    /** Internal ctr */
    protected Button(IScreenBuilder builder, GuiButton button, BiConsumer<Player.Hand, Button> handler) {
        this.button = button;
        builder.addButton(this);
        this.handler = handler;
    }

    @Override
    public void setText(String text) {
        button.displayString = text;
    }

    @Override
    public String getText() {
        return button.displayString;
    }

    @Override
    public void setVisible(boolean b) {
        button.visible = b;
    }

    @Override
    public boolean isVisible() {
        return button.visible;
    }

    @Override
    public void setEnabled(boolean b) {
        button.enabled = b;
    }

    public void setTooltip(List<String> content) {
        this.tooltips = content;
    }

    @Override
    public boolean isEnabled() {
        return button.enabled;
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

    public boolean isHovering() {
        return button.isMouseOver();
    }
}
