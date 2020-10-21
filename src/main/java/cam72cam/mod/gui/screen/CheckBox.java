package cam72cam.mod.gui.screen;

import net.minecraftforge.fml.client.config.GuiCheckBox;

/** Basic checkbox */
public abstract class CheckBox extends Button {
    public CheckBox(IScreenBuilder builder, int x, int y, String text, boolean enabled) {
        super(builder, new GuiCheckBox(-1, builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, text, enabled));
    }

    public boolean isChecked() {
        return ((GuiCheckBox) this.button).isChecked();
    }

    public void setChecked(boolean val) {
        ((GuiCheckBox) this.button).setIsChecked(val);
    }
}
