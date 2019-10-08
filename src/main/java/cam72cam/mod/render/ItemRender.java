package cam72cam.mod.render;

import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.gui.Progress;
import cam72cam.mod.item.ItemBase;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.world.World;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.gl.GlFramebuffer;
import net.minecraft.client.render.model.*;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.render.model.json.ModelItemPropertyOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.render.model.json.Transformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.ExtendedBlockView;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class ItemRender {
    private static final List<BakedQuad> EMPTY = new ArrayList<>();
    private static final SpriteSheet iconSheet = new SpriteSheet(128);

    public static void register(ItemBase item, Identifier tex) {

        ClientEvents.MODEL_BAKE.subscribe(() -> {
            Map<String, String> textures = new HashMap<>();
            textures.put("layer0", tex.toString());
            ModelLoadingRegistry.INSTANCE.registerVariantProvider(resourceManager -> (modelId, context) -> item.getRegistryName().internal.equals(modelId) ?
                    new JsonUnbakedModel(new net.minecraft.util.Identifier("item/generated"), Collections.emptyList(), textures, true, true, ModelTransformation.NONE, Collections.emptyList())
                    : null);
        });
    }

    public static void register(ItemBase item, BiFunction<ItemStack, World, StandardModel> model) {
        register(item, model, null);
    }

    public static void register(ItemBase item, BiFunction<ItemStack, World, StandardModel> model, Function<ItemStack, Pair<String, StandardModel>> cacheRender) {

        ClientEvents.MODEL_BAKE.subscribe(() -> {
            ModelLoadingRegistry.INSTANCE.registerVariantProvider(manager -> (modelId, context) -> item.getRegistryName().internal.equals(modelId) ?
                    new UnbakedModel() {
                        @Override
                        public Collection<net.minecraft.util.Identifier> getModelDependencies() {
                            return Collections.emptyList();
                        }

                        @Override
                        public Collection<net.minecraft.util.Identifier> getTextureDependencies(Function<net.minecraft.util.Identifier, UnbakedModel> var1, Set<String> var2) {
                            return Collections.emptyList();
                        }

                        @Nullable
                        @Override
                        public BakedModel bake(ModelLoader var1, Function<net.minecraft.util.Identifier, Sprite> var2, ModelBakeSettings var3) {
                            ModelItemPropertyOverrideList overrides = new ModelItemPropertyOverrideList(var1, null, id -> null, Collections.emptyList()) {
                                @Override
                                public BakedModel apply(BakedModel bakedModel_1, net.minecraft.item.ItemStack itemStack_1, @Nullable net.minecraft.world.World world_1, @Nullable LivingEntity livingEntity_1) {
                                    return new BakedItemModel(new ItemStack(itemStack_1), World.get(world_1), model, cacheRender, ((BakedItemModel) bakedModel_1).isGUI, ((BakedItemModel) bakedModel_1).overrides);
                                }

                            };
                            return new BakedItemModel(model, cacheRender, overrides);
                        }
                    } : null);
        });

        if (cacheRender != null) {
            ClientEvents.TEXTURE_STITCH.subscribe(() -> {
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
        GlFramebuffer fb = new GlFramebuffer(width, height, true, true);
        fb.setClearColor(0, 0, 0, 0);
        fb.clear(true);
        fb.beginWrite(true);

        GLBoolTracker depth = new GLBoolTracker(GL11.GL_DEPTH_TEST, true);
        GL11.glDepthFunc(GL11.GL_LESS);
        GL11.glClearDepth(1);

        model.renderCustom();

        ByteBuffer buff = ByteBuffer.allocateDirect(4 * width * height);
        fb.beginRead();
        GL11.glReadPixels(0, 0, width, height, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, buff);
        fb.endRead();

        fb.endWrite();
        fb.delete();
        depth.restore();

        iconSheet.setSprite(id, buff);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
    }

    static class BakedItemModel implements FullBakedModel {
        private final ItemStack stack;
        private final World world;
        private final BiFunction<ItemStack, World, StandardModel> model;
        private final Function<ItemStack, Pair<String, StandardModel>> cacheRender;
        private boolean isGUI;
        private final ModelItemPropertyOverrideList overrides;

        BakedItemModel(BiFunction<ItemStack, World, StandardModel> model, Function<ItemStack, Pair<String, StandardModel>> cacheRender, ModelItemPropertyOverrideList overrides) {
            this.world = null;
            this.stack = null;
            this.model = model;
            this.overrides = overrides;
            this.cacheRender = cacheRender;
            isGUI = false;
        }

        BakedItemModel(ItemStack stack, World world, BiFunction<ItemStack, World, StandardModel> model, Function<ItemStack, Pair<String, StandardModel>> cacheRender, boolean isGUI, ModelItemPropertyOverrideList overrides) {
            this.stack = stack;
            this.world = world;
            this.model = model;
            this.cacheRender = cacheRender;
            this.isGUI = isGUI;
            this.overrides = overrides;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand) {
            if (stack == null || world == null) {
                return EMPTY;
            }

            if (isGUI) {
                iconSheet.renderSprite(cacheRender.apply(stack).getKey());
                return EMPTY;
            }

            StandardModel std = model.apply(stack, world);
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
            if (side == null) {
                std.renderCustom();
            }

            return std.getQuads(side, rand);
        }

        @Override
        public boolean useAmbientOcclusion() {
            return true;
        }

        @Override
        public boolean hasDepthInGui() {
            return true;
        }

        @Override
        public boolean isBuiltin() {
            return false;
        }

        @Override
        public Sprite getSprite() {
            return null;
        }

        @Override
        public ModelItemPropertyOverrideList getItemPropertyOverrides() {
            return overrides;
        }

        @Override
        public ModelTransformation getTransformation() {
            return new ModelTransformation(Transformation.NONE, Transformation.NONE, Transformation.NONE, Transformation.NONE, Transformation.NONE, Transformation.NONE, Transformation.NONE, Transformation.NONE) {
                public Transformation getTransformation(ModelTransformation.Type cameraTransformType) {
                    // TODO more efficient
                    if (cacheRender != null && (cameraTransformType == Type.GUI)) {
                        isGUI = true;
                    } else {
                        isGUI = false;
                    }
                    // TODO Expose as part of the renderItem API
                    if (cameraTransformType == Type.FIXED) {
                        return new Transformation(new Vector3f(0, 90, 0), new Vector3f(), new Vector3f(1,1,1));
                    }
                    if (cameraTransformType == Type.HEAD) {
                        return new Transformation(new Vector3f(), new Vector3f(0, 1, 0), new Vector3f(2,2,2));
                    }
                    //TODO minecraft item defaults
                    return Transformation.NONE;
                }
            };
        }

        @Override
        public boolean isVanillaAdapter() {
            return false;
        }

        @Override
        public void emitBlockQuads(ExtendedBlockView extendedBlockView, BlockState blockState, BlockPos blockPos, Supplier<Random> supplier, RenderContext renderContext) {
        }

        @Override
        public void emitItemQuads(net.minecraft.item.ItemStack itemStack, Supplier<Random> supplier, RenderContext renderContext) {
            //TODO remove render override hack?
            renderContext.fallbackConsumer().accept(this);
        }
    }
}
