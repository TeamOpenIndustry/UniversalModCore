package cam72cam.mod.render;

import cam72cam.mod.Config;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.ModCore;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.gui.Progress;
import cam72cam.mod.item.CustomItem;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.render.OpenGL.With;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.world.World;
import net.minecraft.client.shader.Framebuffer;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Item Render Registry (Here be dragons...) */
public class ItemRender {
    private static final SpriteSheet iconSheet = new SpriteSheet(Config.SpriteSize);
    private static Map<CustomItem, String> icons = new HashMap<>();

    public static String getIcon(CustomItem itemBase) {
        return icons.get(itemBase);
    }

    /** Register a simple image for an item */
    public static void register(CustomItem item, Identifier tex) {
        icons.put(item, tex.toString());
    }

    /** Register a complex model for an item */
    public static void register(CustomItem item, IItemModel model) {
        // Link Item to Item Registry Name
        ClientEvents.MODEL_CREATE.subscribe(() ->
                MinecraftForgeClient.registerItemRenderer(item.internal, new BakedItemModel(model))
        );

        // Hook up Sprite Support (and generation)
        if (model instanceof ISpriteItemModel) {
            ClientEvents.RELOAD.subscribe(() -> {
                List<ItemStack> variants = item.getItemVariants(null);
                Progress.Bar bar = Progress.push(item.getClass().getSimpleName() + " Icon", variants.size());
                for (ItemStack stack : variants) {
                    String id = ((ISpriteItemModel) model).getSpriteKey(stack);
                    bar.step(id);
                    createSprite(id, ((ISpriteItemModel) model).getSpriteModel(stack));
                }
                Progress.pop(bar);
            });
        }
    }

    /** Different contexts in which an item can be rendered */
    public enum ItemRenderType {
        NONE(null),
        THIRD_PERSON_LEFT_HAND(null),
        THIRD_PERSON_RIGHT_HAND(IItemRenderer.ItemRenderType.EQUIPPED),
        FIRST_PERSON_LEFT_HAND(null),
        FIRST_PERSON_RIGHT_HAND(IItemRenderer.ItemRenderType.EQUIPPED_FIRST_PERSON),
        HEAD(null),
        GUI(IItemRenderer.ItemRenderType.INVENTORY),
        ENTITY(IItemRenderer.ItemRenderType.ENTITY),
        FRAME(null); // Handled by ENTITY

        private final IItemRenderer.ItemRenderType type;

        ItemRenderType(IItemRenderer.ItemRenderType type) {
            this.type = type;
        }

        public static ItemRenderType from(IItemRenderer.ItemRenderType cameraTransformType) {
            if (cameraTransformType == null) {
                return NONE;
            }

            for (ItemRenderType type : values()) {
                if (cameraTransformType == type.type) {
                    return type;
                }
            }
            return NONE;
        }
    }

    /** Custom Item Model */
    @FunctionalInterface
    public interface IItemModel {
        /** Provide a model to render */
        StandardModel getModel(World world, ItemStack stack);

        /** Apply GL transformations based on the render context */
        default void applyTransform(ItemRenderType type) {
            defaultTransform(type);
        }
        static void defaultTransform(ItemRenderType type) {
            switch (type) {
                case FRAME:
                    GL11.glRotated(90, 0, 1, 0);
                    GL11.glTranslated(-0.9, 0, 0);
                    break;
                case HEAD:
                    GL11.glTranslated(-0.5, 1, 0);
                    GL11.glScaled(2, 2, 2);
                    break;
            }
        }
    }

    /** Support for turning a custom model into a sprite */
    public interface ISpriteItemModel extends IItemModel {
        /** Unique string to represent this stack */
        String getSpriteKey(ItemStack stack);
        /** Model that should be rendered as a sprite */
        StandardModel getSpriteModel(ItemStack stack);
    }

