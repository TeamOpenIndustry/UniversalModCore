package cam72cam.mod.gui.container;

import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.item.ItemStackHandler;
import invtweaks.api.container.ChestContainer;
import invtweaks.api.container.ContainerSection;
import invtweaks.api.container.ContainerSectionCallback;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.items.SlotItemHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Match ClientContainerBuilder spacing as closely as possible */
@ChestContainer
public class ServerContainerBuilder extends net.minecraft.inventory.Container implements IContainerBuilder {
    // server padding overrides
    private static final int slotSize = 18;
    private static final int topOffset = 18;
    private static final int bottomOffset = 7;
    private static final int paddingRight = 7;
    private static final int paddingLeft = 8;
    private static final int stdUiHorizSlots = 9;
    public static final int playerXSize = paddingLeft + stdUiHorizSlots * slotSize + paddingRight;
    private static final int midBarHeight = 4;
    final Consumer<IContainerBuilder> draw;
    final int slotsX;
    final int slotsY;
    int ySize = 0;
    private final IInventory playerInventory;
    Map<ContainerSection, List<Slot>> slotRefs = new HashMap<>();

    public ServerContainerBuilder(IInventory playerInventory, IContainer container) {
        this.playerInventory = playerInventory;

        slotRefs.put(ContainerSection.CHEST, new ArrayList<>());

        this.slotsX = container.getSlotsX();
        this.slotsY = container.getSlotsY();

        this.draw = container::draw;
        draw.accept(this);
    }

    /* Inv Tweaks */

    @ChestContainer.RowSizeCallback
    @Optional.Method(modid = "inventorytweaks")
    public int rowSize() {
        return this.slotsX;
    }

    @ContainerSectionCallback
    @Optional.Method(modid = "inventorytweaks")
    public Map<ContainerSection, List<Slot>> getContainerSections() {
        return slotRefs;
    }

    /* Overrides */


    @Override
    public int drawTopBar(int x, int y, int slots) {
        return y + topOffset;
    }

    @Override
    public void drawSlot(ItemStackHandler handler, int slotID, int x, int y) {
        x += paddingLeft;
        if (handler != null && handler.getSlotCount() > slotID) {
            this.addSlotToContainer(new SlotItemHandler(handler.internal, slotID, x, y));
            slotRefs.get(ContainerSection.CHEST).add(inventorySlots.get(inventorySlots.size() - 1));
        }
    }

    @Override
    public int drawSlotRow(ItemStackHandler handler, int start, int cols, int x, int y) {
        for (int slotID = start; slotID < start + cols; slotID++) {
            drawSlot(handler, slotID, x + (slotID - start) * slotSize, y);
        }
        this.ySize = Math.max(ySize, y + slotSize);
        return y + slotSize;
    }


    @Override
    public int drawSlotBlock(ItemStackHandler handler, int start, int cols, int x, int y) {
        if (cols < slotsX) {
            x += (slotsX - cols) * slotSize / 2;
        }

        for (int slotID = start; slotID < (handler != null ? handler.getSlotCount() : cols); slotID += cols) {
            y = drawSlotRow(handler, slotID, cols, x, y);
        }
        this.ySize = Math.max(ySize, y);
        return y;
    }

    @Override
    public int drawBottomBar(int x, int y, int slots) {
        this.ySize = Math.max(ySize, y + bottomOffset);
        return y + bottomOffset;
    }

    @Override
    public int drawPlayerTopBar(int x, int y) {
        this.ySize = Math.max(ySize, y + bottomOffset);
        return y + bottomOffset;
    }

    @Override
    public int drawPlayerMidBar(int x, int y) {
        this.ySize = Math.max(ySize, y + midBarHeight);
        return y + midBarHeight;
    }


    @Override
    public int drawPlayerInventoryConnector(int x, int y, int horizSlots) {
        int width = 0;
        if (horizSlots > 9) {
            return drawBottomBar(x, y, horizSlots);
        } else if (horizSlots < 9) {
            return drawPlayerTopBar((width - playerXSize) / 2, y);
        } else {
            return drawPlayerMidBar((width - playerXSize) / 2, y);
        }
    }

    @Override
    public void drawTankBlock(int x, int y, int horizSlots, int inventoryRows, Fluid fluid, float percentFull) {

    }

    @Override
    public void drawCenteredString(String quantityStr, int x, int y) {

    }

    @Override
    public void drawSlotOverlay(ItemStack stack, int i, int j) {

    }

    @Override
    public void drawSlotOverlay(String spriteId, int x, int y, double percent, int color) {

    }

    @Override
    public int drawPlayerInventory(int currY, int horizSlots) {
        currY += 9;

        int offset = inventorySlots.size();

        int normInvOffset = (horizSlots - stdUiHorizSlots) * slotSize / 2 + paddingLeft;

        for (int l = 0; l < 3; ++l) {
            for (int j1 = 0; j1 < stdUiHorizSlots; ++j1) {
                this.addSlotToContainer(new Slot(playerInventory, j1 + l * stdUiHorizSlots + stdUiHorizSlots, normInvOffset + j1 * slotSize, currY));
            }
            currY += slotSize;
        }
        currY += 4;

        for (int i1 = 0; i1 < 9; ++i1) {
            this.addSlotToContainer(new Slot(playerInventory, i1, normInvOffset + i1 * slotSize, currY));
        }
        currY += slotSize;

        slotRefs.put(ContainerSection.INVENTORY, inventorySlots.subList(offset + 0, offset + 36));
        slotRefs.put(ContainerSection.INVENTORY_NOT_HOTBAR, inventorySlots.subList(offset + 0, offset + 27));
        slotRefs.put(ContainerSection.INVENTORY_HOTBAR, inventorySlots.subList(offset + 27, offset + 36));

        this.ySize = Math.max(ySize, currY);
        return currY;
    }


    @Override
    public final boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }

    @Override
    public final net.minecraft.item.ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        net.minecraft.item.ItemStack itemstack = net.minecraft.item.ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);
        int numSlots = slotRefs.get(ContainerSection.CHEST).size();

        if (slot != null && slot.getHasStack()) {
            net.minecraft.item.ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            if (index < numSlots) {
                if (!this.mergeItemStack(itemstack1, numSlots, this.inventorySlots.size(), true)) {
                    return net.minecraft.item.ItemStack.EMPTY;
                }
            } else if (!this.mergeItemStack(itemstack1, 0, numSlots, false)) {
                return net.minecraft.item.ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.putStack(net.minecraft.item.ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
        }

        return itemstack;
    }
}
