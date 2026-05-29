package cam72cam.mod.gui.helpers;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.ModCore;
import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.util.With;
import cam72cam.mod.render.opengl.BlendMode;
import cam72cam.mod.render.opengl.RenderContext;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.Texture;
import cam72cam.mod.resource.Identifier;
import com.mojang.blaze3d.matrix.MatrixStack;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.fml.client.gui.GuiUtils;
import org.lwjgl.opengl.GL11;
import util.Matrix4;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/** Common GUI functions that don't really fit anywhere else */
public class GUIHelpers {
    /** Standard 54 slot chest UI */
    public static final Identifier CHEST_GUI_TEXTURE = new Identifier("textures/gui/container/generic_54.png");
    /**
     * Assuming that we're on a single-thread model
     * Internal function, don't use
     */
    private static final Deque<Map<String, BiConsumer<Integer, Integer>>> delayedRenderFunctions = new ArrayDeque<>();

    /** Draw a solid color block */
    public static void drawRect(int x, int y, int width, int height, int color) {
        try (With ctx = RenderContext.apply(
                new RenderState()
                        .color(1, 1, 1, 1)
                        .texture(Texture.NO_TEXTURE)
                        .blend(new BlendMode(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA))
                        .stage(RenderContext.Stage.GUI)
        )) {
            AbstractGui.fill(new MatrixStack(), x, y, x + width, y + height, color);
        }
    }

    /** Draw a full image (tex) at coords with given width/height */
    public static void texturedRect(Identifier tex, int x, int y, int width, int height) {
        try (With ctx = RenderContext.apply(
                new RenderState().texture(Texture.wrap(tex)).stage(RenderContext.Stage.GUI)
        )) {
            // X Y, U V, UW VH, W H, TW TH
            // AbstractGui.blit(x, y, 0, 0, 1, 1, width, height, 1, 1);
            // X Y, W H, U V, UW VH, TW TH
            AbstractGui.blit(new MatrixStack(), x, y, width, height, 0, 0, 1, 1, 1, 1);
        }
    }

    /** Draw fluid block at coords */
    public static void drawFluid(Fluid fluid, int x, int y, int width, int height) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(AtlasTexture.LOCATION_BLOCKS).apply(fluid.internal.get(0).getAttributes().getStillTexture());
        drawSprite(sprite, fluid.internal.get(0).getAttributes().getColor(), x, y, width, height);
    }

    /** Draw a texture sprite at coords, tinted with col  */
    private static void drawSprite(TextureAtlasSprite sprite, int col, int x, int y, int width, int height) {
        double zLevel = 0;

        try (With ctx = RenderContext.apply(
                new RenderState()
                        .texture(Texture.wrap(new Identifier(AtlasTexture.LOCATION_BLOCKS)))
                        .color((col >> 16 & 255) / 255.0f, (col >> 8 & 255) / 255.0f, (col & 255) / 255.0f, 1)
                        .stage(RenderContext.Stage.GUI)
        )) {
            int iW = sprite.getWidth();
            int iH = sprite.getHeight();

            float minU = sprite.getU0();
            float minV = sprite.getV0();


            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuilder();
            buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
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

    /** Draw a left-aligned shadowed string */
    public static void drawString(String text, int x, int y, int color) {
        drawString(text, x, y, color, new Matrix4());
    }
    public static void drawString(String text, int x, int y, int color, Matrix4 matrix) {
        RenderState state = new RenderState().color(1, 1, 1, 1).alpha_test(true);
        state.model_view().multiply(matrix);
        state.stage(RenderContext.Stage.GUI);
        try (With ctx = RenderContext.apply(state)) {
            Minecraft.getInstance().font.draw(new MatrixStack(), text, x, y, color);
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
            Minecraft.getInstance().font.draw(new MatrixStack(), text, (float) (x - getTextWidth(text) / 2), (float) y, color);
        }
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
                .blend(new BlendMode(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA))
                .rescale_normal(true);
        //If it's handled by us then it'll be set to ITEM_IN_GUI later
        //Otherwise we don't care
//              .stage(RenderContext.Stage.GUI);
        state.model_view().multiply(matrix);
        RenderHelper.enableStandardItemLighting();
        try (With ctx = RenderContext.apply(state)) {
            Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(stack.internal, x, y);
        }
        RenderHelper.disableStandardItemLighting();
    }

    /** Try to open an external link in player's browser */
    public static void openLink(String url){
        StringTextComponent component = new StringTextComponent("");
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
        StringTextComponent component = new StringTextComponent("");
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

    /**
     * Draw a Minecraft-style tooltip at cursor's pos
     * Only use in IScreen.draw()!
     * */
    public static void drawTooltipAtCursor(List<String> content) {
        if (delayedRenderFunctions.peek() != null) {
            //Use map to ensure only 1 tooltip is drawn
            delayedRenderFunctions.peek().put("tooltip", (x, y) ->{
                int width = getScreenWidth();
                int height = getScreenHeight();
                List<ITextProperties> properties = content.stream()
                                                          .map(StringTextComponent::new)
                                                          .collect(Collectors.toList());
                GuiUtils.drawHoveringText(new MatrixStack(), properties, x, y, width, height, -1, Minecraft.getInstance().font);
            });
        } else {
            ModCore.error("Trying to call drawTooltipAtCursor outside any IScreen.draw(), which isn't allowed!");
        }
    }

    /** Internal */
    public static void initDelayed() {
        delayedRenderFunctions.push(new Object2ObjectArrayMap<>(4));
    }

    /** Internal */
    public static void runDelayed(int mouseX, int mouseY) {
        if (!delayedRenderFunctions.isEmpty()) {
            delayedRenderFunctions.pop().values().forEach(consumer -> consumer.accept(mouseX, mouseY));
        } else {
            ModCore.error("Trying to call runDelayed without initialized state!");
        }
    }
}
