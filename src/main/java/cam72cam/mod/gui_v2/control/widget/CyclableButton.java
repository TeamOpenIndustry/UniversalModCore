package cam72cam.mod.gui_v2.control.widget;

import cam72cam.mod.entity.Player;
import cam72cam.mod.text.PlayerMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Cycles among the given range of string-able items.
 * @param <T> The type to be cycled.
 */
public class CyclableButton<T> extends Button {
    private final List<T> values;
    private int currentIndex;
    private Consumer<T> onValueChanged;

    public CyclableButton(int width, int height, Iterable<T> values, T initial) {
        super(width, height, PlayerMessage.direct(initial.toString()));
        this.values = new ArrayList<>();
        values.forEach(this.values::add);
        this.currentIndex = this.values.indexOf(initial);
        if (this.currentIndex < 0) this.currentIndex = 0;
        this.vanilla();
    }

    public static <E extends Enum<E>> CyclableButton<E> of(Button template, Class<E> e, E initial) {
        E[] values = e.getEnumConstants();
        return of(template, Arrays.asList(values), initial);
    }

    public static <T> CyclableButton<T> of(Button template, Iterable<T> values, T initial) {
        CyclableButton<T> btn = new CyclableButton<>(template.width(), template.height(), values, initial);
        btn.copyFacade(template);
        btn.setName(PlayerMessage.direct(initial.toString()));
        return btn;
    }

    public CyclableButton<T> valueChanged(Consumer<T> onValueChanged) {
        this.onValueChanged = onValueChanged;
        return this;
    }

    public T getValue() {
        return values.get(currentIndex);
    }

    public void setValue(T value) {
        int idx = values.indexOf(value);
        if (idx >= 0 && idx != currentIndex) {
            currentIndex = idx;
            updateDisplay();
        }
    }

    @Override
    public boolean onClick(Player.Hand hand, int mouseX, int mouseY) {
        if (!isHovering()) return false;
        currentIndex = (currentIndex + 1) % values.size();
        updateDisplay();
        if (handler != null) {
            handler.accept(hand, this);
        }
        if (onValueChanged != null) {
            onValueChanged.accept(getValue());
        }
        return true;
    }

    private void updateDisplay() {
        setName(PlayerMessage.direct(getValue().toString()));
    }
}
