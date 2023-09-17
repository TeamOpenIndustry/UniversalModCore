package cam72cam.mod.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.AbstractButton;

import cam72cam.mod.entity.Player;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** Base interactable GUI element */
public abstract class Button {
    protected final AbstractWidget button;

    /** Internal MC obj */
    private static class InternalButton extends AbstractButton {
        private Consumer<Player.Hand> clicker = hand -> {};

        public InternalButton(int xIn, int yIn, int widthIn, int heightIn, String msg) {
            super(xIn, yIn, widthIn, heightIn, Component.literal(msg));
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
                        this.playDownSound(Minecraft.getInstance().getSoundManager());
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

        @Override
        protected void updateWidgetNarration(NarrationElementOutput p_259858_) {
            this.defaultButtonNarrationText(p_259858_);
        }
    }

    /** Default width/height */
    public Button(IScreenBuilder builder, int x, int y, String text) {
        this(builder, x, y, 200, 20, text);
    }

    /** Custom width/height */
    public Button(IScreenBuilder builder, int x, int y, int width, int height, String text) {
        this(builder, new InternalButton(builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, width, height, text));
        ((InternalButton)this.button).clicker = this::onClickInternal;
    }

    /** Internal ctr */
    Button(IScreenBuilder builder, AbstractWidget button) {
        this.button = button;
        builder.addButton(this);
    }

    /** Currently displayed text */
    public String getText() {
        return button.getMessage().getString();
    }

    /** Override current text */
    public void setText(String text) {
        button.setMessage(Component.literal(text));
    }

    protected void onClickInternal(Player.Hand hand) {
        onClick(hand);
    }

    AbstractWidget internal() {
        return button;
    }

    /** Click handler that must be implemented */
    public abstract void onClick(Player.Hand hand);

    /** Called every screen draw */
    public void onUpdate() {

    }

    /** Override the text color */
    public void setTextColor(int i) {
        button.setFGColor(i);
    }

    /** Set the button visible or not */
    public void setVisible(boolean b) {
        button.visible = b;
    }

    /** enable or disable the button */
    public void setEnabled(boolean b) {
        button.active = b;
    }

    ;
}
