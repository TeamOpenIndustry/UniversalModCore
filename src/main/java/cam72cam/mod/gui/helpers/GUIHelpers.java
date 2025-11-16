package cam72cam.mod.gui.helpers;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.ModCore;
import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.render.opengl.BlendMode;
import cam72cam.mod.render.opengl.RenderContext;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.util.With;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL32;
import util.Matrix4;

/** Common GUI functions that don't really fit anywhere else */
public class GUIHelpers {
    /** Standard 54 slot chest UI */
    public static final Identifier CHEST_GUI_TEXTURE = new Identifier("minecraft",
                                                                      "textures/gui/container/generic_54.png");
    //Initial value
    public static GuiGraphics graphics
            = new GuiGraphics(Minecraft.getInstance(), Minecraft.getInstance().gameRenderer.renderBuffers.bufferSource());

    /** Draw a solid color block */
    public static void drawRect(int x, int y, int width, int height, int color) {
//        try (With ctx = RenderContext.apply(
//                new RenderState()
//                        .color(1, 1, 1, 1)
//                        .texture(Texture.NO_TEXTURE)
//                        .blend(new BlendMode(BlendMode.GL_SRC_ALPHA, BlendMode.GL_ONE_MINUS_SRC_ALPHA))
//        )) {
            graphics.fill(x, y, x + width, y + height, color);
//        }
    }

    /** Draw a full image (tex) at coords with given width/height */
    public static void texturedRect(Identifier tex, int x, int y, int width, int height) {
        // X Y, U V, UW VH, W H, TW TH
        // AbstractGui.blit(x, y, 0, 0, 1, 1, width, height, 1, 1);
        // X Y, W H, U V, UW VH, TW TH
        RenderSystem.setShaderTexture(0, tex.internal);
        graphics.blit(tex.internal, x, y, width, height, 0, 0, 1, 1, 1, 1);
    }

