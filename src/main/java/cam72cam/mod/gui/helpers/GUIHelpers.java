package cam72cam.mod.gui.helpers;

import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;
import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.resource.Identifier;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import org.lwjgl.opengl.GL11;

public class GUIHelpers {
    public static final Identifier CHEST_GUI_TEXTURE = new Identifier("textures/gui/container/generic_54.png");

    public static void drawRect(double x, double y, double width, double height, int color) {
        double zLevel = 0;

        float f3 = (float) (color >> 24 & 255) / 255.0F;
        float f = (float) (color >> 16 & 255) / 255.0F;
        float f1 = (float) (color >> 8 & 255) / 255.0F;
        float f2 = (float) (color & 255) / 255.0F;
        GL11.glColor4f(f, f1, f2, f3);

        GL11.glDisable(GL11.GL_TEXTURE_2D);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(GL11.GL_QUADS, VertexFormats.POSITION);
        bufferbuilder.vertex(x + 0, y + height, zLevel).next();
        bufferbuilder.vertex(x + width, y + height, zLevel).next();
        bufferbuilder.vertex(x + width, y + 0, zLevel).next();
        bufferbuilder.vertex(x + 0, y + 0, zLevel).next();
        tessellator.draw();

        GL11.glEnable(GL11.GL_TEXTURE_2D);

        GL11.glColor4f(1, 1, 1, 1);
    }

    public static void texturedRect(double x, double y, double width, double height) {
        double zLevel = 0;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(GL11.GL_QUADS, VertexFormats.POSITION_TEXTURE);
        bufferbuilder.vertex(x + 0, y + height, zLevel).texture(0, 1).next();
        bufferbuilder.vertex(x + width, y + height, zLevel).texture(1, 1).next();
        bufferbuilder.vertex(x + width, y + 0, zLevel).texture(1, 0).next();
        bufferbuilder.vertex(x + 0, y + 0, zLevel).texture(0, 0).next();
        tessellator.draw();
    }

    public static void drawFluid(Fluid fluid, double x, double d, double width, int height, int scale) {
        FluidVolume.create(fluid.internal, 1000).renderGuiRect(x, d, x + width, d + height);
        MinecraftClient.getInstance().getTextureManager().bindTexture(CHEST_GUI_TEXTURE.internal);
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
        GlStateManager.color4f(1, 1, 1, 1);
    }

    public static void drawCenteredString(String text, int x, int y, int color) {
        MinecraftClient.getInstance().textRenderer.drawWithShadow(text, (float) (x - MinecraftClient.getInstance().textRenderer.getStringWidth(text) / 2), (float) y, color);
    }

    public static void bindTexture(Identifier tex) {
        MinecraftClient.getInstance().getTextureManager().bindTexture(tex.internal);
    }

    public static int getScreenWidth() {
        return MinecraftClient.getInstance().window.getFramebufferWidth()/2;
    }

    public static int getScreenHeight() {
        return MinecraftClient.getInstance().window.getFramebufferHeight() /2;
    }

    public static void drawItem(ItemStack stack, int x, int y) {
        MinecraftClient.getInstance().getItemRenderer().renderGuiItemIcon(stack.internal, x, y);
    }
}
