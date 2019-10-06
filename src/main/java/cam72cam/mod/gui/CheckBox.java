package cam72cam.mod.gui;

import net.minecraft.client.gui.widget.ToggleButtonWidget;

public abstract class CheckBox extends Button {
    public CheckBox(IScreenBuilder builder, int x, int y, String text, boolean enabled) {
        super(builder, new ToggleButtonWidget(builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, 200, 20, enabled));
        button.setMessage(text);
    }

    public boolean isChecked() {
        return ((ToggleButtonWidget) this.button).isToggled();
    }

    public void setChecked(boolean val) {
        ((ToggleButtonWidget) this.button).setToggled(val);
    }
}
