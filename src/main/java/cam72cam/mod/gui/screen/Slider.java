package cam72cam.mod.gui.screen;

import cpw.mods.fml.client.config.GuiSlider;
import cam72cam.mod.entity.Player;

/** Standard slider */
public abstract class Slider extends Button {

    public Slider(IScreenBuilder builder, int x, int y, String text, double min, double max, double start, boolean doublePrecision) {
        super(builder, new GuiSlider(-1, builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, text, min, max, start, null));
        ((GuiSlider) this.button).showDecimal = doublePrecision;
        ((GuiSlider) this.button).parent = slider -> Slider.this.onSlider();
    }

    @Override
    public void onClick(Player.Hand hand) {

    }

    /** Called when the slider value is changed */
    public abstract void onSlider();

    public int getValueInt() {
        return ((GuiSlider) button).getValueInt();
    }

    public double getValue() {
        return ((GuiSlider) button).getValue();
    }
}
