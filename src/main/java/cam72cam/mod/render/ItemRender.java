package cam72cam.mod.render;

import cam72cam.mod.Config;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.ModCore;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.gui.Progress;
import cam72cam.mod.item.CustomItem;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.render.opengl.RenderContext;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.BuiltinPack;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.util.With;
import cam72cam.mod.world.World;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

/** Item Render Registry (Here be dragons...) */
public class ItemRender {
    private static final List<BakedQuad> EMPTY = Collections.emptyList();
    private static final SpriteSheet iconSheet = new SpriteSheet(Config.SpriteSize);

    //String template for simple item models
    private static final String itemPath = "items/%s.json";
    private static final String modelPath = "models/item/%s.json";
    private static final String simpleTexItem = """
            {
              "model": {
                "type": "minecraft:model",
                "model": "%s:item/%s"
              }
            }""";
    private static final String simpleTexModel = """
            {
              "parent": "minecraft:item/generated",
              "textures": {
                  "layer0": "%s"
              }
            }""";
    private static final String composedItem = """
            {
              "model": {
                "type": "minecraft:special",
                "base": "minecraft:item/chest",
                "model": {
                  "type": "universalmodcore:items"
                }
              }
            }""";

    /** Register a simple image for an item */
    public static void register(CustomItem item, Identifier tex) {
        // Put (deferred) model data
        CommonEvents.Item.REGISTER.post(() -> {
            BuiltinPack.addNamespace(item.getRegistryName().getDomain());
            BuiltinPack.addNamespace(tex.getDomain());

            String modelPath = String.format(ItemRender.modelPath, item.getRegistryName().getPath());
            Identifier modelJson = new Identifier(item.getRegistryName().getDomain(), modelPath);
            BuiltinPack.put(modelJson, String.format(simpleTexModel, tex).getBytes(StandardCharsets.UTF_8));

            String itemPath = String.format(ItemRender.itemPath, item.getRegistryName().getPath());
            Identifier itemJson = new Identifier(item.getRegistryName().getDomain(), itemPath);
            BuiltinPack.put(itemJson, String.format(simpleTexItem, item.getRegistryName().getDomain(), item.getRegistryName().getPath())
                                            .getBytes(StandardCharsets.UTF_8));
        });

        ClientEvents.TEXTURE_STITCH.subscribe(evt -> evt.addSprite(tex.internal));
    }

