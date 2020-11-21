package cam72cam.mod.gui.helpers;

import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.render.OpenGL;
import cam72cam.mod.resource.Identifier;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/** Common GUI functions that don't really fit anywhere else */
public class GUIHelpers {
    /** Standard 54 slot chest UI */
    public static final Identifier CHEST_GUI_TEXTURE = new Identifier("textures/gui/container/generic_54.png");

    /** Draw a solid color block */
    public static void drawRect(int x, int y, int width, int height, int color) {
        try (
            OpenGL.With c = OpenGL.color(0, 0, 0, 0);
            OpenGL.With tex = OpenGL.bool(GL11.GL_TEXTURE_2D, false);
            OpenGL.With blend = OpenGL.bool(GL11.GL_BLEND, true)
        ) {
            DrawableHelper.fill(x, y, x + width, y + height, color);
        }
    }

    /** Draw a full image (tex) at coords with given width/height */
    public static void texturedRect(Identifier tex, int x, int y, int width, int height) {
        try (OpenGL.With t = OpenGL.texture(tex)) {
            DrawableHelper.blit(x, y, width, height, 0, 0, 1, 1, 1, 1);
        }
    }

    /** Draw fluid block at coords */
    public static void drawFluid(Fluid fluid, int x, int y, int width, int height) {
        Sprite sprite = MinecraftClient.getInstance().getSpriteAtlas().getSprite(fluid.internal.get(0).spriteId);
        drawSprite(sprite, fluid.internal.get(0).renderColor, x, y, width, height);
    }

    /** Draw a texture sprite at coords, tinted with col  */
    private static void drawSprite(Sprite sprite, int col, int x, int y, int width, int height) {
        double zLevel = 0;

        try (
                OpenGL.With tex = OpenGL.texture(new Identifier(SpriteAtlasTexture.BLOCK_ATLAS_TEX));
                OpenGL.With color = OpenGL.color((col >> 16 & 255) / 255.0f, (col >> 8 & 255) / 255.0f, (col & 255) / 255.0f, 1)
        ) {
            int iW = sprite.getWidth();
            int iH = sprite.getHeight();

            float minU = sprite.getMinU();
            float minV = sprite.getMinV();


            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(7, VertexFormats.POSITION_TEXTURE);
            for (int offY = 0; offY < height; offY += iH) {
                double curHeight = Math.min(iH, height - offY);
                float maxVScaled = sprite.getFrameV(16.0 * curHeight / iH);
                for (int offX = 0; offX < width; offX += iW) {
                    double curWidth = Math.min(iW, width - offX);
                    float maxUScaled = sprite.getFrameU(16.0 * curWidth / iW);
                    buffer.vertex(x + offX, y + offY, zLevel).texture(minU, minV).end();
                    buffer.vertex(x + offX, y + offY + curHeight, zLevel).texture(minU, maxVScaled).end();
                    buffer.vertex(x + offX + curWidth, y + offY + curHeight, zLevel).texture(maxUScaled, maxVScaled).end();
                    buffer.vertex(x + offX + curWidth, y + offY, zLevel).texture(maxUScaled, minV).end();
                }
            }
            tessellator.draw();
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
        GlStateManager.color4f(1, 1, 1, 1);
    }

    /** Draw a shadowed string offset from the center of coords */
    public static void drawCenteredString(String text, int x, int y, int color) {
        try (OpenGL.With c = OpenGL.color(1, 1, 1, 1); OpenGL.With alpha = OpenGL.bool(GL11.GL_ALPHA_TEST, true)) {
            MinecraftClient.getInstance().textRenderer.drawWithShadow(text, (float) (x - MinecraftClient.getInstance().textRenderer.getStringWidth(text) / 2), (float) y, color);
        }
    }

    /** Screen Width in pixels (std coords) */
    public static int getScreenWidth() {
        return MinecraftClient.getInstance().window.getFramebufferWidth()/2;
    }

    /** Screen Height in pixels (std coords) */
    public static int getScreenHeight() {
        return MinecraftClient.getInstance().window.getFramebufferHeight() /2;
    }

    /** Draw a Item at the given coords */
    public static void drawItem(ItemStack stack, int x, int y) {
        try (
            OpenGL.With c = OpenGL.color(1, 1, 1, 1);
            OpenGL.With alpha = OpenGL.bool(GL11.GL_ALPHA_TEST, true);
            OpenGL.With blend = OpenGL.blend(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            OpenGL.With rescale = OpenGL.bool(GL12.GL_RESCALE_NORMAL, true);
        ) {
            MinecraftClient.getInstance().getItemRenderer().renderGuiItemIcon(stack.internal, x, y);
        }
    }
}
