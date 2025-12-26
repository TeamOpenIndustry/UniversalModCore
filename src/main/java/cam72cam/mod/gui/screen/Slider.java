package cam72cam.mod.gui.screen;

import cam72cam.mod.entity.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.client.gui.widget.ForgeSlider;

import java.util.function.Supplier;

import java.util.function.Consumer;

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
                this.setMessage(new TextComponent(setter.get()));
            } else {
                super.updateMessage();
            }
        }
    }

    private String text;
    public Slider(IScreenBuilder builder, int x, int y, String text, double min, double max, double start, boolean doublePrecision, Consumer<Slider> handler) {
        this(builder, x, y, 150, 20, text, min, max, start, doublePrecision, handler);
    }

    public Slider(IScreenBuilder builder, int x, int y, int width, int height, String text, double min, double max, double start, boolean doublePrecision, Consumer<Slider> handler) {
        super(builder,
              new InternalForgeSlider(builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, width, height,
                                                                 new TextComponent(text), new TextComponent(""), min, max, start, doublePrecision, true, null),
              ((hand, button1) -> handler.accept((Slider) button1)));
        ((InternalForgeSlider)this.button).clicker = this::onSlider;
        ((InternalForgeSlider)this.button).setter = this::getSliderText;
    }


    @Deprecated
    public Slider(IScreenBuilder builder, int x, int y, String text, double min, double max, double start, boolean doublePrecision) {
        super(builder,
              new InternalForgeSlider(builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, 150, 20, new TextComponent(text), new TextComponent(""), min, max, start, 0, doublePrecision ? 4 : 0, true),
              ((hand, button1) -> {}));
        ((InternalForgeSlider)this.button).clicker = this::onSlider;
        ((InternalForgeSlider)this.button).setter = this::getSliderText;
    }

    /** Called when the slider value is changed */
    public void onSlider() {
        this.handler.accept(Player.Hand.PRIMARY, this);
    }

    public void setValue(double value) {
        ((InternalForgeSlider)button).setValue(value);
    }

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
