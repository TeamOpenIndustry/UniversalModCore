package cam72cam.mod.gui.container;

import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.item.ItemStackHandler;
import com.mojang.blaze3d.platform.GlStateManager;
import cam72cam.mod.render.OpenGL;
import cam72cam.mod.resource.Identifier;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.opengl.GL11;

import java.util.function.Supplier;

import static cam72cam.mod.gui.helpers.GUIHelpers.CHEST_GUI_TEXTURE;
import static cam72cam.mod.gui.helpers.GUIHelpers.drawRect;

/** GUI Container wrapper for the client side, Do not use directly */
public class ClientContainerBuilder extends AbstractContainerScreen<ServerContainerBuilder> implements IContainerBuilder {
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
    private PoseStack stack;

    public ClientContainerBuilder(ServerContainerBuilder serverContainer, Inventory p_create_2_, Component p_create_3_) {
        super(serverContainer, serverContainer.playerInventory, new TextComponent(""));
        this.server = serverContainer;
        this.imageWidth = paddingRight + serverContainer.slotsX * slotSize + paddingLeft;
        this.imageHeight = server.ySize;
        this.valid = serverContainer.valid;
    }

    @Override
    protected void renderBg(PoseStack stack, float partialTicks, int mouseX, int mouseY) {
        try (OpenGL.With color = OpenGL.color(1, 1, 1, 1)) {
            //this.minecraft.getTextureManager().bindTexture(CHEST_GUI_TEXTURE);
            this.centerX = (this.width - this.imageWidth) / 2;
            this.centerY = (this.height - this.imageHeight) / 2;
            server.draw.accept(this);
        }
    }

    @Override
    public void containerTick() {
        super.tick();
        if (!valid.get()) {
            this.minecraft.player.closeContainer();
        }
    }

    /* IContainerBuilder */

    @Override
    public int drawTopBar(int x, int y, int slots) {
        try (OpenGL.With color = OpenGL.color(1,1,1,1); OpenGL.With tex = OpenGL.texture(CHEST_GUI_TEXTURE)) {
            super.blit(stack, centerX + x, centerY + y, 0, 0, paddingLeft, topOffset);
            // Top Bar
            for (int k = 1; k <= slots; k++) {
                super.blit(stack, centerX + x + paddingLeft + (k - 1) * slotSize, centerY + y, paddingLeft, 0, slotSize, topOffset);
            }
            // Top Right Corner
            super.blit(stack, centerX + x + paddingLeft + slots * slotSize, centerY + y, paddingLeft + stdUiHorizSlots * slotSize, 0, paddingRight, topOffset);
        }
        return y + topOffset;
    }

    @Override
    public void drawSlot(ItemStackHandler handler, int slotID, int x, int y) {
        try (OpenGL.With color = OpenGL.color(1,1,1,1); OpenGL.With tex = OpenGL.texture(CHEST_GUI_TEXTURE)) {
            x += paddingLeft;
            if (handler != null && handler.getSlotCount() > slotID) {
                super.blit(stack, centerX + x, centerY + y, paddingLeft, topOffset, slotSize, slotSize);
            } else {
                drawRect(centerX + x, centerY + y, slotSize, slotSize, 0xFF444444);
            }
        }
    }

