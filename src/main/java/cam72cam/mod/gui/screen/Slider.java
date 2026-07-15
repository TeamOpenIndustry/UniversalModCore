package cam72cam.mod.gui.screen;

import cam72cam.mod.entity.Player;
import net.minecraft.util.text.StringTextComponent;

import java.util.function.Consumer;

/** Standard slider */
public class Slider extends Button {
    public Slider(IScreenBuilder builder, int x, int y, String text, double min, double max, double start, boolean doublePrecision, Consumer<Slider> handler) {
        this(builder, x, y, 150, 20, text, min, max, start, doublePrecision, handler);
    }

    public Slider(IScreenBuilder builder, int x, int y, int width, int height, String text, double min, double max, double start, boolean doublePrecision, Consumer<Slider> handler) {
        super(builder,
              new net.minecraftforge.fml.client.gui.widget.Slider(builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, width, height,
                            new StringTextComponent(text), new StringTextComponent(""), min, max, start, doublePrecision, true, null),
              ((hand, button1) -> handler.accept((Slider) button1)));
        ((net.minecraftforge.fml.client.gui.widget.Slider) this.button).showDecimal = doublePrecision;
        ((net.minecraftforge.fml.client.gui.widget.Slider) this.button).parent = slider -> Slider.this.onSlider();
    }


    @Deprecated
    public Slider(IScreenBuilder builder, int x, int y, String text, double min, double max, double start, boolean doublePrecision) {
        this(builder, x, y, 150, 20, text, min, max, start, doublePrecision, slid -> {});
    }

    /** Called when the slider value is changed */
    public void onSlider() {
        this.handler.accept(Player.Hand.PRIMARY, this);
    }

    public void setValue(double value) {
        ((net.minecraftforge.fml.client.gui.widget.Slider)button).setValue(value);
    }

    public int getValueInt() {
        return ((net.minecraftforge.fml.client.gui.widget.Slider) button).getValueInt();
    }

    public double getValue() {
        return ((net.minecraftforge.fml.client.gui.widget.Slider) button).getValue();
    }
}
