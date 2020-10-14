package cam72cam.mod.gui.helpers;

import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.render.OpenGL;
import cam72cam.mod.resource.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class GUIHelpers {
    public static final Identifier CHEST_GUI_TEXTURE = new Identifier("textures/gui/container/generic_54.png");
    private static final Gui instance = new Gui();

    public static void drawRect(int x, int y, int width, int height, int color) {
        try (
            OpenGL.With c = OpenGL.color(0, 0, 0, 0);
            OpenGL.With tex = OpenGL.bool(GL11.GL_TEXTURE_2D, false);
            OpenGL.With blend = OpenGL.bool(GL11.GL_BLEND, true)
        ) {
            Gui.drawRect(x, y, x + width, y + height, color);
        }
    }

    public static void texturedRect(Identifier tex, int x, int y, int width, int height) {
        texturedRect(tex, x, y, width, height, 0, 0);
    }

    public static void texturedRect(Identifier tex, int x, int y, int width, int height, int texX, int texY) {
        try (OpenGL.With t = OpenGL.texture(tex)) {
            Gui.drawScaledCustomSizeModalRect(x, y, 0, 0, 1, 1, width, height, 1, 1);
        }
    }

    public static void drawFluid(Fluid fluid, int x, int d, int width, int height, int scale) {
        TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(fluid.internal.getStill().toString());
        drawSprite(sprite, fluid.internal.getColor(), x, d, width, height);
    }

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

    public static void drawSprite(TextureAtlasSprite sprite, int col, int x, int y, int width, int height) {
        double zLevel = 0;

        try (
                OpenGL.With tex = OpenGL.texture(new Identifier(TextureMap.LOCATION_BLOCKS_TEXTURE));
                OpenGL.With color = OpenGL.color((col >> 16 & 255) / 255.0f, (col >> 8 & 255) / 255.0f, (col & 255) / 255.0f, 1)
        ) {
            int iW = sprite.getIconWidth();
            int iH = sprite.getIconHeight();

            for (int offY = 0; offY < height; offY += iH) {
                double curHeight = Math.min(iH, height - offY);
                for (int offX = 0; offX < width; offX += iW) {
                    double curWidth = Math.min(iW, width - offX);
                    GUIHelpers.sprite.setup(sprite, (int)curWidth, (int)curHeight);
                    instance.drawTexturedModalRect(x + offX, y + offY, GUIHelpers.sprite, (int)curWidth, (int)curHeight);
                }
            }
        }
    }

    public static void drawTankBlock(int x, int y, int width, int height, Fluid fluid, float percentFull) {
        drawTankBlock(x, y, width, height, fluid, percentFull, true, 0x00000000);
    }

    public static void drawTankBlock(int x, int y, int width, int height, Fluid fluid, float percentFull, boolean drawBackground, int color) {
        if (drawBackground) {
            drawRect(x, y, width, height, 0xFF000000);
        }

        if (percentFull > 0 && fluid != null) {
            int fullHeight = Math.max(1, (int) (height * percentFull));
            drawFluid(fluid, x, y + height - fullHeight, width, fullHeight, 2);
            drawRect(x, y + height - fullHeight, width, fullHeight, color);
        }
    }

    public static void drawCenteredString(String text, int x, int y, int color) {
        try (OpenGL.With c = OpenGL.color(1, 1, 1, 1); OpenGL.With alpha = OpenGL.bool(GL11.GL_ALPHA_TEST, true)) {
            Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(text, (float) (x - Minecraft.getMinecraft().fontRenderer.getStringWidth(text) / 2), (float) y, color);
        }
    }

    public static int getScreenWidth() {
        return new ScaledResolution(Minecraft.getMinecraft()).getScaledWidth();
    }

    public static int getScreenHeight() {
        return new ScaledResolution(Minecraft.getMinecraft()).getScaledHeight();
    }

    public static void drawItem(ItemStack stack, int x, int y) {
        try (
            OpenGL.With c = OpenGL.color(1, 1, 1, 1);
            OpenGL.With alpha = OpenGL.bool(GL11.GL_ALPHA_TEST, true);
            OpenGL.With blend = OpenGL.blend(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            OpenGL.With rescale = OpenGL.bool(GL12.GL_RESCALE_NORMAL, true);
        ) {
            Minecraft.getMinecraft().getRenderItem().renderItemIntoGUI(stack.internal, x, y);
        }
    }
}
