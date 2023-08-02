package cam72cam.mod.gui.screen;

import cam72cam.mod.entity.Player;
import net.minecraftforge.fml.client.config.GuiSlider;

/** Standard slider */
public abstract class Slider extends Button {

    public Slider(IScreenBuilder builder, int x, int y, String text, double min, double max, double start, boolean doublePrecision) {
        super(builder, new GuiSlider(builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, text, min, max, start, b -> {}, null));
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
