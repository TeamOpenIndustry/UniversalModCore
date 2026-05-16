package cam72cam.mod.gui_v2.core.actions;

import cam72cam.mod.input.Keyboard;

public interface IKeyboardListener {
    boolean onKeyPressed(Keyboard.KeyCode key);
    boolean onCharTyped(char ch);
}