    /** Draw fluid block at coords */
    public static void drawFluid(Fluid fluid, int x, int y, int width, int height) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(IClientFluidTypeExtensions.of(fluid.internal.get(0)).getStillTexture());
        drawSprite(sprite, IClientFluidTypeExtensions.of(fluid.internal.get(0)).getTintColor(), x, y, width, height);
    }

    /** Draw a texture sprite at coords, tinted with col  */
    private static void drawSprite(TextureAtlasSprite sprite, int col, int x, int y, int width, int height) {
        double zLevel = 0;

        float[] oldColor = RenderSystem.getShaderColor();
        ShaderInstance oldShader = RenderSystem.getShader();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        int iW = sprite.contents().width();
        int iH = sprite.contents().height();

        float minU = sprite.getU0();
        float minV = sprite.getV0();

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int offY = 0; offY < height; offY += iH) {
            double curHeight = Math.min(iH, height - offY);
            float maxVScaled = sprite.getV((float) (16.0 * curHeight / iH));
            for (int offX = 0; offX < width; offX += iW) {
                double curWidth = Math.min(iW, width - offX);
                float maxUScaled = sprite.getU((float) (16.0 * curWidth / iW));
                buffer.vertex(x + offX, y + offY, zLevel).uv(minU, minV).color((col >> 16 & 255) / 255.0f, (col >> 8 & 255) / 255.0f, (col & 255) / 255.0f, 1).endVertex();
                buffer.vertex(x + offX, y + offY + curHeight, zLevel).uv(minU, maxVScaled).color((col >> 16 & 255) / 255.0f, (col >> 8 & 255) / 255.0f, (col & 255) / 255.0f, 1).endVertex();
                buffer.vertex(x + offX + curWidth, y + offY + curHeight, zLevel).uv(maxUScaled, maxVScaled).color((col >> 16 & 255) / 255.0f, (col >> 8 & 255) / 255.0f, (col & 255) / 255.0f, 1).endVertex();
                buffer.vertex(x + offX + curWidth, y + offY, zLevel).uv(maxUScaled, minV).color((col >> 16 & 255) / 255.0f, (col >> 8 & 255) / 255.0f, (col & 255) / 255.0f, 1).endVertex();
            }
        }
        tessellator.end();

        RenderSystem.setShader(() -> oldShader);
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

    /** Draw a left-aligned shadowed string */
    public static void drawString(String text, int x, int y, int color) {
        drawString(text, x, y, color, new Matrix4());
    }
    public static void drawString(String text, int x, int y, int color, Matrix4 matrix) {
        RenderState state = new RenderState().color(1, 1, 1, 1).alpha_test(true);
        state.model_view().multiply(matrix);
        matrix.m23 = 10;//Z transform
        graphics.pose().pushPose();
        graphics.pose().setIdentity();
        graphics.pose().mulPoseMatrix(new Matrix4f(
                (float) matrix.m00,
                (float) matrix.m01,
                (float) matrix.m02,
                (float) matrix.m03,
                (float) matrix.m10,
                (float) matrix.m11,
                (float) matrix.m12,
                (float) matrix.m13,
                (float) matrix.m20,
                (float) matrix.m21,
                (float) matrix.m22,
                (float) matrix.m23,
                (float) matrix.m30,
                (float) matrix.m31,
                (float) matrix.m32,
                (float) matrix.m33
        ));
        int xPos = (int) (x + matrix.m03 / matrix.m00);
        int yPos = (int) (y + matrix.m13 / matrix.m11);
        graphics.drawString(Minecraft.getInstance().font, text, xPos, yPos, color);
        graphics.pose().popPose();
    }

    /** Draw a shadowed string offset from the center of coords */
    public static void drawCenteredString(String text, int x, int y, int color) {
        drawCenteredString(text, x, y, color, new Matrix4());
    }
    public static void drawCenteredString(String text, int x, int y, int color, Matrix4 matrix) {
        RenderState state = new RenderState().color(1, 1, 1, 1).alpha_test(true);
        state.model_view().multiply(matrix);
        matrix.m23 = 0;//Z transform
        graphics.pose().pushPose();
        graphics.pose().setIdentity();
        graphics.pose().mulPoseMatrix(new Matrix4f(
                (float) matrix.m00,
                (float) matrix.m01,
                (float) matrix.m02,
                (float) matrix.m03,
                (float) matrix.m10,
                (float) matrix.m11,
                (float) matrix.m12,
                (float) matrix.m13,
                (float) matrix.m20,
                (float) matrix.m21,
                (float) matrix.m22,
                (float) matrix.m23,
                (float) matrix.m30,
                (float) matrix.m31,
                (float) matrix.m32,
                (float) matrix.m33
        ));
        int xPos = (int) (x + matrix.m03 / matrix.m00);
        int yPos = (int) (y + matrix.m13 / matrix.m11);
        graphics.drawCenteredString(Minecraft.getInstance().font, text, xPos, yPos, color);
        graphics.pose().popPose();
    }

    /** Gat a string's internal width for further use */
    public static int getTextWidth(String text) {
        return Minecraft.getInstance().font.width(text);
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
                .blend(new BlendMode(GL32.GL_SRC_ALPHA, GL32.GL_ONE_MINUS_SRC_ALPHA))
                .rescale_normal(true);
        state.model_view().multiply(matrix);
        try (With ctx = RenderContext.apply(state)) {
            graphics.renderItem(stack.internal(), x, y);
        }
    }

    /** Try to open an external link in player's browser */
    public static void openLink(String url){
        MutableComponent component = Component.literal("");
        component.setStyle(component.getStyle().withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url)));
        if (Minecraft.getInstance().screen != null) {
            Minecraft.getInstance().screen.handleComponentClicked(component.getStyle());
        } else {
            ModCore.error("Trying to open a link outside a screen: %s", url);
            if (MinecraftClient.isReady() && MinecraftClient.getPlayer() != null) {
                MinecraftClient.getPlayer().sendMessage(PlayerMessage.url(url));
            }
        }
    }

    /** Try to open an external link in player's browser */
    public static void openFile(String path){
        MutableComponent component = Component.literal("");
        component.setStyle(component.getStyle().withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, path)));
        if (Minecraft.getInstance().screen != null) {
            Minecraft.getInstance().screen.handleComponentClicked(component.getStyle());
        } else {
            ModCore.error("Trying to open a file outside a screen: %s", path);
            if (MinecraftClient.isReady() && MinecraftClient.getPlayer() != null) {
                MinecraftClient.getPlayer().sendMessage(PlayerMessage.direct("Please check this location on your computer: " + path));
            }
        }
    }
}