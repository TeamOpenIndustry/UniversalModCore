package cam72cam.mod.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.AbstractButton;

import cam72cam.mod.entity.Player;

import java.util.function.Consumer;

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

    protected final Widget button;

    /** Internal MC obj */
    private static class InternalButton extends AbstractButton {
        private Consumer<Player.Hand> clicker = hand -> {};

        public InternalButton(int xIn, int yIn, int widthIn, int heightIn, String msg) {
            super(xIn, yIn, widthIn, heightIn, msg);
        }

        @Override
        protected boolean isValidClickButton(int p_isValidClickButton_1_) {
            return p_isValidClickButton_1_ == 1 || p_isValidClickButton_1_ == 0;
        }

        @Override
        public boolean mouseClicked(double p_mouseClicked_1_, double p_mouseClicked_3_, int p_mouseClicked_5_) {
            if (this.active && this.visible) {
                if (this.isValidClickButton(p_mouseClicked_5_)) {
                    boolean flag = this.clicked(p_mouseClicked_1_, p_mouseClicked_3_);
                    if (flag) {
                        this.playDownSound(Minecraft.getInstance().getSoundHandler());
                        clicker.accept(p_mouseClicked_5_ == 0 ? Player.Hand.PRIMARY : Player.Hand.SECONDARY);
                        return true;
                    }
                }

                return false;
            } else {
                return false;
            }
        }

        @Override
        public void onPress() {
            clicker.accept(Player.Hand.PRIMARY);
        }
    }

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
             new InternalButton(builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, width, height, text),
             handler);
    }

    @Deprecated
    public Button(IScreenBuilder builder, int x, int y, int width, int height, String text) {
        this(builder,
             new InternalButton(builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, width, height, text),
             (hand, button1) -> {});
    }

    /** Internal ctr */
    Button(IScreenBuilder builder, Widget button, BiConsumer<Player.Hand, Button> handler) {
        this.button = button;
        builder.addButton(this);
        this.handler = handler;
        if (this.button instanceof InternalButton) {
            ((InternalButton) this.button).clicker = this::onClickInternal;
        }
    }

    @Override
    public void setText(String text) {
        button.setMessage(text);
    }

    protected void onClickInternal(Player.Hand hand) {
        onClick(hand);
    }

    Widget internal() {
        return button;
    }

    @Override
    public String getText() {
        return button.getMessage();
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
        button.active = b;
    }

    public void setTooltip(List<String> content) {
        this.tooltips = content;
    }

    @Override
    public boolean isEnabled() {
        return button.active;
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
        button.setFGColor(i);
    }

    public boolean isHovering() {
//        return button.isMouseOver(Minecraft.getInstance().mouseHelper.getMouseX(), Minecraft.getInstance().mouseHelper.getMouseY());
        //Re-wrap here as we want inactive button also get processed
        Minecraft mc = Minecraft.getInstance();
        double mouseX = mc.mouseHelper.getMouseX() * mc.getMainWindow().getScaledWidth() / mc.getMainWindow().getWidth();
        double mouseY = mc.mouseHelper.getMouseY() * mc.getMainWindow().getScaledHeight() / mc.getMainWindow().getHeight();
        return isVisible() && mouseX >= button.x && mouseX <= button.x + button.getWidth()
                           && mouseY >= button.y && mouseY <= button.y + button.getHeight();
    }
}
