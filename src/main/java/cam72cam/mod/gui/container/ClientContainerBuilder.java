package cam72cam.mod.gui.container;

import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.item.ItemStackHandler;
import com.mojang.blaze3d.platform.GlStateManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ingame.ContainerScreen;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.text.LiteralText;
import org.lwjgl.opengl.GL11;

import static cam72cam.mod.gui.helpers.GUIHelpers.CHEST_GUI_TEXTURE;

@Environment(EnvType.CLIENT)
public class ClientContainerBuilder extends ContainerScreen implements IContainerBuilder {
    public static final int slotSize = 18;
    public static final int topOffset = 17;
    public static final int bottomOffset = 7;
    public static final int textureHeight = 222;
    public static final int paddingRight = 7;
    public static final int paddingLeft = 7;
    public static final int stdUiHorizSlots = 9;
    public static final int playerXSize = paddingRight + stdUiHorizSlots * slotSize + paddingLeft;
    private static final int midBarOffset = 4;
    private static final int midBarHeight = 4;
    private final ServerContainerBuilder server;
    private int centerX;
    private int centerY;

    public ClientContainerBuilder(ServerContainerBuilder serverContainer) {
        super(serverContainer, serverContainer.playerInventory, new LiteralText(""));
        this.server = serverContainer;
        this.containerWidth = paddingRight + serverContainer.slotsX * slotSize + paddingLeft;
        this.containerHeight = 114 + serverContainer.slotsY * slotSize;
    }

    @Override
    protected void drawBackground(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bindTexture(CHEST_GUI_TEXTURE.internal);
        this.centerX = (this.width - this.containerWidth)/2;
        this.centerY = (this.height - this.containerHeight)/2;
        server.draw.accept(this);
    }

    /* IContainerBuilder */

    @Override
    public int drawTopBar(int x, int y, int slots) {
        super.blit(centerX + x, centerY + y, 0, 0, paddingLeft, topOffset);
        // Top Bar
        for (int k = 1; k <= slots; k++) {
            super.blit(centerX + x + paddingLeft + (k - 1) * slotSize, centerY + y, paddingLeft, 0, slotSize, topOffset);
        }
        // Top Right Corner
        super.blit(centerX + x + paddingLeft + slots * slotSize, centerY + y, paddingLeft + stdUiHorizSlots * slotSize, 0, paddingRight, topOffset);

        return y + topOffset;
    }

    @Override
    public void drawSlot(ItemStackHandler handler, int slotID, int x, int y) {
        x += paddingLeft;
        if (handler != null && handler.getSlotCount() > slotID) {
            super.blit(centerX + x, centerY + y, paddingLeft, topOffset, slotSize, slotSize);
        } else {
            DrawableHelper.fill(centerX + x, centerY + y, centerX + x + slotSize, centerY + y + slotSize, 0xFF444444);
        }
    }

    @Override
    public int drawSlotRow(ItemStackHandler handler, int start, int cols, int x, int y) {
        // Left Side
        super.blit(centerX + x, centerY + y, 0, topOffset, paddingLeft, slotSize);
        // Middle Slots
        for (int slotID = start; slotID < start + cols; slotID++) {
            int slotOff = (slotID - start);
            drawSlot(handler, slotID, x + slotOff * slotSize, y);
        }
        GL11.glColor4f(1, 1, 1, 1);
        // Right Side
        super.blit(centerX + x + paddingLeft + cols * slotSize, centerY + y, paddingLeft + stdUiHorizSlots * slotSize, topOffset, paddingRight, slotSize);
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
        // Left Bottom
        super.blit(centerX + x, centerY + y, 0, textureHeight - bottomOffset, paddingLeft, bottomOffset);
        // Middle Bottom
        for (int k = 1; k <= slots; k++) {
            super.blit(centerX + x + paddingLeft + (k - 1) * slotSize, centerY + y, paddingLeft, textureHeight - bottomOffset, slotSize, bottomOffset);
        }
        // Right Bottom
        super.blit(centerX + x + paddingLeft + slots * slotSize, centerY + y, paddingLeft + 9 * slotSize, textureHeight - bottomOffset, paddingRight, bottomOffset);

        return y + bottomOffset;
    }

    @Override
    public int drawPlayerTopBar(int x, int y) {
        super.blit(centerX + x, centerY + y, 0, 0, playerXSize, bottomOffset);
        return y + bottomOffset;
    }

    @Override
    public int drawPlayerMidBar(int x, int y) {
        super.blit(centerX + x, centerY + y, 0, midBarOffset, playerXSize, midBarHeight);
        return y + midBarHeight;
    }

    @Override
    public int drawPlayerInventory(int y, int horizSlots) {
        int normInvOffset = (horizSlots - stdUiHorizSlots) * slotSize / 2 + paddingLeft - 7;
        super.blit(centerX + normInvOffset, centerY + y, 0, 126 + 4, playerXSize, 96);
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
        super.drawCenteredString(this.font, text, x + centerX + this.containerWidth / 2, y + centerY, 14737632);
        this.minecraft.getTextureManager().bindTexture(CHEST_GUI_TEXTURE.internal);
    }

    @Override
    public void drawSlotOverlay(ItemStack stack, int x, int y) {
        x += centerX + 1 + paddingLeft;
        y += centerY + 1;

        this.minecraft.getItemRenderer().renderGuiItemIcon(stack.internal, x, y);
        this.minecraft.getTextureManager().bindTexture(CHEST_GUI_TEXTURE.internal);

        GlStateManager.enableAlphaTest();
        GlStateManager.disableDepthTest();
        DrawableHelper.fill(x, y, x + 16, y + 16, -2130706433);
        GlStateManager.enableDepthTest();

        GL11.glColor4f(1, 1, 1, 1);
    }

    @Override
    public void drawSlotOverlay(String spriteId, int x, int y, double height, int color) {
        x += centerX + 1 + paddingLeft;
        y += centerY + 1;

        // TODO better sprite map, but this kinda sucks between versions.  maybe add an enum...
        if (spriteId.equals("minecraft:blocks/fire_layer_1")) {
            spriteId = "minecraft:block/fire_1";
        }

        GUIHelpers.drawRect(x, y + (int)(16 - 16 * height), 16,  16 * height, color);

        Sprite sprite = minecraft.getSpriteAtlas().getSprite(spriteId);
        minecraft.getTextureManager().bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
        GlStateManager.color4f(1,1,1,1);
        DrawableHelper.blit(x, y, 0, 16, 16, sprite);
        minecraft.getTextureManager().bindTexture(CHEST_GUI_TEXTURE.internal);
    }
}