    /** Register a complex model for an item */
    public static void register(CustomItem item, IItemModel model) {
        // Put (deferred) model data
        CommonEvents.Item.REGISTER.post(() -> {
            BuiltinPack.addNamespace(item.getRegistryName().getDomain());

            String itemPath = String.format(ItemRender.itemPath, item.getRegistryName().getPath());
            Identifier itemJson = new Identifier(item.getRegistryName().getDomain(), itemPath);
            BuiltinPack.put(itemJson, composedItem.getBytes(StandardCharsets.UTF_8));
        });

        // Link Item Registry Name to Custom Model
        ClientEvents.MODEL_BAKE.subscribe(event -> {
            ModelBakery.BakingResult bakingResult = event.getBakingResult();
            bakingResult.blockStateModels().put(new ModelResourceLocation(item.getRegistryName().internal, ""), new BakedItemModel(model));
        });

        // Hook up Sprite Support (and generation)
        if (model instanceof ISpriteItemModel) {
            ClientEvents.HACKS.subscribe(() -> {
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
        NONE(ItemDisplayContext.NONE),
        THIRD_PERSON_LEFT_HAND(ItemDisplayContext.THIRD_PERSON_LEFT_HAND),
        THIRD_PERSON_RIGHT_HAND(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND),
        FIRST_PERSON_LEFT_HAND(ItemDisplayContext.FIRST_PERSON_LEFT_HAND),
        FIRST_PERSON_RIGHT_HAND(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND),
        HEAD(ItemDisplayContext.HEAD),
        GUI(ItemDisplayContext.GUI),
        ENTITY(ItemDisplayContext.GROUND),
        FRAME(ItemDisplayContext.FIXED);

        private final ItemDisplayContext type;

        ItemRenderType(ItemDisplayContext type) {
            this.type = type;
        }

        public static ItemRenderType from(ItemDisplayContext cameraTransformType) {
            for (ItemRenderType type : values()) {
                if (cameraTransformType == type.type) {
                    return type;
                }
            }
            //We're facing enums added by other mods, use NONE as fallback
            return NONE;
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
            if (type == ItemRenderType.GUI) {
                ctx.stage(RenderContext.Stage.ITEM_IN_GUI);
            } else {
                ctx.stage(RenderContext.Stage.ITEM_IN_WORLD);
            }
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
                ByteBuffer buff = BufferUtils.createByteBuffer(4 * width * height);
                buff.put(ByteBuffer.wrap(Files.readAllBytes(sprite.toPath())));
                buff.flip();
                iconSheet.setSprite(id, buff);
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        With restore = OptiFine.overrideFastRender(false);
        RenderType.cutout().setupRenderState();

        TextureTarget fb = new TextureTarget(width, height, true, true);
        fb.setClearColor(0, 0, 0, 0);
        fb.clear();
        fb.bindWrite(true);

        RenderState state = new RenderState();
        state.model_view().setIdentity();
        state.projection().setIdentity();

        try (With with = RenderContext.apply(state)) {
            boolean depthEnabled = GL11.glGetBoolean(GL11.GL_DEPTH_TEST);
            int oldDepth = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);

            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LESS);
            GL11.glClearDepth(1);

            model.renderCustom(new RenderState().stage(RenderContext.Stage.ITEM_SPRITE_TEX));

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
        }

        RenderType.cutout().clearRenderState();
        restore.close();
    }

    static BiConsumer<PoseStack, Integer> doRender = (s, i) -> {};

    public static void setupRenderer() {
        SpecialModelRenderers.ID_MAPPER.put(ResourceLocation.fromNamespaceAndPath(ModCore.MODID, "items"),
                                            UMCItemModelRenderer.Unbaked.MAP_CODEC);
    }

    static class UMCItemModelRenderer implements NoDataSpecialModelRenderer {
        @Override
        public void render(ItemDisplayContext ctx, PoseStack matrixStack, MultiBufferSource source, int combinedLight, int combinedOverlay, boolean hasFoil) {
            doRender.accept(matrixStack, combinedLight);
        }

        record Unbaked(Void data) implements SpecialModelRenderer.Unbaked {
            public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new UMCItemModelRenderer.Unbaked(null));

            @Override
            public MapCodec<? extends SpecialModelRenderer.Unbaked> type() {
                return MAP_CODEC;
            }

            @Override
            public @NotNull SpecialModelRenderer<?> bake(EntityModelSet p_388631_) {
                return new UMCItemModelRenderer();
            }
        }
    }

    /** Custom Model where we can hack into the MC/Forge render system */
    static class BakedItemModel implements BakedModel {
        private ItemStack stack;
        private final IItemModel model;
        private ItemRenderType type;


        BakedItemModel(IItemModel model) {
            this.stack = null;
            this.model = model;
            this.type = ItemRenderType.NONE;
        }

        @Override
        public List<BakedQuad> getQuads(@org.jetbrains.annotations.Nullable BlockState p_235039_, @org.jetbrains.annotations.Nullable Direction p_235040_, RandomSource p_235041_) {
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
        public TextureAtlasSprite getParticleIcon() {
            return null;
        }

        @Override
        public ItemTransforms getTransforms() {
            //TODO 1.21.4
            return ItemTransforms.NO_TRANSFORMS;
        }

        @Override
        public void applyTransform(ItemDisplayContext cameraTransformType, PoseStack mat, boolean applyLeftHandTransform) {
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
                    RenderType.cutoutMipped().setupRenderState();

                    mat.pushPose();
                    // Maybe backwards?
                    //mat.last().pose().mul(matrix.last().pose());

                    RenderState state = new RenderState(mat);
                    model.applyTransform(stack, type, state);

                    int j = i % 65536;
                    int k = i / 65536;
                    state.lightmap(j/240f, k/240f);
                    RenderContext.lastLightX = j;
                    RenderContext.lastLightY = k;

                    //std.renderCustom();
                    std.render(state);

                    mat.popPose();

                    RenderType.cutoutMipped().setupRenderState();
                }
                // TODO return std.getQuads(side, rand);
            };
        }
    }
}
