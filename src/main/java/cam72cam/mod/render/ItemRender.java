package cam72cam.mod.render;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.gui.Progress;
import cam72cam.mod.item.ItemBase;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.world.World;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.TextureStitchEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class ItemRender {
    private static final List<Runnable> registers = new ArrayList<>();
    private static final List<Consumer<TextureStitchEvent.Pre>> textures = new ArrayList<>();
    private static final SpriteSheet iconSheet = new SpriteSheet(128);
    private static Map<ItemBase, IIcon> icons = new HashMap<>();

    public static void registerItems() {
        registers.forEach(Runnable::run);
    }

    public static IIcon getIcon(ItemBase itemBase) {
        return icons.get(itemBase);
    }


    public static class EventBus {
        @SubscribeEvent(priority = EventPriority.LOW)
        public void onTextureStich(TextureStitchEvent.Pre event) {
            textures.forEach(texture -> texture.accept(event));
        }
    }

    public static void register(ItemBase item, Identifier tex) {
        textures.add(event -> icons.put(item, Minecraft.getMinecraft().getTextureMapBlocks().registerIcon(tex.internal.toString())));
    }

    public static void register(ItemBase item, BiFunction<ItemStack, World, StandardModel> model) {
        register(item, model, null);
    }

    public static void register(ItemBase item, BiFunction<ItemStack, World, StandardModel> model, Function<ItemStack, Pair<String, StandardModel>> cacheRender) {
        registers.add(() ->
                MinecraftForgeClient.registerItemRenderer(item.internal, new BakedItemModel(model, cacheRender))
        );

        if (cacheRender != null) {
            textures.add((event) -> {
                List<ItemStack> variants = item.getItemVariants(null);
                Progress.Bar bar = Progress.push(item.getClass().getSimpleName() + " Icon", variants.size());
                for (ItemStack stack : variants) {
                    Pair<String, StandardModel> info = cacheRender.apply(stack);
                    bar.step(info.getKey());
                    createSprite(info.getKey(), info.getValue());
                }
                Progress.pop(bar);
            });
        }
    }

    private static void createSprite(String id, StandardModel model) {
        int width = iconSheet.spriteSize;
        int height = iconSheet.spriteSize;
        Framebuffer fb = new Framebuffer(width, height, true);
        fb.setFramebufferColor(0, 0, 0, 0);
        fb.framebufferClear();
        fb.bindFramebuffer(true);

        GLBoolTracker depth = new GLBoolTracker(GL11.GL_DEPTH_TEST, true);
        GL11.glDepthFunc(GL11.GL_LESS);
        GL11.glClearDepth(1);

        model.renderCustom();

        ByteBuffer buff = ByteBuffer.allocateDirect(4 * width * height);
        GL11.glReadPixels(0, 0, width, height, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, buff);

        fb.unbindFramebuffer();
        fb.deleteFramebuffer();
        depth.restore();

        iconSheet.setSprite(id, buff);
    }

    static class BakedItemModel implements IItemRenderer {
        private final BiFunction<ItemStack, World, StandardModel> model;
        private final Function<ItemStack, Pair<String, StandardModel>> cacheRender;

        BakedItemModel(BiFunction<ItemStack, World, StandardModel> model, Function<ItemStack, Pair<String, StandardModel>> cacheRender) {
            this.model = model;
            this.cacheRender = cacheRender;
        }


        @Override
        public void renderItem(ItemRenderType type, net.minecraft.item.ItemStack itemstack, Object... data) {
            /* TODO 1.7.10
            if (type == ItemCameraTransforms.TransformType.FIXED) {
                return new ItemTransformVec3f(new Vector3f(0, 90, 0), new Vector3f(0, 0, 0), new Vector3f(1, 1, 1));
            }
            if (type == ItemCameraTransforms.TransformType.HEAD) {
                return new ItemTransformVec3f(new Vector3f(0, 0, 0), new Vector3f(0, 1, 0), new Vector3f(2, 2, 2));
            }
             */


            ItemStack stack = new ItemStack(itemstack);
            switch (type) {
                case INVENTORY:
                    iconSheet.renderSprite(cacheRender.apply(stack).getKey());
                    return;
                case ENTITY:
                case EQUIPPED:
                case EQUIPPED_FIRST_PERSON:
                    StandardModel std = model.apply(stack, MinecraftClient.getPlayer().getWorld());
                    std.renderCustom();
                    break;
                case FIRST_PERSON_MAP:
                    break;
            }
        }

        @Override
        public boolean handleRenderType(net.minecraft.item.ItemStack item, ItemRenderType type) {
            return true;
        }

        @Override
        public boolean shouldUseRenderHelper(ItemRenderType type, net.minecraft.item.ItemStack item, ItemRendererHelper helper) {
            return false;
        }
    }
}
