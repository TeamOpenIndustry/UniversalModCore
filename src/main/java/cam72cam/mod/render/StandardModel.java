package cam72cam.mod.render;

import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.render.opengl.RenderContext;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.Texture;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.util.With;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.WeightedBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Pair;
import util.Matrix4;

import java.util.ArrayList;
import java.util.List;
import java.util.*;

/** A model that can render both standard MC constructs and custom OpenGL */
public class StandardModel {
    private final List<Pair<BlockState, BakedModel>> models = new ArrayList<>();
    private final List<RenderFunction> custom = new ArrayList<>();
    //Special hack for in-gui/item block model(quads)
    private final Map<Pair<BlockState, BakedModel>, LitRenderFunc> inGuiBlock = new HashMap<>();

    /** Hacky way to turn an item into a blockstate, probably has some weird edge cases */
    private static BlockState itemToBlockState(cam72cam.mod.item.ItemStack stack) {
        Block block = Block.byItem(stack.internal().getItem());
        BlockState gravelState = block.defaultBlockState();//.getStateFromMeta(stack.internal.getMetadata());
        if (block instanceof RotatedPillarBlock) {
            gravelState = gravelState.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        }
        return gravelState;
    }

    /** Add a block with a solid color */
    public StandardModel addColorBlock(Color color, Matrix4 transform) {
        BlockState state = Fuzzy.CONCRETE.enumerate()
                .stream()
                .map(x -> Block.byItem(x.internal().getItem()))
                .filter(x -> x.defaultMapColor() == color.internal.getMapColor())
                .map(Block::defaultBlockState)
                .findFirst().get();

        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getBlockModel(state);
        Pair<BlockState, BakedModel> pair = Pair.of(state, new BakedScaledModel(model, transform));
        models.add(pair);
        inGuiBlock.put(pair, getRenderFunc(new net.minecraft.world.item.ItemStack(state.getBlock().asItem()), transform));
        return this;
    }

