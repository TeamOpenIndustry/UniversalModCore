package cam72cam.mod.render;

import cam72cam.mod.Config;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.ModCore;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.gui.Progress;
import cam72cam.mod.item.CustomItem;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.util.With;
import cam72cam.mod.world.World;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.tileentity.ItemStackTileEntityRenderer;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Direction;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.*;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Item Render Registry (Here be dragons...) */
public class ItemRender {
    private static final List<BakedQuad> EMPTY = Collections.emptyList();
    private static final SpriteSheet iconSheet = new SpriteSheet(Config.SpriteSize);

    /** Register a simple image for an item */
    public static void register(CustomItem item, Identifier tex) {
        SimpleModelTransform foo = new SimpleModelTransform(ImmutableMap.of());

        ClientEvents.MODEL_BAKE.subscribe(event -> event.getModelRegistry().put(new ModelResourceLocation(item.getRegistryName().internal, ""), new ItemLayerModel(ImmutableList.of(
                new RenderMaterial(AtlasTexture.LOCATION_BLOCKS, tex.internal)
        )).bake(new IModelConfiguration() {
            @Nullable
            @Override
            public IUnbakedModel getOwnerModel() {
                return null;
            }

            @Override
            public String getModelName() {
                return null;
            }

            @Override
            public boolean isTexturePresent(String name) {
                return false;
            }

            @Override
            public RenderMaterial resolveTexture(String name) {
                return null;
            }

            @Override
            public boolean isShadedInGui() {
                return false;
            }

            @Override
            public boolean isSideLit() {
                return false;
            }

            @Override
            public boolean useSmoothLighting() {
                return false;
            }

            @Override
            public ItemCameraTransforms getCameraTransforms() {
                return ItemCameraTransforms.NO_TRANSFORMS;
            }

            @Override
            public IModelTransform getCombinedTransform() {
                return new SimpleModelTransform(PerspectiveMapWrapper.getTransforms(getCameraTransforms()));
            }
        }, event.getModelLoader(), ModelLoader.defaultTextureGetter(), foo, ItemOverrideList.EMPTY, tex.internal)));

        ClientEvents.TEXTURE_STITCH.subscribe(evt -> evt.addSprite(tex.internal));

        ClientEvents.MODEL_CREATE.subscribe(() -> Minecraft.getInstance().getItemRenderer().getItemModelShaper().register(item.internal, new ModelResourceLocation(item.getRegistryName().internal, "")));
    }