    @Override
    public int drawSlotRow(ItemStackHandler handler, int start, int cols, int x, int y) {
        try (OpenGL.With color = OpenGL.color(1,1,1,1); OpenGL.With tex = OpenGL.texture(CHEST_GUI_TEXTURE)) {
            // Left Side
            super.blit(stack, centerX + x, centerY + y, 0, topOffset, paddingLeft, slotSize);
            // Middle Slots
            for (int slotID = start; slotID < start + cols; slotID++) {
                int slotOff = (slotID - start);
                drawSlot(handler, slotID, x + slotOff * slotSize, y);
            }
            GL11.glColor4f(1, 1, 1, 1);
            // Right Side
            super.blit(stack, centerX + x + paddingLeft + cols * slotSize, centerY + y, paddingLeft + stdUiHorizSlots * slotSize, topOffset, paddingRight, slotSize);
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
        try (OpenGL.With color = OpenGL.color(1,1,1,1); OpenGL.With tex = OpenGL.texture(CHEST_GUI_TEXTURE)) {
            // Left Bottom
            super.blit(stack, centerX + x, centerY + y, 0, textureHeight - bottomOffset, paddingLeft, bottomOffset);
            // Middle Bottom
            for (int k = 1; k <= slots; k++) {
                super.blit(stack, centerX + x + paddingLeft + (k - 1) * slotSize, centerY + y, paddingLeft, textureHeight - bottomOffset, slotSize, bottomOffset);
            }
            // Right Bottom
            super.blit(stack, centerX + x + paddingLeft + slots * slotSize, centerY + y, paddingLeft + 9 * slotSize, textureHeight - bottomOffset, paddingRight, bottomOffset);
        }
        return y + bottomOffset;
    }

    @Override
    public int drawPlayerTopBar(int x, int y) {
        try (OpenGL.With color = OpenGL.color(1,1,1,1); OpenGL.With tex = OpenGL.texture(CHEST_GUI_TEXTURE)) {
            super.blit(stack, centerX + x, centerY + y, 0, 0, playerXSize, bottomOffset);
        }
        return y + bottomOffset;
    }

    @Override
    public int drawPlayerMidBar(int x, int y) {
        try (OpenGL.With color = OpenGL.color(1,1,1,1); OpenGL.With tex = OpenGL.texture(CHEST_GUI_TEXTURE)) {
            super.blit(stack, centerX + x, centerY + y, 0, midBarOffset, playerXSize, midBarHeight);
        }
        return y + midBarHeight;
    }

    @Override
    public int drawPlayerInventory(int y, int horizSlots) {
        int normInvOffset = (horizSlots - stdUiHorizSlots) * slotSize / 2 + paddingLeft - 7;
        try (OpenGL.With color = OpenGL.color(1,1,1,1); OpenGL.With tex = OpenGL.texture(CHEST_GUI_TEXTURE)) {
            super.blit(stack, centerX + normInvOffset, centerY + y, 0, 126 + 4, playerXSize, 96);
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
        super.drawCenteredString(stack, this.font, text, x + centerX + this.imageWidth / 2, y + centerY, 14737632);
    }

    @Override
    public void drawSlotOverlay(ItemStack stack, int x, int y) {
        x += centerX + 1 + paddingLeft;
        y += centerY + 1;

        this.minecraft.getItemRenderer().renderAndDecorateItem(stack.internal, x, y);

        try (
                OpenGL.With color = OpenGL.color(1, 1, 1, 1);
                OpenGL.With alpha = OpenGL.bool(GL11.GL_ALPHA_TEST, true);
                OpenGL.With depth = OpenGL.bool(GL11.GL_DEPTH_TEST, false)
        ) {
            //1.17.1 GlStateManager._enableAlphaTest();
            GlStateManager._disableDepthTest();
            drawRect(x, y, 16, 16, -2130706433);
            GlStateManager._enableDepthTest();
        }
    }

    @Override
    public void drawSlotOverlay(String spriteId, int x, int y, double height, int color) {
        x += centerX + 1 + paddingLeft;
        y += centerY + 1;

        try (OpenGL.With c = OpenGL.color(1, 1, 1, 1)) {
            fill(new PoseStack(), x, y + (int) (16 - 16 * height), x + 16, y + 16, color);
        }


        // TODO better sprite map, but this kinda sucks between versions.  maybe add an enum...
        if (spriteId.equals("minecraft:blocks/fire_layer_1")) {
            spriteId = "minecraft:block/fire_1";
        }

        TextureAtlasSprite sprite = minecraft.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(new ResourceLocation(spriteId));
        try (
                OpenGL.With color_ = OpenGL.color(1,1,1,1);
                OpenGL.With tex = OpenGL.texture(new Identifier(TextureAtlas.LOCATION_BLOCKS))
        ) {
            blit(stack, x, y, 0, 16, 16, sprite);
        }
    }

    @Override
    public void render(PoseStack stack, int mouseX, int mouseY, float partialTicks)
    {
        this.stack = stack;
        super.render(stack, mouseX, mouseY, partialTicks);
        this.renderTooltip(stack, mouseX, mouseY);
    }
}
