package cam72cam.mod.gui.helpers;

import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.render.OpenGL;
import cam72cam.mod.resource.Identifier;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
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
            AbstractGui.fill(new MatrixStack(), x, y, x + width, y + height, color);
        }
    }

    /** Draw a full image (tex) at coords with given width/height */
    public static void texturedRect(Identifier tex, int x, int y, int width, int height) {
        try (OpenGL.With t = OpenGL.texture(tex)) {
            // X Y, U V, UW VH, W H, TW TH
            // AbstractGui.blit(x, y, 0, 0, 1, 1, width, height, 1, 1);
            // X Y, W H, U V, UW VH, TW TH
            AbstractGui.blit(new MatrixStack(), x, y, width, height, 0, 0, 1, 1, 1, 1);
        }
    }

    /** Draw fluid block at coords */
    public static void drawFluid(Fluid fluid, int x, int y, int width, int height) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasSpriteGetter(AtlasTexture.LOCATION_BLOCKS_TEXTURE).apply(fluid.internal.get(0).getAttributes().getStillTexture());
        drawSprite(sprite, fluid.internal.get(0).getAttributes().getColor(), x, y, width, height);
    }

    /** Draw a texture sprite at coords, tinted with col  */
    private static void drawSprite(TextureAtlasSprite sprite, int col, int x, int y, int width, int height) {
        double zLevel = 0;

        try (
                OpenGL.With tex = OpenGL.texture(new Identifier(AtlasTexture.LOCATION_BLOCKS_TEXTURE));
                OpenGL.With color = OpenGL.color((col >> 16 & 255) / 255.0f, (col >> 8 & 255) / 255.0f, (col & 255) / 255.0f, 1)
        ) {
            int iW = sprite.getWidth();
            int iH = sprite.getHeight();

            float minU = sprite.getMinU();
            float minV = sprite.getMinV();


            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
            for (int offY = 0; offY < height; offY += iH) {
                double curHeight = Math.min(iH, height - offY);
                float maxVScaled = sprite.getInterpolatedV(16.0 * curHeight / iH);
                for (int offX = 0; offX < width; offX += iW) {
                    double curWidth = Math.min(iW, width - offX);
                    float maxUScaled = sprite.getInterpolatedU(16.0 * curWidth / iW);
                    buffer.pos(x + offX, y + offY, zLevel).tex(minU, minV).endVertex();
                    buffer.pos(x + offX, y + offY + curHeight, zLevel).tex(minU, maxVScaled).endVertex();
                    buffer.pos(x + offX + curWidth, y + offY + curHeight, zLevel).tex(maxUScaled, maxVScaled).endVertex();
                    buffer.pos(x + offX + curWidth, y + offY, zLevel).tex(maxUScaled, minV).endVertex();
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
    }

    /** Draw a shadowed string offset from the center of coords */
    public static void drawCenteredString(String text, int x, int y, int color) {
        try (OpenGL.With c = OpenGL.color(1, 1, 1, 1); OpenGL.With alpha = OpenGL.bool(GL11.GL_ALPHA_TEST, true)) {
            Minecraft.getInstance().fontRenderer.drawStringWithShadow(new MatrixStack(), text, (float) (x - Minecraft.getInstance().fontRenderer.getStringWidth(text) / 2), (float) y, color);
        }
    }

    /** Screen Width in pixels (std coords) */
    public static int getScreenWidth() {
        return Minecraft.getInstance().getMainWindow().getFramebufferWidth()/2;
    }

    /** Screen Height in pixels (std coords) */
    public static int getScreenHeight() {
        return Minecraft.getInstance().getMainWindow().getFramebufferHeight()/2;
    }

    /** Draw a Item at the given coords */
    public static void drawItem(ItemStack stack, int x, int y) {
        try (
            OpenGL.With c = OpenGL.color(1, 1, 1, 1);
            OpenGL.With alpha = OpenGL.bool(GL11.GL_ALPHA_TEST, true);
            OpenGL.With blend = OpenGL.blend(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            OpenGL.With rescale = OpenGL.bool(GL12.GL_RESCALE_NORMAL, true);
        ) {
            Minecraft.getInstance().getItemRenderer().renderItemIntoGUI(stack.internal, x, y);
        }
    }
}
