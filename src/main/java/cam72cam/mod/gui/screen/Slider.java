package cam72cam.mod.gui.screen;

import cam72cam.mod.entity.Player;
import net.minecraft.util.text.StringTextComponent;

/** Standard slider */
public abstract class Slider extends Button {

    public Slider(IScreenBuilder builder, int x, int y, String text, double min, double max, double start, boolean doublePrecision) {
        super(builder, new net.minecraftforge.fml.client.gui.widget.Slider(builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, new StringTextComponent(text), min, max, start, null, null));
        ((net.minecraftforge.fml.client.gui.widget.Slider) this.button).showDecimal = doublePrecision;
        ((net.minecraftforge.fml.client.gui.widget.Slider) this.button).parent = slider -> Slider.this.onSlider();
    }

    @Override
    public void onClick(Player.Hand hand) {

    }

    /** Called when the slider value is changed */
    public abstract void onSlider();

    public int getValueInt() {
        return ((net.minecraftforge.fml.client.gui.widget.Slider) button).getValueInt();
    }

    public double getValue() {
        return ((net.minecraftforge.fml.client.gui.widget.Slider) button).getValue();
    }
}
