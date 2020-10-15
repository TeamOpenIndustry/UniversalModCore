package cam72cam.mod.gui.screen;

import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.resource.Identifier;

public interface IScreenBuilder {
    /** Close this screen */
    void close();

    /** Add a button to this screen */
    void addButton(Button btn);

    /** Current width */
    int getWidth();

    /** Current height */
    int getHeight();

    /**
     * Add an image to the GUI
     * @see cam72cam.mod.gui.helpers.GUIHelpers#texturedRect(Identifier, int, int, int, int)
     */
    void drawImage(Identifier tex, int x, int y, int width, int height);

    /**
     * Add a tank to the GUI
     * @see cam72cam.mod.gui.helpers.GUIHelpers#drawTankBlock(int, int, int, int, Fluid, float, boolean, int)
     */
    void drawTank(int x, int y, int width, int height, Fluid fluid, float fluidPercent, boolean background, int color);

    /** @see cam72cam.mod.gui.helpers.GUIHelpers#drawCenteredString(String, int, int, int)  */
    void drawCenteredString(String str, int x, int y, int color);

    /** Show this GUI */
    void show();

    /** Add a text field to this GUI */
    void addTextField(TextField textField);
}
