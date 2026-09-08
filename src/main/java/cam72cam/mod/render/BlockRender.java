package cam72cam.mod.render;

import cam72cam.mod.ModCore;
import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.block.BlockType;
import cam72cam.mod.block.BlockTypeEntity;
import cam72cam.mod.block.tile.TileEntity;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.render.opengl.RenderContext;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Registry for block rendering (and internal implementation)
 *
 * Currently only supports TE's, not standard blocks
 */
public class BlockRender {
    // Don't need to return a *new* array list for no result
    private static final List<BakedQuad> EMPTY = Collections.emptyList();
    // Block coloring (grass) hooks
    private static final List<Consumer<BlockColors>> colors = new ArrayList<>();
    // BlockEntity type -> BlockEntity Renderer
    private static final Map<Identifier, Function<BlockEntity, StandardModel>> renderers = new HashMap<>();
    // Internal hack for globally rendered TE's
    private static List<net.minecraft.world.level.block.entity.BlockEntity> prev = new ArrayList<>();

    static {
        ClientEvents.TICK.subscribe(() -> {
            if (Minecraft.getInstance().level == null) {
                return;
            }
            /*
            Find all UMC TEs
            Create new array to prevent CME's with poorly behaving mods
            TODO: Opt out of renderGlobal!
             */
            /* TODO 1.17.1
            List<net.minecraft.tileentity.TileEntity> tes = new ArrayList<>(Minecraft.getInstance().level.getBlockEntity()).stream()
                    .filter(x -> x instanceof TileEntity && ((TileEntity) x).isLoaded() && x.getViewDistance() > 0)
                    .collect(Collectors.toList());
            if (Minecraft.getInstance().level.getGameTime() % 20 == 1) {
                prev = new ArrayList<>(Minecraft.getInstance().level.blockEntityList).stream()
                        .filter(x -> x instanceof TileEntity)
                        .collect(Collectors.toList());
            }
            Minecraft.getInstance().levelRenderer.updateGlobalBlockEntities(prev, tes);
            prev = tes;
             */
        });
    }

    /** Internal, do not use.  Is fired by UMC directly
     * @param blockColors*/
    public static void onPostColorSetup(BlockColors blockColors) {
        colors.forEach(r -> r.accept(blockColors));

        renderers.forEach((type, render) -> {
            BlockEntityRenderers.register(TileEntity.getType(type), (ted) -> new BlockEntityRenderer<TileEntity>() {
                @Override
                public boolean shouldRender(TileEntity p_173568_, Vec3 p_173569_) {
                    return p_173568_.instance() == null || Vec3.atCenterOf(p_173568_.getBlockPos()).closerThan(p_173569_, p_173568_.instance().getRenderDistance());
                }

                @Override
                public void render(TileEntity te, float partialTicks, PoseStack var3, MultiBufferSource p_112310_, int combinedLightIn, int p_112312_, Vec3 p_401186_) {
                    if (ModCore.isInReload()) {
                        return;
                    }

                    BlockEntity instance = te.instance();
                    if (instance == null) {
                        return;
                    }
                    StandardModel model = render.apply(instance);
                    if (model == null) {
                        return;
                    }

                    if (!model.hasCustom()) {
                        return;
                    }

                    RenderType.cutoutMipped().setupRenderState();

                    int j = combinedLightIn % 65536;
                    int k = combinedLightIn / 65536;
                    model.renderCustom(new RenderState(var3).lightmap(j/240f, k/240f).stage(RenderContext.Stage.BLOCK), partialTicks);

                    RenderType.cutoutMipped().clearRenderState();
                }

                @Override
                public AABB getRenderBoundingBox(TileEntity blockEntity) {
                    if (blockEntity.instance() != null) {
                        return blockEntity.bbCache.get(blockEntity.instance().getRenderBoundingBox());
                    }
                    return TileEntity.INFINITE_EXTENT_AABB;
                }

                @Override
                public boolean shouldRenderOffScreen() {
                    return true;
                }
            });
        });
    }

    // TODO version for non TE blocks

    public static <T extends BlockEntity> void register(BlockType block, Function<T, StandardModel> model, Class<T> cls) {
        renderers.put(((BlockTypeEntity)block).id, (te) -> model.apply(cls.cast(te)));

        colors.add((blockColors) -> {
            blockColors.register((state, worldIn, pos, tintIndex) -> worldIn != null && pos != null ? BiomeColors.getAverageGrassColor(worldIn, pos) : GrassColor.get(0.5D, 1.0D), block.internal);
        });

        ClientEvents.MODEL_BAKE.subscribe(event -> {
            ModelBakery.BakingResult bakingResult = event.getBakingResult();
            bakingResult.blockStateModels().put(block.internal.defaultBlockState(), new DynamicBlockStateModel() {
                @Override
                public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockModelPart> parts) {
                    if (block instanceof BlockTypeEntity) {
                        TileEntity data = level.getModelData(pos).get(TileEntity.TE_PROPERTY);
                        if (data == null || !cls.isInstance(data.instance())) {
                            System.out.println(data);
                            return;
                        }
                        StandardModel out = model.apply(cls.cast(data.instance()));
                        if (out == null) {
                            return;
                        }

                        QuadCollection.Builder builder = new QuadCollection.Builder();
                        out.getQuads(null, random).forEach(builder::addUnculledFace);
                        for (Direction dir : Direction.values()) {
                            out.getQuads(dir, random).forEach(quad -> builder.addCulledFace(dir, quad));
                        }
                        parts.add(new SimpleModelWrapper(builder.build(), true, particleIcon(), null));
                    } else {
                        // TODO
                    }
                }

                @Override
                public TextureAtlasSprite particleIcon() {
                    if (block.internal.defaultMapColor() == MapColor.METAL) {
                        return Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getBlockModel(Blocks.IRON_BLOCK.defaultBlockState()).particleIcon();
                    }
                    return Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getBlockModel(Blocks.STONE.defaultBlockState()).particleIcon();
                }
            });
        });
    }
}