    /** Internal method to render a model to a framebuffer and drop it in the texture sheet */
    private static void createSprite(String id, StandardModel model) {
        int width = iconSheet.spriteSize;
        int height = iconSheet.spriteSize;
        File sprite = GLTexture.cacheFile(id.replace("/", ".") + "_" + "sprite" + iconSheet.spriteSize + ".raw");
        if (sprite.exists()) {
            try {
                ByteBuffer buff = ByteBuffer.allocateDirect(4 * width * height);
                buff.put(ByteBuffer.wrap(Files.readAllBytes(sprite.toPath())));
                buff.flip();
                iconSheet.setSprite(id, buff);
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Framebuffer fb = new Framebuffer(width, height, true);
        fb.setFramebufferColor(0, 0, 0, 0);
        fb.framebufferClear();
        fb.bindFramebuffer(true);

        try (With projection = OpenGL.matrix(GL11.GL_PROJECTION)) {
            GL11.glLoadIdentity();
            try (With modelM = OpenGL.matrix(GL11.GL_MODELVIEW)) {
                GL11.glLoadIdentity();
                try (With depth = OpenGL.bool(GL11.GL_DEPTH_TEST, true)) {
                    int oldDepth = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
                    GL11.glDepthFunc(GL11.GL_LESS);
                    GL11.glClearDepth(1);

                    model.renderCustom();

                    ByteBuffer buff = ByteBuffer.allocateDirect(4 * width * height);
                    GL11.glReadPixels(0, 0, width, height, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, buff);

                    fb.unbindFramebuffer();
                    fb.deleteFramebuffer();
                    GL11.glDepthFunc(oldDepth);

                    iconSheet.setSprite(id, buff);

                    try {
                        byte[] data = new byte[buff.capacity()];
                        buff.get(data);
                        Files.write(sprite.toPath(), data);
                    } catch (IOException e) {
                        ModCore.catching(e);
                    }
                }
            }
        }
    }

    /** Custom Model where we can hack into the MC/Forge render system */
    static class BakedItemModel implements IItemRenderer {
        private ItemStack stack;
        private final IItemModel model;

        BakedItemModel(IItemModel model) {
            this.model = model;
        }

        @Override
        public boolean handleRenderType(net.minecraft.item.ItemStack item, ItemRenderType type) {
            return true;
        }

        @Override
        public boolean shouldUseRenderHelper(ItemRenderType type, net.minecraft.item.ItemStack item, ItemRendererHelper helper) {
            return false;
        }

        @Override
        public void renderItem(ItemRenderType typeIn, net.minecraft.item.ItemStack item, Object... data) {
            ItemStack stack = new ItemStack(item);
            if (stack.isEmpty()) {
                return;
            }

            ItemRender.ItemRenderType type = ItemRender.ItemRenderType.from(typeIn);

            if (type == ItemRender.ItemRenderType.GUI && model instanceof ISpriteItemModel) {
                GL11.glPushMatrix();
                GL11.glRotated(180, 1, 0, 0);
                //GL11.glRotated(180, 0, 1, 0);
                GL11.glTranslated(-2, 2, 0);
                GL11.glScaled(20, 20, 20);
                GL11.glTranslated(0, -1, 0);
                GL11.glEnable(GL11.GL_ALPHA_TEST);
                iconSheet.renderSprite(((ISpriteItemModel) model).getSpriteKey(stack));
                GL11.glPopMatrix();
                return;
            }

            StandardModel std = model.getModel(MinecraftClient.getPlayer().getWorld(), stack);
            if (std == null) {
                return;
            }

            GL11.glPushMatrix();
            switch (type) {
                case GUI:
                    GL11.glRotated(180, 1, 0, 0);
                    GL11.glRotated(180, 0, 1, 0);
                    GL11.glScaled(16, 16, 16);
                    GL11.glTranslated(-1, -1, 0);
                    break;
                case FIRST_PERSON_RIGHT_HAND:
                    GL11.glRotated(90, 0, 1, 0);
                    GL11.glTranslated(1, 0, 0.7);
                    GL11.glRotated(180, 0, 1, 0);
                    GL11.glRotated(10, 1, 0, 0);
                    break;
                case THIRD_PERSON_RIGHT_HAND:
                    GL11.glTranslated(1, -0.5, 0.5);
                    GL11.glRotated(180, 0, 1, 0);
                    break;
                case ENTITY:
                    GL11.glTranslated(0.5, 0, 0.5);
                    GL11.glRotated(180, 0, 1, 0);
                    break;
            }

            model.applyTransform(type);
            std.render();
            GL11.glPopMatrix();
        }
    }
}
