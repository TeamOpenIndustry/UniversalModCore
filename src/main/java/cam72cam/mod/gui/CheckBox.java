package cam72cam.mod.gui;

import net.minecraft.client.gui.widget.ToggleButtonWidget;
import net.minecraft.util.Identifier;

public abstract class CheckBox extends Button {
    protected static final Identifier TEXTURE = new Identifier("textures/gui/recipe_book.png");
    public CheckBox(IScreenBuilder builder, int x, int y, String text, boolean enabled) {
        super(builder, new ToggleButtonWidget(builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, 200, 20, enabled));
        ((ToggleButtonWidget)button).setTextureUV(152, 182, 28, 18, TEXTURE);
        button.setMessage(text);
    }

    public boolean isChecked() {
        return ((ToggleButtonWidget) this.button).isToggled();
    }

    public void setChecked(boolean val) {
        ((ToggleButtonWidget) this.button).setToggled(val);
    }
}
