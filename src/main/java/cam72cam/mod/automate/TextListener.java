package cam72cam.mod.automate;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

@FunctionalInterface
public interface TextListener extends DocumentListener {
    void onTextChanged();

    @Override
    default void insertUpdate(DocumentEvent documentEvent) {
        onTextChanged();
    }

    @Override
    default void removeUpdate(DocumentEvent documentEvent) {
        onTextChanged();
    }

    @Override
    default void changedUpdate(DocumentEvent documentEvent) {
        onTextChanged();
    }
}
