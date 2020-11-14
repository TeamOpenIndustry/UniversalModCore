package cam72cam.mod.gui.container;

import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.item.ItemStackHandler;

/** Provides a way to spec out a container piece by piece that functions both server and client side for slot synchronization */
public interface IContainerBuilder {
    /** Draw the top bar of a container window */
    int drawTopBar(int x, int y, int slots);

    /** Draw the bottom of a container window (above the player inv) */
    int drawBottomBar(int x, int y, int slots);

    /** Draw the connector between the container and the player inv */
    int drawPlayerInventoryConnector(int x, int y, int horizSlots);

    /** Draw the top bar of the player inv */
    int drawPlayerTopBar(int x, int y);

    /** Draw the bottom bar of the player inv */
    int drawPlayerInventory(int currY, int horizSlots);

    /** Draw a connector for when the player inv width == container width (TODO internal only?) */
    int drawPlayerMidBar(int x, int y);

    /** Draw a transparent version of this stack at these coords */
    void drawSlotOverlay(ItemStack stack, int x, int y);

    /** Draw this sprite at these coords */
    void drawSlotOverlay(String spriteId, int x, int y, double height, int color);

    /** Draw a single slot at these coords */
    void drawSlot(ItemStackHandler handler, int slotID, int x, int y);

    /** Draw an entire row of slots at these coords */
    int drawSlotRow(ItemStackHandler handler, int startSlotID, int slotColumns, int x, int y);

    /** Draw an entire block of slots at these coords (startId -> inv end) */
    int drawSlotBlock(ItemStackHandler handler, int startSlotId, int slotsColumns, int x, int y);

    /** Draw a tank at coords the size of cols * 16 x rows * 16 */
    void drawTankBlock(int x, int y, int slotColumns, int slotRows, Fluid fluid, float percentFull);

    /** Draw a centered and shadowed string at coords */
    void drawCenteredString(String quantityStr, int x, int y);
}
