package cam72cam.mod.gui.container;

import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.item.ItemStackHandler;
import cam72cam.mod.render.OpenGL;
import cam72cam.mod.resource.Identifier;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;

import java.util.function.Supplier;

import static cam72cam.mod.gui.helpers.GUIHelpers.CHEST_GUI_TEXTURE;

/** GUI Container wrapper for the client side, Do not use directly */
public class ClientContainerBuilder extends GuiContainer implements IContainerBuilder {
    private static final int slotSize = 18;
    private static final int topOffset = 17;
    private static final int bottomOffset = 7;
    private static final int textureHeight = 222;
    private static final int paddingRight = 7;
    private static final int paddingLeft = 7;
    private static final int stdUiHorizSlots = 9;
    private static final int playerXSize = paddingRight + stdUiHorizSlots * slotSize + paddingLeft;
    private static final int midBarOffset = 4;
    private static final int midBarHeight = 4;
    private final ServerContainerBuilder server;
    private final Supplier<Boolean> valid;
    private int centerX;
    private int centerY;

    public ClientContainerBuilder(ServerContainerBuilder serverContainer, Supplier<Boolean> valid) {
        super(serverContainer);
        this.server = serverContainer;
        this.xSize = paddingRight + serverContainer.slotsX * slotSize + paddingLeft;
        this.ySize = server.ySize;
        this.valid = valid;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        OpenGL.RenderContext ctx = new OpenGL.RenderContext();
        ctx.color(1, 1, 1, 1);
        try (OpenGL.RenderContext color = ctx.apply()) {
            //this.mc.getTextureManager().bindTexture(CHEST_GUI_TEXTURE);
            this.centerX = (this.width - this.xSize) / 2;
            this.centerY = (this.height - this.ySize) / 2;
            server.draw.accept(this);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (!valid.get()) {
            this.mc.displayGuiScreen(null);
            if (this.mc.currentScreen == null) {
                this.mc.setIngameFocus();
            }
        }
    }

    /* IContainerBuilder */

    @Override
    public int drawTopBar(int x, int y, int slots) {
        OpenGL.RenderContext ctx = new OpenGL.RenderContext();
        ctx.color(1,1,1,1);
        ctx.texture(CHEST_GUI_TEXTURE);
        try (OpenGL.RenderContext color = ctx.apply()) {
            super.drawTexturedModalRect(centerX + x, centerY + y, 0, 0, paddingLeft, topOffset);
            // Top Bar
            for (int k = 1; k <= slots; k++) {
                super.drawTexturedModalRect(centerX + x + paddingLeft + (k - 1) * slotSize, centerY + y, paddingLeft, 0, slotSize, topOffset);
            }
            // Top Right Corner
            super.drawTexturedModalRect(centerX + x + paddingLeft + slots * slotSize, centerY + y, paddingLeft + stdUiHorizSlots * slotSize, 0, paddingRight, topOffset);
        }
        return y + topOffset;
    }

    @Override
    public void drawSlot(ItemStackHandler handler, int slotID, int x, int y) {
        OpenGL.RenderContext ctx = new OpenGL.RenderContext();
        ctx.color(1,1,1,1);
        ctx.texture(CHEST_GUI_TEXTURE);
        try (OpenGL.RenderContext color = ctx.apply()) {
            x += paddingLeft;
            if (handler != null && handler.getSlotCount() > slotID) {
                super.drawTexturedModalRect(centerX + x, centerY + y, paddingLeft, topOffset, slotSize, slotSize);
            } else {
                Gui.drawRect(centerX + x, centerY + y, centerX + x + slotSize, centerY + y + slotSize, 0xFF444444);
            }
        }
    }

    @Override
    public int drawSlotRow(ItemStackHandler handler, int start, int cols, int x, int y) {
        OpenGL.RenderContext ctx = new OpenGL.RenderContext();
        ctx.color(1,1,1,1);
        ctx.texture(CHEST_GUI_TEXTURE);
        try (OpenGL.RenderContext color = ctx.apply()) {
            // Left Side
            super.drawTexturedModalRect(centerX + x, centerY + y, 0, topOffset, paddingLeft, slotSize);
            // Middle Slots
            for (int slotID = start; slotID < start + cols; slotID++) {
                int slotOff = (slotID - start);
                drawSlot(handler, slotID, x + slotOff * slotSize, y);
            }
        }
        try (OpenGL.RenderContext color = ctx.apply()) {
            // Right Side
            super.drawTexturedModalRect(centerX + x + paddingLeft + cols * slotSize, centerY + y, paddingLeft + stdUiHorizSlots * slotSize, topOffset, paddingRight, slotSize);
        }
        return y + slotSize;
    }

    @Override
    public int drawSlotBlock(ItemStackHandler handler, int start, int cols, int x, int y) {
        if (cols < server.slotsX) {
            x += (server.slotsX - cols) * slotSize / 2;
        }

        for (int slotID = start; slotID < handler.getSlotCount(); slotID += cols) {
            y = drawSlotRow(handler, slotID, cols, x, y);
        }
        return y;
    }

    @Override
    public int drawBottomBar(int x, int y, int slots) {
        OpenGL.RenderContext ctx = new OpenGL.RenderContext();
        ctx.color(1,1,1,1);
        ctx.texture(CHEST_GUI_TEXTURE);
        try (OpenGL.RenderContext color = ctx.apply()) {
            // Left Bottom
            super.drawTexturedModalRect(centerX + x, centerY + y, 0, textureHeight - bottomOffset, paddingLeft, bottomOffset);
            // Middle Bottom
            for (int k = 1; k <= slots; k++) {
                super.drawTexturedModalRect(centerX + x + paddingLeft + (k - 1) * slotSize, centerY + y, paddingLeft, textureHeight - bottomOffset, slotSize, bottomOffset);
            }
            // Right Bottom
            super.drawTexturedModalRect(centerX + x + paddingLeft + slots * slotSize, centerY + y, paddingLeft + 9 * slotSize, textureHeight - bottomOffset, paddingRight, bottomOffset);
        }
        return y + bottomOffset;
    }

    @Override
    public int drawPlayerTopBar(int x, int y) {
        OpenGL.RenderContext ctx = new OpenGL.RenderContext();
        ctx.color(1,1,1,1);
        ctx.texture(CHEST_GUI_TEXTURE);
        try (OpenGL.RenderContext color = ctx.apply()) {
            super.drawTexturedModalRect(centerX + x, centerY + y, 0, 0, playerXSize, bottomOffset);
        }
        return y + bottomOffset;
    }

    @Override
    public int drawPlayerMidBar(int x, int y) {
        OpenGL.RenderContext ctx = new OpenGL.RenderContext();
        ctx.color(1,1,1,1);
        ctx.texture(CHEST_GUI_TEXTURE);
        try (OpenGL.RenderContext color = ctx.apply()) {
            super.drawTexturedModalRect(centerX + x, centerY + y, 0, midBarOffset, playerXSize, midBarHeight);
        }
        return y + midBarHeight;
    }

    @Override
    public int drawPlayerInventory(int y, int horizSlots) {
        int normInvOffset = (horizSlots - stdUiHorizSlots) * slotSize / 2 + paddingLeft - 7;
        OpenGL.RenderContext ctx = new OpenGL.RenderContext();
        ctx.color(1,1,1,1);
        ctx.texture(CHEST_GUI_TEXTURE);
        try (OpenGL.RenderContext color = ctx.apply()) {
            super.drawTexturedModalRect(centerX + normInvOffset, centerY + y, 0, 126 + 4, playerXSize, 96);
        }
        return y + 96;
    }

    @Override
    public int drawPlayerInventoryConnector(int x, int y, int horizSlots) {
        int normInvOffset = (horizSlots - stdUiHorizSlots) * slotSize / 2 + paddingLeft - 7;
        if (horizSlots < server.slotsX) {
            x += (server.slotsX - horizSlots) * slotSize / 2;
        }

        if (horizSlots > 9) {
            return drawBottomBar(x, y, horizSlots);
        } else if (horizSlots < 9) {
            return drawPlayerTopBar(x + normInvOffset, y);
        } else {
            return drawPlayerMidBar(x + normInvOffset, y);
        }
    }

    @Override
    public void drawTankBlock(int x, int y, int horizSlots, int inventoryRows, Fluid fluid, float percentFull) {
        x += paddingLeft + centerX;
        y += centerY;

        int width = horizSlots * slotSize;
        int height = inventoryRows * slotSize;

        GUIHelpers.drawTankBlock(x, y, width, height, fluid, percentFull);
    }

    @Override
    public void drawCenteredString(String text, int x, int y) {
        super.drawCenteredString(this.fontRenderer, text, x + centerX + this.xSize / 2, y + centerY, 14737632);
    }

    @Override
    public void drawSlotOverlay(ItemStack stack, int x, int y) {
        x += centerX + 1 + paddingLeft;
        y += centerY + 1;

        this.mc.getRenderItem().renderItemIntoGUI(stack.internal, x, y);

        OpenGL.RenderContext ctx = new OpenGL.RenderContext()
                .color(1, 1, 1, 1)
                .alpha_test(true)
                .depth_test(false);
        try (OpenGL.RenderContext rc = ctx.apply()) {
            Gui.drawRect(x, y, x + 16, y + 16, -2130706433);
        }
    }

    @Override
    public void drawSlotOverlay(String spriteId, int x, int y, double height, int color) {
        x += centerX + 1 + paddingLeft;
        y += centerY + 1;

        OpenGL.RenderContext ctx = new OpenGL.RenderContext();
        ctx.color(1,1,1,1);
        try (OpenGL.RenderContext c = ctx.apply()) {
            drawRect(x, y + (int) (16 - 16 * height), x + 16, y + 16, color);
            // Reset the state manager color
            GlStateManager.color(1,1,1,1);
        }

        TextureAtlasSprite sprite = mc.getTextureMapBlocks().getAtlasSprite(spriteId);
        ctx.texture(new Identifier(TextureMap.LOCATION_BLOCKS_TEXTURE));
        try (OpenGL.RenderContext c = ctx.apply()) {
            super.drawTexturedModalRect(x, y, sprite, 16, 16);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }
}
