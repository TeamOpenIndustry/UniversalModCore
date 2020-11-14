package cam72cam.mod.gui.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.AbstractButtonWidget;

import java.util.function.Consumer;
import cam72cam.mod.entity.Player;

/** Base interactable GUI element */
public abstract class Button {
    /** Internal MC obj */
    final AbstractButtonWidget button;

    private static class ButtonWidget extends AbstractButtonWidget {

        private Consumer<Player.Hand> clicker = hand -> {};

        public ButtonWidget(int int_1, int int_2, int int_3, int int_4, String string_1) {
            super(int_1, int_2, int_3, int_4, string_1);
        }

        @Override
        protected boolean isValidClickButton(int int_1) {
            return int_1 == 0 || int_1 == 1;
        }

        @Override
        public boolean mouseClicked(double double_1, double double_2, int int_1) {
            if (this.active && this.visible) {
                if (this.isValidClickButton(int_1)) {
                    boolean boolean_1 = this.clicked(double_1, double_2);
                    if (boolean_1) {
                        this.playDownSound(MinecraftClient.getInstance().getSoundManager());
                        clicker.accept(int_1 == 0 ? Player.Hand.PRIMARY : Player.Hand.SECONDARY);
                        return true;
                    }
                }

                return false;
            } else {
                return false;
            }
        }
    }

    /** Default width/height */
    public Button(IScreenBuilder builder, int x, int y, String text) {
        this(builder, x, y, 200, 20, text);
    }

    /** Custom width/height */
    public Button(IScreenBuilder builder, int x, int y, int width, int height, String text) {
        this(builder, new ButtonWidget(builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, width, height, text));
        ((ButtonWidget)button).clicker = this::onClickInternal;
    }

    /** Internal ctr */
    protected Button(IScreenBuilder builder, AbstractButtonWidget button) {
        this.button = button;
        builder.addButton(this);
    }

    /** Currently displayed text */
    public String getText() {
        return button.getMessage();
    }

    /** Override current text */
    public void setText(String text) {
        button.setMessage(text);
    }

    protected void onClickInternal(Player.Hand hand) {
        onClick(hand);
    }

    AbstractButtonWidget internal() {
        return button;
    }

    /** Click handler that must be implemented */
    public abstract void onClick(Player.Hand hand);

    /** Called every screen draw */
    public void onUpdate() {

    }

    /** Override the text color */
    public void setTextColor(int i) {
        //TODO button.packedFGColour = i;
    }

    /** Set the button visible or not */
    public void setVisible(boolean b) {
        button.visible = b;
    }

    /** enable or disable the button */
    public void setEnabled(boolean b) {
        button.active = b;
    }
}
