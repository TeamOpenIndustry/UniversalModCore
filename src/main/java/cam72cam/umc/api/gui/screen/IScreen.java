package cam72cam.umc.api.gui.screen;

import cam72cam.umc.api.entity.Player;
import cam72cam.umc.api.input.Keyboard;
import cam72cam.umc.api.render.opengl.RenderState;

import javax.annotation.Nullable;

public interface IScreen {
    /** Called when screen is initially constructed */
    void init(IScreenBuilder screen);

    @Deprecated
    default void onEnterKey(IScreenBuilder builder) { }
    /**
     * Called when any key is pressed outside textfield
     * @param keyCode The typed key's code, or null if not recognizable
     * */
    default void onKeyType(IScreenBuilder builder, @Nullable Keyboard.KeyCode keyCode){
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
