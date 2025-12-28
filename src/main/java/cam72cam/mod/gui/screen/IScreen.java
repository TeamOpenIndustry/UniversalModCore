package cam72cam.mod.gui.screen;

import cam72cam.mod.entity.Player;
import cam72cam.mod.input.Keyboard;
import cam72cam.mod.render.opengl.RenderState;

public interface IScreen {
    /** Called when screen is initially constructed */
    void init(IScreenBuilder screen);

    @Deprecated
    default void onEnterKey(IScreenBuilder builder) { }
    /** Called when any key is pressed outside textfield */
    default void onKeyType(IScreenBuilder builder, Keyboard.KeyCode keyCode){
        if (keyCode == Keyboard.KeyCode.NUMPADENTER || keyCode == Keyboard.KeyCode.RETURN) {
            onEnterKey(builder);
        }
    }

    /**
     * Called when player click his mouse outside textfield
     * @param hand PRIMARY -> LMB, SECONDARY -> other buttons
     * */
    default void onMouseClick(int x, int y, Player.Hand hand){

    }

    /** Called during close */
    void onClose();

    @Deprecated
    default void draw(IScreenBuilder builder) { }
    /** Called once per screen draw */
    default void draw(IScreenBuilder builder, RenderState state) {
        draw(builder);
    }
}
