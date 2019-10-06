package cam72cam.mod.gui;

import cam72cam.mod.util.Hand;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.AbstractButtonWidget;

import java.util.function.Consumer;

public abstract class Button {
    final AbstractButtonWidget button;

    public Button(IScreenBuilder builder, int x, int y, String text) {
        this(builder, x, y, 200, 20, text);
    }

    private static class ButtonWidget extends AbstractButtonWidget {

        private Consumer<Hand> clicker = hand -> {};

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
                        clicker.accept(int_1 == 0 ? Hand.PRIMARY : Hand.SECONDARY);
                        return true;
                    }
                }

                return false;
            } else {
                return false;
            }
        }
    }

    public Button(IScreenBuilder builder, int x, int y, int width, int height, String text) {
        this(builder, new ButtonWidget(builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, width, height, text));
        ((ButtonWidget)button).clicker = this::onClick;
    }

    Button(IScreenBuilder builder, AbstractButtonWidget button) {
        this.button = button;
        builder.addButton(this);
    }

    public void setText(String text) {
        button.setMessage(text);
    }

    public abstract void onClick(Hand hand);

    AbstractButtonWidget internal() {
        return button;
    }

    public void onUpdate() {

    }

    public void setTextColor(int i) {
        //TODO button.packedFGColour = i;
    }

    public void setVisible(boolean b) {
        button.visible = b;
    }
}
