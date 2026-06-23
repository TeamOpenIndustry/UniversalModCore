package cam72cam.umc.api.gui.screen;

import cam72cam.umc.api.entity.Player;
import net.minecraftforge.fml.client.config.GuiCheckBox;

import java.util.function.BiConsumer;

/** Basic checkbox */
public class CheckBox extends Button {
    public CheckBox(IScreenBuilder builder, int x, int y, String text, boolean enabled, BiConsumer<Player.Hand, CheckBox> handler) {
        super(builder,
              new GuiCheckBox(-1, builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, text, enabled),
              ((hand, button1) -> handler.accept(hand, (CheckBox) button1)));
    }

    @Deprecated
    public CheckBox(IScreenBuilder builder, int x, int y, String text, boolean enabled) {
        super(builder,
              new GuiCheckBox(-1, builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, text, enabled),
              ((hand, button1) -> {}));
    }


    public boolean isChecked() {
        return ((GuiCheckBox) this.button).isChecked();
    }

    public void setChecked(boolean val) {
        ((GuiCheckBox) this.button).setIsChecked(val);
    }
}
