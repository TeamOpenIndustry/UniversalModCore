package cam72cam.mod.gui.helpers;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.ModCore;
import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.mixin.accessor.ATextureAtlasSprite;
import cam72cam.mod.render.opengl.RenderContext;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.util.With;
import com.mojang.blaze3d.vertex.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import util.Matrix4;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/** Common GUI functions that don't really fit anywhere else */
public class GUIHelpers {
    /** Standard 54 slot chest UI */
    public static final Identifier CHEST_GUI_TEXTURE = new Identifier("minecraft", "textures/gui/container/generic_54.png");

    /**
     * Assuming that we're on a single-thread model
     * Internal function, don't use
     */
    private static final Deque<Map<String, BiConsumer<Integer, Integer>>> delayedRenderFunctions = new ArrayDeque<>();

    //Initial value
    public static GuiGraphics graphics
            = new GuiGraphics(Minecraft.getInstance(), Minecraft.getInstance().gameRenderer.guiRenderState);

    /** Draw a solid color block */
    public static void drawRect(int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + height, color);
    }

    /** Draw a full image (tex) at coords with given width/height */
    public static void texturedRect(Identifier tex, int x, int y, int width, int height) {
        // X Y, U V, UW VH, W H, TW TH
        // AbstractGui.blit(x, y, 0, 0, 1, 1, width, height, 1, 1);
        // X Y, W H, U V, UW VH, TW TH
        graphics.blit(RenderPipelines.GUI_TEXTURED, tex.internal, x, y, width, height, 0, 0, 1, 1, 1, 1);
    }

    /** Draw fluid block at coords */
    public static void drawFluid(Fluid fluid, int x, int y, int width, int height) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                                             .apply(IClientFluidTypeExtensions.of(fluid.internal.getFirst()).getStillTexture());
        drawSprite(sprite, IClientFluidTypeExtensions.of(fluid.internal.getFirst()).getTintColor(), x, y, width, height);
    }

    /** Draw a texture sprite at coords, tinted with col  */
    private static void drawSprite(TextureAtlasSprite sprite, int col, int x, int y, int width, int height) {
        int iW = sprite.contents().width();
        int iH = sprite.contents().height();

        for (int offY = 0; offY < height; offY += iH) {
            int curHeight = Math.min(iH, height - offY);
            for (int offX = 0; offX < width; offX += iW) {
                int curWidth = Math.min(iW, width - offX);
                ATextureAtlasSprite spriteAccessor = ATextureAtlasSprite.from(sprite);

                graphics.blit(RenderPipelines.GUI_TEXTURED, TextureAtlas.LOCATION_BLOCKS,
                              x + offX, y + offY, sprite.getX(), sprite.getY(), curWidth, curHeight, iW, iH,
                              spriteAccessor.getAtlasWidth(), spriteAccessor.getAtlasHeight());
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
//            drawRect(x, y, width, height, 0xFF000000);
        }

        if (percentFull > 0 && fluid != null) {
            int fullHeight = Math.max(1, (int) (height * percentFull));
            drawFluid(fluid, x, y + height - fullHeight, width, fullHeight);
//            drawRect(x, y + height - fullHeight, width, fullHeight, color);
        }
    }

    /** Draw a left-aligned shadowed string */
    public static void drawString(String text, int x, int y, int color) {
        drawString(text, x, y, color, new Matrix4());
    }
    public static void drawString(String text, int x, int y, int color, Matrix4 matrix) {
        RenderState state = new RenderState().color(1, 1, 1, 1).alpha_test(true).stage(RenderContext.Stage.GUI);
        //Reset Z to prevent culling
        matrix.m23 = 0;
        state.model_view().multiply(matrix);
        state.depth_test(false);
        try (With with = RenderContext.apply(state)) {
            Font font = Minecraft.getInstance().font;
            font.drawInBatch(
                    Component.literal(text), x, y, color, false, new Matrix4f(),
                    RenderContext.IMMEDIATE, Font.DisplayMode.SEE_THROUGH, 0, 15728880
            );
            RenderContext.IMMEDIATE.endBatch();
        }
    }

    /** Draw a shadowed string offset from the center of coords */
    public static void drawCenteredString(String text, int x, int y, int color) {
        drawCenteredString(text, x, y, color, new Matrix4());
    }
    public static void drawCenteredString(String text, int x, int y, int color, Matrix4 matrix) {
        RenderState state = new RenderState().color(1, 1, 1, 1).alpha_test(true).stage(RenderContext.Stage.GUI);
        //Reset Z to prevent culling
        matrix.m23 = 0;
        state.model_view().multiply(matrix);
        state.depth_test(false);
        try (With with = RenderContext.apply(state)) {
            Font font = Minecraft.getInstance().font;
            font.drawInBatch(
                    Component.literal(text), x - font.width(text) / 2f, y, color, false, new Matrix4f(),
                    RenderContext.IMMEDIATE, Font.DisplayMode.SEE_THROUGH, 0, 15728880
            );
            RenderContext.IMMEDIATE.endBatch();
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
        //TODO doesn't work properly with Z translations...should we restrict it?
        Matrix3x2f matrix3x2f = new Matrix3x2f();
        Matrix4f matrix4f = new Matrix4f();
        matrix3x2f.m00 = matrix4f.m00();
        matrix3x2f.m01 = matrix4f.m01();
        matrix3x2f.m10 = matrix4f.m10();
        matrix3x2f.m11 = matrix4f.m11();
        matrix3x2f.m20 = matrix4f.m30();
        matrix3x2f.m21 = matrix4f.m31();

        graphics.pose().pushMatrix().mul(matrix3x2f);
        graphics.renderItem(stack.internal(), x, y);
        graphics.pose().popMatrix();
    }

    /** Try to open an external link in player's browser */
    public static void openLink(String url){
        MutableComponent component = Component.literal("");
        try {
            component.setStyle(component.getStyle().withClickEvent(new ClickEvent.OpenUrl(new URI(url))));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
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
        component.setStyle(component.getStyle().withClickEvent(new ClickEvent.OpenFile(path)));
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
        if (delayedRenderFunctions.peek() != null && Minecraft.getInstance().screen != null) {
            //Use map to ensure only 1 tooltip is drawn
            delayedRenderFunctions.peek().put("tooltip", (x, y) ->{
                List<ClientTooltipComponent> components = content.stream()
                                                                 .map(Component::literal)
                                                                 .map(Component::getVisualOrderText)
                                                                 .map(ClientTooltipComponent::create)
                                                                 .collect(Collectors.toList());
                graphics.renderTooltip(Minecraft.getInstance().font, components, x, y,
                                       DefaultTooltipPositioner.INSTANCE, null);
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