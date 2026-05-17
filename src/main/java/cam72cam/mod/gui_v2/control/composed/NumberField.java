package cam72cam.mod.gui_v2.control.composed;

import cam72cam.mod.gui_v2.control.ComposedWidget;
import cam72cam.mod.gui_v2.control.widget.Button;
import cam72cam.mod.gui_v2.control.widget.Slider;
import cam72cam.mod.gui_v2.control.widget.TextField;
import cam72cam.mod.text.PlayerMessage;

import java.util.function.Consumer;
import java.util.regex.Pattern;

public class NumberField extends ComposedWidget<NumberField> {
    private static final Pattern DECIMAL = Pattern.compile("-?\\d*\\.?\\d*");
    private static final Pattern INTEGER = Pattern.compile("-?\\d+");

    private final TextField textField;
    private final Slider slider;
    private final Button button;

    private final Consumer<Double> callback;
    private final boolean allowDecimal;
    private final String formatter;

    private double value;
    private boolean showSlider;

    public NumberField(int width, int height, PlayerMessage name, double min, double max, double orig, boolean allowDecimal, Consumer<Double> callback) {
        super(width, height);
        this.value = Math.max(min, Math.min(max, orig));
        this.button = Button.vanilla(height, height, PlayerMessage.direct("↺"), (hand, btn) -> onButtonChange());
        this.slider = Slider.horizontal(width - height, height, name, min, max, value, allowDecimal ? 4 : 0, this::onSliderChange);
        this.textField = new TextField(width - height, height, this::onTextChange) {
            @Override
            public void onFocusLost() {
                super.onFocusLost();
                String str = getText();
                double d = str.isEmpty() ? slider.getMinBound() : Double.parseDouble(str);
                setText(String.format(formatter, Math.max(slider.getMinBound(), Math.min(slider.getMaxBound(), d))));
            }
        };
        this.textField.setValidator(this::verify);
        this.allowDecimal = allowDecimal;
        this.formatter = allowDecimal ? "%.4f" : "%.0f";
        this.callback = callback;

        this.addChildren(this.textField, 0, 0);
        this.addChildren(this.slider, 0, 0);
        this.addChildren(this.button, width - height, 0);
        //Wrapped true for triggering refreshment
        this.showSlider = false;
        this.onButtonChange();
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        if (this.textField != null) {
            //Initialized, safe to use
            int h = this.height();
            this.slider.setWidth(width - h);
            this.textField.setWidth(width - h);
            this.button.setWidth(h);
            this.setChildRelativeX(this.button, this.slider.width());
            requestLayout();
        }
    }

    @Override
    public void setHeight(int height) {
        super.setHeight(height);
        if (this.textField != null) {
            //Initialized, safe to use
            int w = this.width();
            this.slider.setWidth(w - height);
            this.textField.setWidth(w - height);
            this.button.setWidth(height);
            this.setChildRelativeX(this.button, this.slider.width());
            this.slider.setHeight(height);
            this.textField.setHeight(height);
            this.button.setHeight(height);
            requestLayout();
        }
    }

    protected void onButtonChange() {
        this.showSlider = !this.showSlider;
        if (this.showSlider) {
            this.textField.setVisible(false);
            this.slider.setValue(this.value);
            this.slider.setVisible(true);
        } else {
            this.slider.setVisible(false);
            this.textField.setText(String.format(formatter, this.value));
            this.textField.setVisible(true);
        }
    }

    protected void onSliderChange(Slider slider) {
        this.value = slider.getValue();
        if (!allowDecimal) {
            this.value = Math.round(this.value);
        }
        callback.accept(this.value);
    }

    protected void onTextChange(String newText) {
        if (newText.isEmpty()) {
            this.value = 0;
            return;
        }
        this.value = Double.parseDouble(newText);
        callback.accept(this.value);
    }

    protected boolean verify(String text) {
        if (text.isEmpty()) {
            return true;
        }
        return allowDecimal ? DECIMAL.matcher(text).matches() : INTEGER.matcher(text).matches();
    }
}