    /** Register a complex model for an item */
    public static void register(CustomItem item, IItemModel model) {
        // Link Item to Item Registry Name
        ClientEvents.MODEL_CREATE.subscribe(() -> Minecraft.getInstance().getItemRenderer().getItemModelShaper().register(item.internal, new ModelResourceLocation(item.getRegistryName().internal, "")));

        // Link Item Registry Name to Custom Model
        ClientEvents.MODEL_BAKE.subscribe((ModelBakeEvent event) -> event.getModelRegistry().put(new ModelResourceLocation(item.getRegistryName().internal, ""), new BakedItemModel(model)));

        // Hook up Sprite Support (and generation)
        if (model instanceof ISpriteItemModel) {
            ClientEvents.TEXTURE_STITCH.subscribe(() -> {
                List<ItemStack> variants = item.getItemVariants(null);
                Progress.Bar bar = Progress.push(item.getClass().getSimpleName() + " Icon", variants.size());
                for (ItemStack stack : variants) {
                    Identifier id = ((ISpriteItemModel) model).getSpriteKey(stack);
                    bar.step(id.toString());
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
        default void applyTransform(ItemStack stack, ItemRenderType type, RenderState ctx) {
            defaultTransform(type, ctx);
        }
        static void defaultTransform(ItemRenderType type, RenderState state) {
            switch (type) {
                case FRAME:
                    state.rotate(90, 0, 1, 0);
                    state.translate(-0.9, 0, 0);
                    break;
                case HEAD:
                    state.translate(-0.5, 1, 0);
                    state.scale(2, 2, 2);
                    break;
            }
        }
    }

    /** Support for turning a custom model into a sprite */
    public interface ISpriteItemModel extends IItemModel {
        /** Unique string to represent this stack */
        Identifier getSpriteKey(ItemStack stack);
        /** Model that should be rendered as a sprite */
        StandardModel getSpriteModel(ItemStack stack);
    }

    /** Internal method to render a model to a framebuffer and drop it in the texture sheet */
    private static void createSprite(Identifier id, StandardModel model) {
        int width = iconSheet.spriteSize;
        int height = iconSheet.spriteSize;
        File sprite = ModCore.cacheFile(new Identifier(id.getDomain(),id.getPath() + "_sprite" + iconSheet.spriteSize + ".raw"));
        if (sprite.exists()) {
            try {
                ByteBuffer buff = GLAllocation.createByteBuffer(4 * width * height);
                buff.put(ByteBuffer.wrap(Files.readAllBytes(sprite.toPath())));
                buff.flip();
                iconSheet.setSprite(id, buff);
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        With restore = OptiFine.overrideFastRender(false);

        Framebuffer fb = new Framebuffer(width, height, true, true);
        fb.setClearColor(0, 0, 0, 0);
        fb.clear(Minecraft.ON_OSX);
        fb.bindWrite(true);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        boolean depthEnabled = GL11.glGetBoolean(GL11.GL_DEPTH_TEST);
        int oldDepth = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LESS);
        GL11.glClearDepth(1);

        model.renderCustom(new RenderState());

        fb.bindRead();
        ByteBuffer buff = ByteBuffer.allocateDirect(4 * width * height);
        GL11.glReadPixels(0, 0, width, height, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, buff);
        fb.unbindRead();

        fb.unbindWrite();
        fb.destroyBuffers();

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

        if (!depthEnabled) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }
        GL11.glDepthFunc(oldDepth);

        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPopMatrix();

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();

        restore.close();
    }

    static BiConsumer<MatrixStack, Integer> doRender = (s, i) -> {};
    public static Callable<ItemStackTileEntityRenderer> ISTER() {
        return () -> new ItemStackTileEntityRenderer() {
            @Override
            public void renderByItem(net.minecraft.item.ItemStack stack, TransformType p_239207_2_, MatrixStack matrixStack, IRenderTypeBuffer buffer, int combinedLight, int combinedOverlay) {
                doRender.accept(matrixStack, combinedLight);
            }
        };
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
            return EMPTY;
        }

        @Override
        public boolean useAmbientOcclusion() {
            return true;
        }

        @Override
        public boolean isGui3d() {
            return true;
        }

        @Override
        public boolean usesBlockLight() {
            return false;
        }

        @Override
        public boolean isCustomRenderer() {
            return true;
        }

        @Override
        public TextureAtlasSprite getParticleIcon() {
            return null;
        }

        @Override
        public ItemOverrideList getOverrides() {
            return new ItemOverrideListHack();
        }

        @Override
        public IBakedModel handlePerspective(ItemCameraTransforms.TransformType cameraTransformType, MatrixStack mat) {
            this.type = ItemRenderType.from(cameraTransformType);

            doRender = (matrix, i) -> {
                if (stack == null) {
                    return;
                }

                if (type == ItemRenderType.GUI && model instanceof ISpriteItemModel) {
                    iconSheet.renderSprite(((ISpriteItemModel) model).getSpriteKey(stack), new RenderState(matrix));
                    return ;
                }

                StandardModel std = model.getModel(MinecraftClient.getPlayer().getWorld(), stack);
                if (std == null) {
                    return ;
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
                if (!ModCore.isInReload()) {
                    RenderType.solid().setupRenderState();

                    mat.pushPose();
                    // Maybe backwards?
                    mat.last().pose().multiply(matrix.last().pose());

                    RenderState state = new RenderState(mat);
                    model.applyTransform(stack, type, state);

                    int j = i % 65536;
                    int k = i / 65536;
                    state.lightmap(j/240f, k/240f);

                    //std.renderCustom();
                    std.render(state);

                    mat.popPose();

                    RenderType.solid().clearRenderState();
                }
                // TODO return std.getQuads(side, rand);
            };


            return ForgeHooksClient.handlePerspective(this, cameraTransformType, mat);
        }

        class ItemOverrideListHack extends ItemOverrideList {
            ItemOverrideListHack() {
                super();
            }

            @Override
            public IBakedModel resolve(IBakedModel model, net.minecraft.item.ItemStack stack, @Nullable ClientWorld worldIn, @Nullable LivingEntity entityIn) {
                BakedItemModel.this.stack = new ItemStack(stack);
                return BakedItemModel.this;
            }
        }
    }
}
