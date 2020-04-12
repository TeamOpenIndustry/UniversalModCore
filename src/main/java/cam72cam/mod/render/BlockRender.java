package cam72cam.mod.render;

import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.block.BlockType;
import cam72cam.mod.block.tile.TileEntity;
import cam72cam.mod.event.ClientEvents;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.color.world.GrassColors;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.model.*;
import net.minecraft.client.render.model.json.ModelItemPropertyOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class BlockRender {
    private static final List<Runnable> colors = new ArrayList<>();
    private static final Map<Class<? extends BlockEntity>, Function<BlockEntity, StandardModel>> renderers = new HashMap<>();
    private static List<net.minecraft.block.entity.BlockEntity> prev = new ArrayList<>();

    static {
        ClientEvents.TICK.subscribe(() -> {
            if (MinecraftClient.getInstance().world == null) {
                return;
            }
            List<net.minecraft.block.entity.BlockEntity> tes = new ArrayList<>(MinecraftClient.getInstance().world.blockEntities).stream()
                    .filter(x -> x instanceof TileEntity && ((TileEntity) x).isLoaded())
                    .collect(Collectors.toList());
            MinecraftClient.getInstance().worldRenderer.updateNoCullingBlockEntities(prev, tes);
            prev = tes;
        });
    }

    public static void onPostColorSetup() {
        colors.forEach(Runnable::run);

        TileEntity.getTypes().forEach(t -> {
        BlockEntityRendererRegistry.INSTANCE.register((BlockEntityType<TileEntity>)t, x -> new BlockEntityRenderer<TileEntity>(x) {
            @Override
            public void render(TileEntity te, float partialTicks, MatrixStack var3, VertexConsumerProvider var4, int var5, int var6) {
                BlockEntity instance = te.instance();
                if (instance == null) {
                    System.out.println("WAT NULL");
                    return;
                }
                Class<? extends BlockEntity> cls = instance.getClass();
                Function<BlockEntity, StandardModel> renderer = renderers.get(cls);
                if (renderer == null) {
                    return;
                }

                StandardModel model = renderer.apply(instance);
                if (model == null) {
                    return;
                }

                if (!model.hasCustom()) {
                    return;
                }

                GL11.glPushMatrix();
                {
                    RenderLayer.getSolid().startDrawing();
                    //GL11.glTranslated(te.pos.x, te.pos.y, te.pos.z);
                    RenderSystem.multMatrix(var3.peek().getModel());
                    model.renderCustom(partialTicks);
                    RenderLayer.getSolid().endDrawing();
                }
                GL11.glPopMatrix();
            }

            @Override
            public boolean rendersOutsideBoundingBox(TileEntity te) {
                return true;
            }
        });

        });
    }

    // TODO version for non TE blocks

    public static <T extends BlockEntity> void register(BlockType block, Function<T, StandardModel> model, Class<T> cls) {
        renderers.put(cls, (te) -> model.apply(cls.cast(te)));

        colors.add(() -> {
            BlockColors blockColors = MinecraftClient.getInstance().getBlockColorMap();
            blockColors.registerColorProvider((state, worldIn, pos, tintIndex) -> worldIn != null && pos != null ? BiomeColors.getGrassColor(worldIn, pos) : GrassColors.getColor(0.5D, 1.0D), block.internal);
        });

        ClientEvents.MODEL_BAKE.subscribe(() -> {
            ModelLoadingRegistry.INSTANCE.registerVariantProvider(manager -> (modelId, context) -> block.identifier.equals(modelId) ?
                    new UnbakedModel() {
                        @Override
                        public Collection<Identifier> getModelDependencies() {
                            return Collections.emptyList();
                        }

                        @Override
                        public Collection<SpriteIdentifier> getTextureDependencies(Function<Identifier, UnbakedModel> var1, Set<Pair<String, String>> var2) {
                            return Collections.emptyList();
                        }

                        @Nullable
                        @Override
                        public BakedModel bake(ModelLoader var1, Function<SpriteIdentifier, Sprite> var2, ModelBakeSettings var3, Identifier var4) {
                            return new FullBakedModel() {
                                @Override
                                public boolean isVanillaAdapter() {
                                    return false;
                                }

                                @Override
                                public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
                                    net.minecraft.block.entity.BlockEntity be = blockView.getBlockEntity(pos);
                                    if (be instanceof TileEntity) {
                                        TileEntity te = (TileEntity) be;
                                        BlockEntity data = te.instance();
                                        if (cls.isInstance(data)) {
                                            StandardModel out = model.apply(cls.cast(data));
                                            if (out != null) {
                                                context.fallbackConsumer().accept(new BakedModel() {
                                                    @Override
                                                    public List<BakedQuad> getQuads(@Nullable BlockState var1, @Nullable Direction side, Random rand) {
                                                        return out.getQuads(side, rand);
                                                    }

                                                    @Override
                                                    public boolean useAmbientOcclusion() {
                                                        return true;
                                                    }

                                                    @Override
                                                    public boolean hasDepth() {
                                                        return true;
                                                    }

                                                    @Override
                                                    public boolean isSideLit() {
                                                        return true;
                                                    }

                                                    @Override
                                                    public boolean isBuiltin() {
                                                        return false;
                                                    }

                                                    @Override
                                                    public Sprite getSprite() {
                                                        if (state.getMaterial() == Material.METAL) {
                                                            return MinecraftClient.getInstance().getBlockRenderManager().getModel(Blocks.IRON_BLOCK.getDefaultState()).getSprite();
                                                        }
                                                        return MinecraftClient.getInstance().getBlockRenderManager().getModel(Blocks.STONE.getDefaultState()).getSprite();
                                                    }

                                                    @Override
                                                    public ModelTransformation getTransformation() {
                                                        return null;
                                                    }

                                                    @Override
                                                    public ModelItemPropertyOverrideList getItemPropertyOverrides() {
                                                        return null;
                                                    }
                                                });
                                            }
                                        }
                                    }
                                }

                                @Override
                                public void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier, RenderContext context) {

                                }

                                @Override
                                public List<BakedQuad> getQuads(@Nullable BlockState var1, @Nullable Direction var2, Random var3) {
                                    return Collections.emptyList();
                                }

                                @Override
                                public boolean useAmbientOcclusion() {
                                    return false;
                                }

                                @Override
                                public boolean hasDepth() {
                                    return false;
                                }

                                @Override
                                public boolean isSideLit() {
                                    return true;
                                }

                                @Override
                                public boolean isBuiltin() {
                                    return false;
                                }

                                @Override
                                public Sprite getSprite() {
                                    return MinecraftClient.getInstance().getBlockRenderManager().getModel(Blocks.STONE.getDefaultState()).getSprite();
                                }

                                @Override
                                public ModelTransformation getTransformation() {
                                    return ModelTransformation.NONE;
                                }

                                @Override
                                public ModelItemPropertyOverrideList getItemPropertyOverrides() {
                                    return ModelItemPropertyOverrideList.EMPTY;
                                }
                            };
                        }
                    } : null);
            });
    }
}
