package cam72cam.mod.gui.container;

import cam72cam.mod.render.opengl.RenderState;

/**
 * Defines a container which is synchronized both client and server side
 *
 * @see cam72cam.mod.gui.GuiRegistry for more details
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
