package cam72cam.mod.gui.screen;

import cam72cam.mod.entity.Player;
import net.minecraftforge.fml.client.config.GuiSlider;

import java.util.function.Consumer;

/** Standard slider */
public class Slider extends Button {
    public Slider(IScreenBuilder builder, int x, int y, String text, double min, double max, double start, boolean doublePrecision, Consumer<Slider> handler) {
        this(builder, x, y, 150, 20, text, min, max, start, doublePrecision, handler);
    }

    public Slider(IScreenBuilder builder, int x, int y, int width, int height, String text, double min, double max, double start, boolean doublePrecision, Consumer<Slider> handler) {
        super(builder,
              new GuiSlider(-1, builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, width, height,
                            text, "", min, max, start, doublePrecision, true, null),
              ((hand, button1) -> handler.accept((Slider) button1)));
        ((GuiSlider) this.button).parent = slider -> Slider.this.onSlider();
    }


    @Deprecated
    public Slider(IScreenBuilder builder, int x, int y, String text, double min, double max, double start, boolean doublePrecision) {
        super(builder,
              new GuiSlider(-1, builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, text, min, max, start, null),
              ((hand, button1) -> {}));
        ((GuiSlider) this.button).showDecimal = doublePrecision;
        ((GuiSlider) this.button).parent = slider -> Slider.this.onSlider();
    }

    /** Called when the slider value is changed */
    public void onSlider() {
        this.handler.accept(Player.Hand.PRIMARY, this);
    }

    public void setValue(double value) {
        ((GuiSlider)button).setValue(value);
    }

    public int getValueInt() {
        return ((GuiSlider) button).getValueInt();
    }

    public double getValue() {
        return ((GuiSlider) button).getValue();
    }
}
