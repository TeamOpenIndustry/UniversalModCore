package cam72cam.mod.gui.screen;


import cam72cam.mod.entity.Player;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.network.chat.TextComponent;

import java.util.function.Consumer;

/** Basic checkbox */
public abstract class CheckBox extends Button {
    /** Internal onPress wrapper as Forge doesn't have corresponding hook */
    private static class InternalCB extends Checkbox {
        private Consumer<Player.Hand> clicker = hand -> {};

        public InternalCB(int xIn, int yIn, int widthIn, int heightIn, String msg, boolean enabled) {
            super(xIn, yIn, widthIn, heightIn, new TextComponent(msg), enabled);
        }

        @Override
        public void onPress() {
            super.onPress();
            clicker.accept(Player.Hand.PRIMARY);
        }
    }

    public CheckBox(IScreenBuilder builder, int x, int y, String text, boolean enabled) {
        super(builder, new InternalCB(builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, 100, 20, text, enabled));
        ((InternalCB)this.button).clicker = this::onClickInternal;
    }

    public boolean isChecked() {
        return ((Checkbox)button).selected();
    }

    @Override
    protected void onClickInternal(Player.Hand hand) {
        super.onClickInternal(hand);
    }

    public void setChecked() {
        ((Checkbox)button).onPress();
    }
}
