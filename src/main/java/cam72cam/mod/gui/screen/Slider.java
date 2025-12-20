package cam72cam.mod.gui.screen;

import cam72cam.mod.entity.Player;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.widget.ForgeSlider;
import java.util.function.Supplier;

/** Standard slider */
public abstract class Slider extends Button {
    /** Internal wrapper to add onSlider Hook */
    private static class InternalForgeSlider extends ForgeSlider {
        private Runnable clicker = () -> {};
        private Supplier<String> setter = () -> "";

        public InternalForgeSlider(int x, int y, int width, int height, Component prefix, Component suffix, double minValue, double maxValue, double currentValue, double stepSize, int precision, boolean drawString) {
            super(x, y, width, height, prefix, suffix, minValue, maxValue, currentValue, stepSize, precision, drawString);
        }

        @Override
        protected void applyValue() {
            super.applyValue();
            clicker.run();
        }

        @Override
        protected void updateMessage() {
            if (setter != null && setter.get() != null && !setter.get().isEmpty()) {
                this.setMessage(Component.literal(setter.get()));
            } else {
                super.updateMessage();
            }
        }
    }

    private String text;

    public Slider(IScreenBuilder builder, int x, int y, String text, double min, double max, double start, boolean doublePrecision) {
//        super(builder, new net.minecraftforge.client.gui.widget.Slider(builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, new TextComponent(text), min, max, start, b -> {}, null));
        super(builder, new InternalForgeSlider(builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, 150, 20,
                                               Component.literal(text), Component.literal(""), min, max, start, 0, doublePrecision ? 4 : 0, true));
        ((InternalForgeSlider)this.button).clicker = this::onSlider;
        ((InternalForgeSlider)this.button).setter = this::getSliderText;
    }

    @Override
    public void onClick(Player.Hand hand) {

    }

    /** Called when the slider value is changed */
    public abstract void onSlider();

    public int getValueInt() {
        return ((ForgeSlider) button).getValueInt();
    }

    public double getValue() {
        return ((ForgeSlider) button).getValue();
    }

    @Override
    public void setText(String text) {
        this.text = text;
        ((InternalForgeSlider)this.button).updateMessage();
    }

    private String getSliderText() {
        return this.text;
    }
}
