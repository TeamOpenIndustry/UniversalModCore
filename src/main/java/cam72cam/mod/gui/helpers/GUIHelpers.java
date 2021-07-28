package cam72cam.mod.gui.helpers;

import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.render.OpenGL;
import cam72cam.mod.resource.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/** Common GUI functions that don't really fit anywhere else */
public class GUIHelpers {
    /** Standard 54 slot chest UI */
    public static final Identifier CHEST_GUI_TEXTURE = new Identifier("textures/gui/container/generic_54.png");
    // Internal hack for using Gui functions
    private static final Gui instance = new Gui();

    /** Draw a solid color block */
    public static void drawRect(int x, int y, int width, int height, int color) {
        try (
            OpenGL.With c = OpenGL.color(0, 0, 0, 0);
            OpenGL.With tex = OpenGL.bool(GL11.GL_TEXTURE_2D, false);
            OpenGL.With blend = OpenGL.bool(GL11.GL_BLEND, true)
        ) {
            Gui.drawRect(x, y, x + width, y + height, color);
        }
    }

    /** Draw a full image (tex) at coords with given width/height */
    public static void texturedRect(Identifier tex, int x, int y, int width, int height) {
        try (OpenGL.With t = OpenGL.texture(tex)) {
            Gui.drawScaledCustomSizeModalRect(x, y, 0, 0, 1, 1, width, height, 1, 1);
        }
    }

    /** Draw fluid block at coords */
    public static void drawFluid(Fluid fluid, int x, int y, int width, int height) {
        TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(fluid.internal.getStillIcon().getIconName());
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
    private static final HackedSprite sprite = new HackedSprite();

    /** Draw a texture sprite at coords, tinted with col  */
    private static void drawSprite(TextureAtlasSprite sprite, int col, int x, int y, int width, int height) {
        double zLevel = 0;

        try (
                OpenGL.With tex = OpenGL.texture(new Identifier(TextureMap.locationBlocksTexture));
                OpenGL.With color = OpenGL.color((col >> 16 & 255) / 255.0f, (col >> 8 & 255) / 255.0f, (col & 255) / 255.0f, 1)
        ) {
            int iW = sprite.getIconWidth();
            int iH = sprite.getIconHeight();

            for (int offY = 0; offY < height; offY += iH) {
                double curHeight = Math.min(iH, height - offY);
                for (int offX = 0; offX < width; offX += iW) {
                    double curWidth = Math.min(iW, width - offX);
                    GUIHelpers.sprite.setup(sprite, (int)curWidth, (int)curHeight);
                    instance.drawTexturedModelRectFromIcon(x + offX, y + offY, GUIHelpers.sprite, (int)curWidth, (int)curHeight);
                }
            }
        }
    }

    /** Draw the fluid in a tank with a black background at % full */
    public static void drawTankBlock(int x, int y, int width, int height, Fluid fluid, float percentFull) {
        drawTankBlock(x, y, width, height, fluid, percentFull, true, 0x00000000);
    }

    /** Draw the fluid in a tank with a colored background at % full */
    public static void drawTankBlock(int x, int y, int width, int height, Fluid fluid, float percentFull, boolean drawBackground, int color) {
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
    public static void drawCenteredString(String text, int x, int y, int color) {
        try (OpenGL.With c = OpenGL.color(1, 1, 1, 1); OpenGL.With alpha = OpenGL.bool(GL11.GL_ALPHA_TEST, true)) {
            Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(text, (x - Minecraft.getMinecraft().fontRendererObj.getStringWidth(text) / 2), y, color);
        }
    }

    /** Screen Width in pixels (std coords) */
    public static int getScreenWidth() {
        Minecraft mc = Minecraft.getMinecraft();
        return new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaledWidth();
    }

    /** Screen Height in pixels (std coords) */
    public static int getScreenHeight() {
        Minecraft mc = Minecraft.getMinecraft();
        return new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaledHeight();
    }

    /** Draw a Item at the given coords */
    public static void drawItem(ItemStack stack, int x, int y) {
        IItemRenderer ir = MinecraftForgeClient.getItemRenderer(stack.internal, IItemRenderer.ItemRenderType.INVENTORY);
        try (
                OpenGL.With c = OpenGL.color(1, 1, 1, 1);
                OpenGL.With alpha = OpenGL.bool(GL11.GL_ALPHA_TEST, true);
                OpenGL.With blend = OpenGL.blend(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                OpenGL.With rescale = OpenGL.bool(GL12.GL_RESCALE_NORMAL, true);
                OpenGL.With mat = OpenGL.matrix()
        ) {
            GL11.glPushMatrix();
            GL11.glTranslated(x, y, 0);
            ir.renderItem(IItemRenderer.ItemRenderType.INVENTORY, stack.internal);
            GL11.glPopMatrix();
        }
    }
}
