package cam72cam.mod.gui;

import cam72cam.mod.util.Hand;
import net.minecraft.client.gui.widget.SliderWidget;

public abstract class Slider extends Button {

    private final double min;
    private final double max;
    private final boolean doublePrecision;

    public static class HookedSliderWidget extends SliderWidget {
        public Slider parent;

        protected HookedSliderWidget(int int_1, int int_2, int int_3, int int_4, double double_1) {
            super(int_1, int_2, int_3, int_4, double_1);
        }

        @Override
        protected void updateMessage() {
            super.setMessage("" + parent.getValue());
        }

        @Override
        protected void applyValue() {
            parent.onSlider();
        }

        public double getValue() {
            return value;
        }
    }


    public Slider(IScreenBuilder builder, int x, int y, String text, double min, double max, double start, boolean doublePrecision) {
        super(builder, new HookedSliderWidget(builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, 150, 20, (start - min) / (max - min)));
        this.min = min;
        this.max = max;
        this.doublePrecision = doublePrecision;
        ((HookedSliderWidget) this.button).parent = this;
    }

    @Override
    public void onClick(Hand hand) {

    }

    public abstract void onSlider();

    public int getValueInt() {
        return (int)Math.round(getValue());
    }

    public double getValue() {
        return ((HookedSliderWidget) button).getValue() * (max - min) + min;
    }
}
