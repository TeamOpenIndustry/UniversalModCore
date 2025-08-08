package cam72cam.mod.gui.screen;


import cam72cam.mod.entity.Player;
import net.minecraft.client.gui.widget.button.CheckboxButton;
import net.minecraft.util.text.TranslationTextComponent;

/** Basic checkbox */
public abstract class CheckBox extends Button {
    public CheckBox(IScreenBuilder builder, int x, int y, String text, boolean enabled) {
//        super(builder, x, y, 200, 20, (enabled ? "X" : "█") + " " + text);
        super(builder, new CheckboxButton(builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, 100, 20,
                                          new TranslationTextComponent(text), enabled));
    }

    public boolean isChecked() {
        return ((CheckboxButton)button).selected();
    }

    @Override
    protected void onClickInternal(Player.Hand hand) {
        this.setChecked();
        super.onClickInternal(hand);
    }

    public void setChecked() {
        ((CheckboxButton)button).onPress();
    }
}