    /** Add snow layers */
    public StandardModel addSnow(int layers, Matrix4 transform) {
        layers = Math.max(1, Math.min(8, layers));
        BlockState state = Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, layers);
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getBlockModel(state);
        models.add(Pair.of(state, new BakedScaledModel(model, transform)));
        return this;
    }

    /** Add item as a block (best effort) */
    public StandardModel addItemBlock(ItemStack bed, Matrix4 transform) {
        BlockState state = itemToBlockState(bed);
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getBlockModel(state);
        if (model instanceof WeightedBakedModel weightedBakedModel) {
            //TODO Modify result to make it not dynamic
        }
        Pair<BlockState, BakedModel> pair = Pair.of(state, new BakedScaledModel(model, transform));
        models.add(pair);
        inGuiBlock.put(pair, getRenderFunc(bed.internal(), transform));
        return this;
    }

    /** Add item (think dropped item) */
    public StandardModel addItem(ItemStack stack, Matrix4 transform) {
        custom.add((matrix, pt) -> {
            matrix.model_view().multiply(transform);

            //Otherwise we'll get brown-tinted light texture with iris...find out why
            try (With ctx = RenderContext.applyBaseState(matrix)) {
                Minecraft.getInstance().getItemRenderer().renderStatic(stack.internal(), ItemDisplayContext.NONE, 15728880, OverlayTexture.NO_OVERLAY,
                                                                       new PoseStack(), RenderContext.IMMEDIATE, null, 0);
                RenderContext.IMMEDIATE.endBatch();
            }
        });
        return this;
    }

    /** Do whatever you want here! */
    public StandardModel addCustom(RenderFunction fn) {
        this.custom.add(fn);
        return this;
    }

    /** Get the quads for the MC standard rendering */
    List<BakedQuad> getQuads(Direction side, RandomSource rand) {
        List<BakedQuad> quads = new ArrayList<>();
        for (Pair<BlockState, BakedModel> model : models) {
            quads.addAll(model.getValue().getQuads(model.getKey(), side, rand));
        }

        return quads;
    }

    /** Render this entire model
     * @param state*/
    public void render(RenderState state) {
        render(0, state);
    }

    /** Render this entire model (partial tick aware) */
    public void render(float partialTicks, RenderState state) {
        renderCustom(state, partialTicks);
        renderQuads(state);
    }

    /** Render only the MC quads in this model */
    public void renderQuads(RenderState state) {
        if (models.isEmpty()) {
            return;
        }

        try (With ctx = RenderContext.apply(state.clone().texture(Texture.wrap(new Identifier(TextureAtlas.LOCATION_BLOCKS))))) {
            BufferBuilder worldRenderer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);

            for (Pair<BlockState, BakedModel> model : models) {
                if ((state.getStage() == RenderContext.Stage.GUI
                        || state.getStage() == RenderContext.Stage.ITEM_IN_WORLD
                        || state.getStage() == RenderContext.Stage.ITEM_IN_GUI
                        || state.getStage() == RenderContext.Stage.OVERLAY)
                    && inGuiBlock.containsKey(model)) {
                    //Skip here as block's quad behave bad in those consequences
                    continue;
                }

                List<BakedQuad> quads = new ArrayList<>();

                int i = Minecraft.getInstance().getBlockColors().getColor(model.getLeft(), null, null, 0);
                float f = (float)(i >> 16 & 255) / 255.0F;
                float f1 = (float)(i >> 8 & 255) / 255.0F;
                float f2 = (float)(i & 255) / 255.0F;

                quads.addAll(model.getRight().getQuads(null, null, Minecraft.getInstance().font.random));
                for (Direction facing : Direction.values()) {
                    quads.addAll(model.getRight().getQuads(null, facing, Minecraft.getInstance().font.random));
                }

                quads.forEach(quad -> worldRenderer.putBulkData(new PoseStack().last(), quad, f, f1, f2, 1.0F, 12 << 4, OverlayTexture.NO_OVERLAY));
            }
            MeshData data = worldRenderer.build();
            if (data != null) {
                BufferUploader.draw(data);
            }
        }
    }

    /** Render the OpenGL parts directly
     * @param state*/
    public void renderCustom(RenderState state) {
        renderCustom(state, 0);
    }

    /** Render the OpenGL parts directly (partial tick aware) */
    public void renderCustom(RenderState state, float partialTicks) {
        custom.forEach(cons -> cons.render(state.clone(), partialTicks));
        switch (state.getStage()) {
            case GUI, ITEM_IN_GUI, OVERLAY ->
                    inGuiBlock.forEach((k, v) -> v.render(state.clone(), partialTicks, 15728880));
            case ITEM_IN_WORLD -> {
                    int light = (int) (RenderContext.lastLightY * 65536 + RenderContext.lastLightX);
                    inGuiBlock.forEach((k, v) -> v.render(state.clone(), partialTicks, light));
            }
        }

        if (!RenderContext.IMMEDIATE.startedBuilders.isEmpty()) {
            try (With ctx = RenderContext.apply(state.clone().texture(Texture.wrap(new Identifier(TextureAtlas.LOCATION_BLOCKS))))) {
                RenderContext.IMMEDIATE.endBatch();
            }
        }
    }

    /** Is there anything that's not MC standard in this model? */
    public boolean hasCustom() {
        return !custom.isEmpty();
    }

    private static LitRenderFunc getRenderFunc(net.minecraft.world.item.ItemStack stack, Matrix4 transform) {
        return (matrix, pt, light) -> {
            matrix.model_view().multiply(transform).translate(0.5, 0.5, 0.5);

            Block block = Block.byItem(stack.getItem());
            if (block instanceof RotatedPillarBlock) {
                //Rotate to Z direction
                matrix.model_view().rotate(Math.toRadians(90), 1, 0, 0);
            }
            try (With ctx = RenderContext.apply(matrix)) {
                Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.NONE,
                                                                       light, OverlayTexture.NO_OVERLAY,
                                                                       new PoseStack(), RenderContext.IMMEDIATE, null, 0);
                RenderContext.IMMEDIATE.endBatch();
            }
        };
    }

    @FunctionalInterface
    private static interface LitRenderFunc {
        void render(RenderState state, float partialTick, int light);
    }
}
