package cam72cam.mod.gui.helpers;

import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.util.With;
import cam72cam.mod.render.opengl.BlendMode;
import cam72cam.mod.render.opengl.RenderContext;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.Texture;
import cam72cam.mod.resource.Identifier;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;
import util.Matrix4;

/** Common GUI functions that don't really fit anywhere else */
public class GUIHelpers {
    /** Standard 54 slot chest UI */
    public static final Identifier CHEST_GUI_TEXTURE = new Identifier("textures/gui/container/generic_54.png");

    /** Draw a solid color block */
    public static void drawRect(int x, int y, int width, int height, int color) {
        try (With ctx = RenderContext.apply(
                new RenderState()
                        .color(1, 1, 1, 1)
                        .texture(Texture.NO_TEXTURE)
                        .blend(BlendMode.OPAQUE)
        )) {
            GuiComponent.fill(new PoseStack(), x, y, x + width, y + height, color);
        }
    }

    /** Draw a full image (tex) at coords with given width/height */
    public static void texturedRect(Identifier tex, int x, int y, int width, int height) {
        try (With ctx = RenderContext.apply(
                new RenderState().texture(Texture.wrap(tex))
        )) {
            // X Y, U V, UW VH, W H, TW TH
            // AbstractGui.blit(x, y, 0, 0, 1, 1, width, height, 1, 1);
            // X Y, W H, U V, UW VH, TW TH
            GuiComponent.blit(new PoseStack(), x, y, width, height, 0, 0, 1, 1, 1, 1);
        }
    }

    /** Draw fluid block at coords */
    public static void drawFluid(Fluid fluid, int x, int y, int width, int height) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(fluid.internal.get(0).getAttributes().getStillTexture());
        drawSprite(sprite, fluid.internal.get(0).getAttributes().getColor(), x, y, width, height);
    }

    /** Draw a texture sprite at coords, tinted with col  */
    private static void drawSprite(TextureAtlasSprite sprite, int col, int x, int y, int width, int height) {
        double zLevel = 0;

        try (With ctx = RenderContext.apply(
                new RenderState()
                        .texture(Texture.wrap(new Identifier(TextureAtlas.LOCATION_BLOCKS)))
                        .color((col >> 16 & 255) / 255.0f, (col >> 8 & 255) / 255.0f, (col & 255) / 255.0f, 1)
        )) {
            int iW = sprite.getWidth();
            int iH = sprite.getHeight();

            float minU = sprite.getU0();
            float minV = sprite.getV0();


            Tesselator tessellator = Tesselator.getInstance();
            BufferBuilder buffer = tessellator.getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            for (int offY = 0; offY < height; offY += iH) {
                double curHeight = Math.min(iH, height - offY);
                float maxVScaled = sprite.getV(16.0 * curHeight / iH);
                for (int offX = 0; offX < width; offX += iW) {
                    double curWidth = Math.min(iW, width - offX);
                    float maxUScaled = sprite.getU(16.0 * curWidth / iW);
                    buffer.vertex(x + offX, y + offY, zLevel).uv(minU, minV).endVertex();
                    buffer.vertex(x + offX, y + offY + curHeight, zLevel).uv(minU, maxVScaled).endVertex();
                    buffer.vertex(x + offX + curWidth, y + offY + curHeight, zLevel).uv(maxUScaled, maxVScaled).endVertex();
                    buffer.vertex(x + offX + curWidth, y + offY, zLevel).uv(maxUScaled, minV).endVertex();
                }
            }
            tessellator.end();
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
        drawCenteredString(text, x, y, color, new Matrix4());
    }
    public static void drawCenteredString(String text, int x, int y, int color, Matrix4 matrix) {
        RenderState state = new RenderState().color(1, 1, 1, 1).alpha_test(true);
        state.model_view().multiply(matrix);
        try (With ctx = RenderContext.apply(state)) {
            Minecraft.getInstance().font.draw(new PoseStack(), text, (float) (x - Minecraft.getInstance().font.width(text) / 2), (float) y, color);
        }
    }

    /** Screen Width in pixels (std coords) */
    public static int getScreenWidth() {
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    /** Screen Height in pixels (std coords) */
    public static int getScreenHeight() {
        return Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }

    /** Draw a Item at the given coords */
    public static void drawItem(ItemStack stack, int x, int y) {
        drawItem(stack, x, y, new Matrix4());
    }

    public static void drawItem(ItemStack stack, int x, int y, Matrix4 matrix) {
        RenderState state = new RenderState()
                .color(1, 1, 1, 1)
                .alpha_test(false)
                .blend(new BlendMode(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA))
                .rescale_normal(true);
        state.model_view().multiply(matrix);
        try (With ctx = RenderContext.apply(state)) {
            Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(stack.internal, x, y);
        }
    }
}
