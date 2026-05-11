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
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        this.texturedRect(VANILLA_BUTTON, x, y, 0, 46 + state * 20, width / 2, height);
        this.texturedRect(VANILLA_BUTTON, x + width / 2, y, 200 - width / 2, 46 + state * 20, width / 2, height);
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
    public void drawCenteredString(String text, int x, int y, int color) {
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
