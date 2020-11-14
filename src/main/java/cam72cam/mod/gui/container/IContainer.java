package cam72cam.mod.gui.container;

/**
 * Defines a container which is synchronized both client and server side
 *
 * @see cam72cam.mod.gui.GuiRegistry for more details
 */
public interface IContainer {
    /** Called once server side to layout the GUI and every tick client side to actually draw the screen + slots */
    void draw(IContainerBuilder builder);

    /** Width of this container in slots */
    int getSlotsX();

    /** Height of this container in slots */
    int getSlotsY();
}
