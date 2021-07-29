package cam72cam.mod.gui.screen;


import cam72cam.mod.entity.Player;
import net.minecraft.network.chat.TextComponent;

/** Basic checkbox */
public abstract class CheckBox extends Button {
    public CheckBox(IScreenBuilder builder, int x, int y, String text, boolean enabled) {
        super(builder, x-25, y, 200, 20, (enabled ? "X" : "█") + " " + text);
    }

    public boolean isChecked() {
        return button.getMessage().getString().contains("X");
    }

    @Override
    protected void onClickInternal(Player.Hand hand) {
        this.setChecked(!this.isChecked());
        super.onClickInternal(hand);
    }

    public void setChecked(boolean val) {
        if (val) {
            button.setMessage(new TextComponent(button.getMessage().getString().replace("█", "X")));
        } else {
            button.setMessage(new TextComponent(button.getMessage().getString().replace("X", "█")));
        }
    }
}
