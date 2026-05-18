package cam72cam.mod.gui_v2.control.composed;

import cam72cam.mod.entity.Player;
import cam72cam.mod.gui_v2.control.ComposedWidget;
import cam72cam.mod.gui_v2.control.widget.Button;
import cam72cam.mod.text.PlayerMessage;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Cycles among the given range of string-able items.
 * @param <T> The type to be cycled.
 */
public class CyclableButton<T> extends ComposedWidget<CyclableButton<T>> {
    private final Button button;

    private final T[] options;
    private int index;
    private T selected;

    private final PlayerMessage original;
    private final Consumer<T> callback;

    /**
     * Create a button based on given template button's appearance and size
     */
    public CyclableButton(Button template, List<T> options, T sel, Consumer<T> callback) {
        super(template.width(), template.height());
        this.options = (T[]) options.toArray(new Object[]{});
        this.index = options.indexOf(sel);
        if (this.index == -1) {
            throw new IllegalArgumentException("Undefined default selected value: " + sel);
        }
        this.selected = sel;

        this.original = template.getName();
        this.button = new Button(template.width(), template.height(), formatName(original), this::onCLick);
        this.button.copyFacade(template);
        this.callback = callback;
        this.addChildren(button, 0, 0);
    }

    public static <E extends Enum<E>> CyclableButton<E> ofEnum(Button template, Class<E> classE, E sel, Consumer<E> callback) {
        List<E> options = Arrays.asList(classE.getEnumConstants());
        return new CyclableButton<E>(template, options, sel, callback);
    }

    private void onCLick(Player.Hand hand, Button btn) {
        int offset = hand == Player.Hand.PRIMARY ? 1 : -1;
        this.index += offset;
        if (this.index >= this.options.length) {
            this.index = 0;
        }
        if (this.index < 0) {
            this.index = this.options.length - 1;
        }
        this.selected = this.options[this.index];
        this.setName(this.formatName(original));
        if (this.callback != null) {
            this.callback.accept(this.selected);
        }
    }

    /**
     * Replace "sel" in button's name for new name
     */
    private PlayerMessage formatName(PlayerMessage message) {
        return PlayerMessage.direct(message.internal.getFormattedText().replace("sel", selected.toString()));
    }
}
