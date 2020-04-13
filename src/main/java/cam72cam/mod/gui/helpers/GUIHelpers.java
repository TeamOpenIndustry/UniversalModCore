package cam72cam.mod.gui.helpers;

import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.resource.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import org.lwjgl.opengl.GL11;

public class GUIHelpers {
    public static final ResourceLocation CHEST_GUI_TEXTURE = new ResourceLocation("textures/gui/container/generic_54.png");

    public static void drawRect(double x, double y, double width, double height, int color) {
        double zLevel = 0;

        float f3 = (float) (color >> 24 & 255) / 255.0F;
        float f = (float) (color >> 16 & 255) / 255.0F;
        float f1 = (float) (color >> 8 & 255) / 255.0F;
        float f2 = (float) (color & 255) / 255.0F;
        GL11.glColor4f(f, f1, f2, f3);

        GL11.glDisable(GL11.GL_TEXTURE_2D);

        Tessellator bufferbuilder = Tessellator.instance;
        bufferbuilder.startDrawing(GL11.GL_QUADS);
        bufferbuilder.addVertex(x + 0, y + height, zLevel);
        bufferbuilder.addVertex(x + width, y + height, zLevel);
        bufferbuilder.addVertex(x + width, y + 0, zLevel);
        bufferbuilder.addVertex(x + 0, y + 0, zLevel);
        bufferbuilder.draw();

        GL11.glEnable(GL11.GL_TEXTURE_2D);

        GL11.glColor4f(1, 1, 1, 1);
    }

    public static void texturedRect(double x, double y, double width, double height) {
        double zLevel = 0;
        Tessellator bufferbuilder = Tessellator.instance;
        bufferbuilder.startDrawing(GL11.GL_QUADS);
        bufferbuilder.addVertexWithUV(x + 0, y + height, zLevel, 0, 1);
        bufferbuilder.addVertexWithUV(x + width, y + height, zLevel, 1, 1);
        bufferbuilder.addVertexWithUV(x + width, y + 0, zLevel, 1, 0);
        bufferbuilder.addVertexWithUV(x + 0, y + 0, zLevel, 0, 0);
        bufferbuilder.draw();
    }

    public static void drawFluid(Fluid fluid, double x, double d, double width, int height, int scale) {
        drawSprite(fluid.internal.getStillIcon(), fluid.internal.getColor(), x, d, width, height, scale);
    }

    public static void drawSprite(IIcon sprite, int col, double x, double y, double width, double height, int scale) {
        double zLevel = 0;

        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);

        GL11.glColor4f((col >> 16 & 255) / 255.0f, (col >> 8 & 255) / 255.0f, (col & 255) / 255.0f, 1);
        int iW = sprite.getIconWidth() * scale;
        int iH = sprite.getIconHeight() * scale;

        float minU = sprite.getMinU();
        float minV = sprite.getMinV();

        Tessellator buffer = Tessellator.instance;
        buffer.startDrawing(GL11.GL_QUADS);
        for (int offY = 0; offY < height; offY += iH) {
            double curHeight = Math.min(iH, height - offY);
            float maxVScaled = sprite.getInterpolatedV(16.0 * curHeight / iH);
            for (int offX = 0; offX < width; offX += iW) {
                double curWidth = Math.min(iW, width - offX);
                float maxUScaled = sprite.getInterpolatedU(16.0 * curWidth / iW);
                buffer.addVertexWithUV(x + offX, y + offY, zLevel, minU, minV);
                buffer.addVertexWithUV(x + offX, y + offY + curHeight, zLevel, minU, maxVScaled);
                buffer.addVertexWithUV(x + offX + curWidth, y + offY + curHeight, zLevel, maxUScaled, maxVScaled);
                buffer.addVertexWithUV(x + offX + curWidth, y + offY, zLevel, maxUScaled, minV);
            }
        }
        buffer.draw();

        Minecraft.getMinecraft().getTextureManager().bindTexture(CHEST_GUI_TEXTURE);
    }

    public static void drawTankBlock(double x, double y, double width, double height, Fluid fluid, float percentFull) {
        drawTankBlock(x, y, width, height, fluid, percentFull, true, 0x00000000);
    }

    public static void drawTankBlock(double x, double y, double width, double height, Fluid fluid, float percentFull, boolean drawBackground, int color) {
        if (drawBackground) {
            drawRect(x, y, width, height, 0xFF000000);
        }

        if (percentFull > 0 && fluid != null) {
            int fullHeight = Math.max(1, (int) (height * percentFull));
            drawFluid(fluid, x, y + height - fullHeight, width, fullHeight, 2);
            drawRect(x, y + height - fullHeight, width, fullHeight, color);
        }
        GL11.glColor4f(1, 1, 1, 1);
    }

    public static void drawCenteredString(String text, int x, int y, int color) {
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(text, (x - Minecraft.getMinecraft().fontRendererObj.getStringWidth(text) / 2), y, color);
    }

    public static void bindTexture(Identifier tex) {
        Minecraft.getMinecraft().renderEngine.bindTexture(tex.internal);
    }

    public static int getScreenWidth() {
        Minecraft mc = Minecraft.getMinecraft();
        return new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaledWidth();
    }

    public static int getScreenHeight() {
        Minecraft mc = Minecraft.getMinecraft();
        return new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaledHeight();
    }

    private static RenderItem itemrenderer = new RenderItem();
    public static void drawItem(ItemStack stack, int x, int y) {
        IItemRenderer ir = MinecraftForgeClient.getItemRenderer(stack.internal, IItemRenderer.ItemRenderType.INVENTORY);
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, 0);
        ir.renderItem(IItemRenderer.ItemRenderType.INVENTORY, stack.internal);
        GL11.glPopMatrix();
    }
}
