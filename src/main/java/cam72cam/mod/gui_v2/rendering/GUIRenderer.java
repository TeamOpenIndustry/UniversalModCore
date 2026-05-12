package cam72cam.mod.gui_v2.rendering;

import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.render.opengl.RenderContext;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.Texture;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.util.With;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import util.Matrix4;

public class GUIRenderer {
    public static final Identifier VANILLA_BUTTON = new Identifier("textures/gui/widgets.png");

    private final GuiScreen instance;
    private final ScaledResolution resolution;

    public GUIRenderer(GuiScreen gui) {
        this.instance = gui;
        this.resolution = new ScaledResolution(Minecraft.getMinecraft());
    }

    /** Draw a solid color block */
    public void drawRect(int x, int y, int width, int height, int color) {
        Gui.drawRect(x, y, x + width, y + height, color);
    }

    /** Draw a full image (tex) at coords with given width/height */
    public void texturedRect(Identifier tex, int x, int y, int width, int height) {
        this.texturedRect(tex, x, y, 0, 0, width, height);
    }

    /** Draw a full image (tex) at coords with given width/height */
    public void texturedRect(Identifier tex, int x, int y, int startU, int startV, int width, int height) {
        try (With ctx = RenderContext.apply(new RenderState().texture(Texture.wrap(tex)))) {
            instance.drawTexturedModalRect(x, y, startU, startV, width, height);
        }
    }

