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
import cpw.mods.fml.client.config.GuiUtils;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.lwjgl.opengl.GL11;
import util.Matrix4;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.BiConsumer;

/** Common GUI functions that don't really fit anywhere else */
public class GUIHelpers {
    /** Standard 54 slot chest UI */
    public static final Identifier CHEST_GUI_TEXTURE = new Identifier("textures/gui/container/generic_54.png");
    /**
     * Assuming that we're on a single-thread model
     * Internal function, don't use
     */
    private static final Deque<Map<String, BiConsumer<Integer, Integer>>> delayedRenderFunctions = new ArrayDeque<>();
    // Internal hack for using Gui functions
    private static final Gui instance = new Gui();

    /** Draw a solid color block */
    public static void drawRect(int x, int y, int width, int height, int color) {
        try (With ctx = RenderContext.apply(
                new RenderState()
                        .color(1, 1, 1, 1)
                        .texture(Texture.NO_TEXTURE)
                        .blend(new BlendMode(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA))
                        .stage(RenderContext.Stage.GUI)
        )) {
            Gui.drawRect(x, y, x + width, y + height, color);
        }
    }

    /** Draw a full image (tex) at coords with given width/height */
    public static void texturedRect(Identifier tex, int x, int y, int width, int height) {
        try (With ctx = RenderContext.apply(
                new RenderState().texture(Texture.wrap(tex)).stage(RenderContext.Stage.GUI)
        )) {
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

        try (With ctx = RenderContext.apply(
                new RenderState()
                        .texture(Texture.wrap(new Identifier(TextureMap.locationBlocksTexture)))
                        .color((col >> 16 & 255) / 255.0f, (col >> 8 & 255) / 255.0f, (col & 255) / 255.0f, 1)
                        .stage(RenderContext.Stage.GUI)
        )) {
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
        drawCenteredString(text, x, y, color, new Matrix4());
    }
    public static void drawCenteredString(String text, int x, int y, int color, Matrix4 matrix) {
        drawString(text, x - Minecraft.getMinecraft().fontRendererObj.getStringWidth(text) / 2, y, color, matrix);
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
            GL11.glColor4f(1, 1, 1, 0);
            Minecraft.getMinecraft().fontRendererObj.drawString(text, x, y, color);
        }
    }

    /** Gat a string's internal width for further use */
    public static int getTextWidth(String text) {
        return Minecraft.getMinecraft().fontRendererObj.getStringWidth(text);
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
        drawItem(stack, x, y, new Matrix4());
    }

    private static RenderBlocks renderBlocks = new RenderBlocks();
    public static void drawItem(ItemStack stack, int x, int y, Matrix4 matrix) {
        if (stack.isEmpty()) {
            return;
        }
        RenderState state = new RenderState()
                .color(1, 1, 1, 1)
                .alpha_test(true)
                .blend(new BlendMode(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA))
                .rescale_normal(true)
                .translate(x, y, 0);
        //If it's handled by us then it'll be set to ITEM_IN_GUI later
        //Otherwise we don't care
//              .stage(RenderContext.Stage.GUI);

        state.model_view().multiply(matrix);
        try (With ctx = RenderContext.apply(state)) {
            IItemRenderer renderer = MinecraftForgeClient.getItemRenderer(stack.internal, IItemRenderer.ItemRenderType.INVENTORY);
            if (renderer != null) {
                renderer.renderItem(IItemRenderer.ItemRenderType.INVENTORY, stack.internal);
            } else {
                // This is broken but does not crash...
                Block block = Block.getBlockFromItem(stack.internal.getItem());
                if (block != null) {
                    renderBlocks.renderBlockAsItem(block, stack.internal.getMetadata(), 1.0f);
                }
            }
        }
    }

    /** Try to open an external link in player's browser */
    public static void openLink(String url){
        URI uri = null;
        try {
            uri = new URI(url);
            if (!GuiChat.supportedProtocols.contains(uri.getScheme().toLowerCase()))
            {
                throw new URISyntaxException(url, "Unsupported protocol: " + uri.getScheme().toLowerCase());
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        if (Minecraft.getMinecraft().currentScreen != null) {
            openLinkInternal(uri);
        } else {
            ModCore.error("Trying to open a link outside a screen: %s", url);
            if (MinecraftClient.isReady() && MinecraftClient.getPlayer() != null) {
                MinecraftClient.getPlayer().sendMessage(PlayerMessage.url(url));
            }
        }
    }

    /** Try to open an external link in player's browser */
    public static void openFile(String path){
        if (Minecraft.getMinecraft().currentScreen != null) {
            openLinkInternal((new File(path)).toURI());
        } else {
            ModCore.error("Trying to open a file outside a screen: %s", path);
            if (MinecraftClient.isReady() && MinecraftClient.getPlayer() != null) {
                MinecraftClient.getPlayer().sendMessage(PlayerMessage.direct("Please check this location on your computer: " + path));
            }
        }
    }

    private static void openLinkInternal(URI p_146407_1_)
    {
        try {
            Class<?> oclass = Class.forName("java.awt.Desktop");
            Object object = oclass.getMethod("getDesktop", new Class[0]).invoke(null);
            oclass.getMethod("browse", new Class[] {URI.class}).invoke(object, p_146407_1_);
        } catch (Throwable throwable) {
            ModCore.error("Couldn't open link", throwable);
        }
    }

    /**
     * Draw a Minecraft-style tooltip at cursor's pos
     * Only use in IScreen.draw()!
     * */
    public static void drawTooltipAtCursor(List<String> content) {
        if (delayedRenderFunctions.peek() != null && Minecraft.getMinecraft().currentScreen instanceof GuiScreen) {
            //Use map to ensure only 1 tooltip is drawn
            delayedRenderFunctions.peek().put("tooltip", (x, y) ->{
                int width = getScreenWidth();
                int height = getScreenHeight();
                ((GuiScreen)Minecraft.getMinecraft().currentScreen).drawHoveringText(content, x, y);
            });
        } else {
            ModCore.error("Trying to call drawTooltipAtCursor outside any IScreen.draw(), which isn't allowed!");
        }
    }

    /** Internal */
    public static void initDelayed() {
        delayedRenderFunctions.push(new HashMap<>(4));
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
