package cam72cam.umc.api.gui.container;

import cam72cam.umc.api.gui.GuiRegistry;
import cam72cam.umc.api.render.opengl.RenderState;

/**
 * Defines a container which is synchronized both client and server side
 *
 * @see GuiRegistry for more details
 */
public interface IContainer {
    @Deprecated
    default void draw(IContainerBuilder builder) { }

    /** Called once server side to layout the GUI and every tick client side to actually draw the screen + slots */
    default void draw(IContainerBuilder builder, RenderState state) {
        draw(builder);
    }

    /** Width of this container in slots */
    int getSlotsX();

    /** Height of this container in slots */
    int getSlotsY();
}