    //0 - disabled, 1 - normal, 2 - hovering
    public void drawVanillaButton(int x, int y, int width, int height, int state) {
        if (width < 6 || height < 6) {
            //Don't handle
            return;
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        int uBase = 0;
        int vBase = 46 + state * 20;
        int border = 3;
        int texTotalW = 200;
        int texTotalH = 20;

        int uLeft   = uBase;
        int uCenter = uBase + border;
        int uRight  = uBase + texTotalW - border;
        int vTop    = vBase;
        int vMiddle = vBase + border;
        int vBottom = vBase + texTotalH - border;

        int centerTexW = texTotalW - 2 * border;
        int centerTexH = texTotalH - 2 * border;

        // Corners
        texturedRect(VANILLA_BUTTON, x, y, uLeft,  vTop,    border, border);
        texturedRect(VANILLA_BUTTON, x + width - border, y, uRight, vTop, border, border);
        texturedRect(VANILLA_BUTTON, x, y + height - border, uLeft,  vBottom, border, border);
        texturedRect(VANILLA_BUTTON, x + width - border, y + height - border, uRight, vBottom, border, border);

        int innerW =  width - 2 * border;
        int innerH = height - 2 * border;

        // Upper/Lower edge
        if (innerW > 0) {
            int upperY = y;
            int lowerY = y + height - border;
            int startX = x + border;
            int remaining = innerW;
            while (remaining > 0) {
                int w = Math.min(remaining, centerTexW);
                texturedRect(VANILLA_BUTTON, startX, upperY, uCenter, vTop, w, border);
                texturedRect(VANILLA_BUTTON, startX, lowerY, uCenter, vBottom, w, border);
                startX += w;
                remaining -= w;
            }
        }

        // Left/Right Edge
        if (innerH > 0) {
            int leftX = x;
            int rightX = x + width - border;
            int startY = y + border;
            int remaining = innerH;
            while (remaining > 0) {
                int h = Math.min(remaining, centerTexH);
                texturedRect(VANILLA_BUTTON, leftX, startY, uLeft, vMiddle, border, h);
                texturedRect(VANILLA_BUTTON, rightX, startY, uRight, vMiddle, border, h);
                startY += h;
                remaining -= h;
            }
        }

        // Internal
        if (innerW > 0 && innerH > 0) {
            int startY = y + border;
            int remainingH = innerH;
            while (remainingH > 0) {
                int h = Math.min(remainingH, centerTexH);
                int startX = x + border;
                int remainingW = innerW;
                while (remainingW > 0) {
                    int w = Math.min(remainingW, centerTexW);
                    texturedRect(VANILLA_BUTTON, startX, startY, uCenter, vMiddle, w, h);
                    startX += w;
                    remainingW -= w;
                }
                startY += h;
                remainingH -= h;
            }
        }
    }

    /** Draw fluid block at coords */
    public void drawFluid(Fluid fluid, int x, int y, int width, int height) {
        TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(fluid.internal.getStill().toString());
        drawSprite(sprite, fluid.internal.getColor(), x, y, width, height);
    }

    /** This is kinda fun, we want to use the standard sprite drawer with a partial sprite! */
    private static class HackedSprite extends TextureAtlasSprite {
        HackedSprite() {
            super("unknown");
        }

        void setup(TextureAtlasSprite other, int width, int height) {
            this.copyFrom(other);
            this.width = width;
            this.height = height;
            int inX = (int) (originX / getMinU());
            int inY = (int) (originY / getMinV());
            this.initSprite(inX, inY, originX, originY, rotated);
        }
    }
    private final HackedSprite sprite = new HackedSprite();

    /** Draw a texture sprite at coords, tinted with col  */
    private void drawSprite(TextureAtlasSprite sprite, int col, int x, int y, int width, int height) {
        double zLevel = 0;

        try (With ctx = RenderContext.apply(
                new RenderState()
                        .texture(Texture.wrap(new Identifier(TextureMap.LOCATION_BLOCKS_TEXTURE)))
                        .color((col >> 16 & 255) / 255.0f, (col >> 8 & 255) / 255.0f, (col & 255) / 255.0f, 1)
                        .stage(RenderContext.Stage.GUI)
        )) {
            int iW = sprite.getIconWidth();
            int iH = sprite.getIconHeight();

            for (int offY = 0; offY < height; offY += iH) {
                double curHeight = Math.min(iH, height - offY);
                for (int offX = 0; offX < width; offX += iW) {
                    double curWidth = Math.min(iW, width - offX);
                    this.sprite.setup(sprite, (int)curWidth, (int)curHeight);
                    instance.drawTexturedModalRect(x + offX, y + offY, this.sprite, (int)curWidth, (int)curHeight);
                }
            }
        }
    }

    /** Draw the fluid in a tank with a black background at % full */
    public void drawTankBlock(int x, int y, int width, int height, Fluid fluid, float percentFull) {
        drawTankBlock(x, y, width, height, fluid, percentFull, true, 0x00000000);
    }

    /** Draw the fluid in a tank with a colored background at % full */
    public void drawTankBlock(int x, int y, int width, int height, Fluid fluid, float percentFull, boolean drawBackground, int color) {
        if (drawBackground) {
            drawRect(x, y, width, height, 0xFF000000);
        }

        if (percentFull > 0 && fluid != null) {
            int fullHeight = Math.max(1, (int) (height * percentFull));
            drawFluid(fluid, x, y + height - fullHeight, width, fullHeight);
            drawRect(x, y + height - fullHeight, width, fullHeight, color);
        }
    }

    /** Draw a shadowed string offset from the center of coords */
    public void drawCenteredString(String text, int x, int y, int color /*ARGB*/) {
        drawCenteredString(text, x, y, color, new Matrix4());
    }
    public void drawCenteredString(String text, int x, int y, int color, Matrix4 matrix) {
        drawString(text, (int) (x - Minecraft.getMinecraft().fontRenderer.getStringWidth(text) / 2f), y, color, matrix);
    }

    /** Draw a left-aligned shadowed string */
    public void drawString(String text, int x, int y, int color) {
        drawString(text, x, y, color, new Matrix4());
    }
    public void drawString(String text, int x, int y, int color, Matrix4 matrix) {
        RenderState state = new RenderState().color(1, 1, 1, 1).alpha_test(true);
        state.model_view().multiply(matrix);
        state.stage(RenderContext.Stage.GUI);
        try (With ctx = RenderContext.apply(state)) {
            GlStateManager.color(1, 1, 1, 0);
            Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(text, x, y, color);
        }
    }

    /** Gat a string's internal width for further use */
    public int getTextWidth(String text) {
        return Minecraft.getMinecraft().fontRenderer.getStringWidth(text);
    }

    /** Screen Width in pixels (std coords) */
    public int getScreenWidth() {
        return resolution.getScaledWidth();
    }

    /** Screen Height in pixels (std coords) */
    public int getScreenHeight() {
        return resolution.getScaledHeight();
    }

    /** Draw a Item at the given coords */
    public void drawItem(ItemStack stack, int x, int y) {
        drawItem(stack, x, y, new Matrix4());
    }

    public void drawItem(ItemStack stack, int x, int y, Matrix4 matrix) {
        RenderState state = new RenderState();
        state.model_view().multiply(matrix);
        try (With ctx = RenderContext.apply(state)) {
            Minecraft.getMinecraft().getRenderItem().renderItemIntoGUI(stack.internal, x, y);
        }
    }
}
