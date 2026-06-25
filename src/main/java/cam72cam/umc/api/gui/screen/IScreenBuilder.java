package cam72cam.umc.api.gui.screen;

import cam72cam.umc.api.fluid.Fluid;
import cam72cam.umc.api.resource.Identifier;
import cam72cam.umc.api.gui.helpers.GUIHelpers;

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
     * @see GUIHelpers#texturedRect(Identifier, int, int, int, int)
     */
    void drawImage(Identifier tex, int x, int y, int width, int height);

    /**
     * Add a tank to the GUI
     * @see GUIHelpers#drawTankBlock(int, int, int, int, Fluid, float, boolean, int)
     */
    void drawTank(int x, int y, int width, int height, Fluid fluid, float fluidPercent, boolean background, int color);

    /** @see GUIHelpers#drawCenteredString(String, int, int, int)  */
    void drawCenteredString(String str, int x, int y, int color);

    /** Show this GUI */
    void show();

    /** Add a text field to this GUI */
    void addTextField(TextField textField);
}
