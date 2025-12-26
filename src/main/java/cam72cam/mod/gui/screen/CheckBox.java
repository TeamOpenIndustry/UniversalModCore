package cam72cam.mod.gui.screen;

import cam72cam.mod.entity.Player;

import net.minecraft.client.gui.widget.button.CheckboxButton;
import net.minecraft.util.text.StringTextComponent;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Basic checkbox */
public class CheckBox extends Button {
    /** Internal onPress wrapper as Forge doesn't have corresponding hook */
    private static class InternalCB extends CheckboxButton {
        private Consumer<Player.Hand> clicker = hand -> {};

        public InternalCB(int xIn, int yIn, int widthIn, int heightIn, String msg, boolean enabled) {
            super(xIn, yIn, widthIn, heightIn, new StringTextComponent(msg), enabled);
        }

        @Override
        public void onPress() {
            super.onPress();
            clicker.accept(Player.Hand.PRIMARY);
        }
    }

    public CheckBox(IScreenBuilder builder, int x, int y, String text, boolean enabled, BiConsumer<Player.Hand, CheckBox> handler) {
        super(builder,
              new InternalCB(builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, 100, 20, text, enabled),
              ((hand, button1) -> handler.accept(hand, (CheckBox) button1)));
    }

    @Deprecated
    public CheckBox(IScreenBuilder builder, int x, int y, String text, boolean enabled) {
//        super(builder, x, y, 200, 20, (enabled ? "X" : "█") + " " + text);
        super(builder,
              new InternalCB(builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, 100, 20, text, enabled),
              ((hand, button1) -> {}));
        ((InternalCB)this.button).clicker = this::onClickInternal;
    }


    public boolean isChecked() {
        return ((CheckboxButton)button).selected();
    }

    @Override
    protected void onClickInternal(Player.Hand hand) {
        super.onClickInternal(hand);
    }

    public void setChecked(boolean val) {
        ((CheckboxButton) button).selected = val;
    }
}
