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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Direction;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.ItemLayerModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.SimpleModelState;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Item Render Registry (Here be dragons...) */
public class ItemRender {
    private static final List<BakedQuad> EMPTY = Collections.emptyList();
    private static final SpriteSheet iconSheet = new SpriteSheet(Config.SpriteSize);

    /** Register a simple image for an item */
    public static void register(CustomItem item, Identifier tex) {
        ClientEvents.MODEL_BAKE.subscribe(event -> event.getModelRegistry().put(new ModelResourceLocation(item.getRegistryName().internal, ""), new ItemLayerModel(ImmutableList.of(
                tex.internal
        )).bake(event.getModelLoader(), ModelLoader.defaultTextureGetter(), new SimpleModelState(ImmutableMap.of()), DefaultVertexFormats.ITEM)));

        ClientEvents.TEXTURE_STITCH.subscribe(evt -> evt.addSprite(tex.internal));

        ClientEvents.MODEL_CREATE.subscribe(() -> Minecraft.getInstance().getItemRenderer().getItemModelMesher().register(item.internal, new ModelResourceLocation(item.getRegistryName().internal, "")));
    }

    /** Register a complex model for an item */
    public static void register(CustomItem item, IItemModel model) {
        // Link Item to Item Registry Name
        ClientEvents.MODEL_CREATE.subscribe(() -> Minecraft.getInstance().getItemRenderer().getItemModelMesher().register(item.internal, new ModelResourceLocation(item.getRegistryName().internal, "")));

        // Link Item Registry Name to Custom Model
        ClientEvents.MODEL_BAKE.subscribe((ModelBakeEvent event) -> event.getModelRegistry().put(new ModelResourceLocation(item.getRegistryName().internal, ""), new BakedItemModel(model)));

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
        NONE(TransformType.NONE),
        THIRD_PERSON_LEFT_HAND(TransformType.THIRD_PERSON_LEFT_HAND),
        THIRD_PERSON_RIGHT_HAND(TransformType.THIRD_PERSON_RIGHT_HAND),
        FIRST_PERSON_LEFT_HAND(TransformType.FIRST_PERSON_LEFT_HAND),
        FIRST_PERSON_RIGHT_HAND(TransformType.FIRST_PERSON_RIGHT_HAND),
        HEAD(TransformType.HEAD),
        GUI(TransformType.GUI),
        ENTITY(TransformType.GROUND),
        FRAME(TransformType.FIXED);

        private final TransformType type;

        ItemRenderType(TransformType type) {
            this.type = type;
        }

        public static ItemRenderType from(TransformType cameraTransformType) {
            for (ItemRenderType type : values()) {
                if (cameraTransformType == type.type) {
                    return type;
                }
            }
            return null;
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

        Framebuffer fb = new Framebuffer(width, height, true, true);
        fb.setFramebufferColor(0, 0, 0, 0);
        fb.framebufferClear(Minecraft.IS_RUNNING_ON_MAC);
        fb.bindFramebuffer(true);

        try (With projection = OpenGL.matrix(GL11.GL_PROJECTION)) {
            GL11.glLoadIdentity();
            try (With modelM = OpenGL.matrix(GL11.GL_MODELVIEW)) {
                GL11.glLoadIdentity();
                try (With depth = OpenGL.bool(GL11.GL_DEPTH_TEST, true); With alpha = OpenGL.bool(GL11.GL_ALPHA_TEST, true)) {

                    // GL COLOR MATERIAL = true

                    int oldDepth = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
                    GL11.glDepthFunc(GL11.GL_LESS);
                    GL11.glClearDepth(1);

                    model.renderCustom();

                    fb.bindFramebufferTexture();
                    ByteBuffer buff = ByteBuffer.allocateDirect(4 * width * height);
                    GL11.glReadPixels(0, 0, width, height, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, buff);
                    fb.unbindFramebufferTexture();

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
                        sprite.delete();
                    }
                }
            }
        }
    }

    /** Custom Model where we can hack into the MC/Forge render system */
    static class BakedItemModel implements IBakedModel {
        private ItemStack stack;
        private final IItemModel model;
        private ItemRenderType type;


        BakedItemModel(IItemModel model) {
            this.stack = null;
            this.model = model;
            this.type = ItemRenderType.NONE;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand) {
            if (stack == null) {
                return EMPTY;
            }

            if (type == ItemRenderType.GUI && model instanceof ISpriteItemModel) {
                iconSheet.renderSprite(((ISpriteItemModel) model).getSpriteKey(stack));
                return EMPTY;
            }

            StandardModel std = model.getModel(MinecraftClient.getPlayer().getWorld(), stack);
            if (std == null) {
                return EMPTY;
            }


            /*
             * I am an evil wizard!
             *
             * So it turns out that I can stick a draw call in here to
             * render my own stuff. This subverts forge's entire baked model
             * system with a single line of code and injects my own OpenGL
             * payload. Fuck you modeling restrictions.
             *
             * This is probably really fragile if someone calls getQuads
             * before actually setting up the correct GL context.
             */
            if (side == null && !ModCore.isInReload()) {
                model.applyTransform(type);
                std.renderCustom();
            }

            return std.getQuads(side, rand);
        }

        @Override
        public boolean isAmbientOcclusion() {
            return true;
        }

        @Override
        public boolean isGui3d() {
            return true;
        }

        @Override
        public boolean isBuiltInRenderer() {
            return false;
        }

        @Override
        public TextureAtlasSprite getParticleTexture() {
            return null;
        }

        @Override
        public ItemOverrideList getOverrides() {
            return new ItemOverrideListHack();
        }

        @Override
        public Pair<? extends IBakedModel, Matrix4f> handlePerspective(TransformType cameraTransformType) {
            this.type = ItemRenderType.from(cameraTransformType);
            return ForgeHooksClient.handlePerspective(this, cameraTransformType);
        }

        class ItemOverrideListHack extends ItemOverrideList {
            ItemOverrideListHack() {
                super();
            }

            @Override
            public IBakedModel getModelWithOverrides(IBakedModel model, net.minecraft.item.ItemStack stack, @Nullable net.minecraft.world.World worldIn, @Nullable LivingEntity entityIn) {
                BakedItemModel.this.stack = new ItemStack(stack);
                return BakedItemModel.this;
            }
        }
    }
}
