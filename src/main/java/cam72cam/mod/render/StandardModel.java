package cam72cam.mod.render;

import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemStack;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.*;
import cam72cam.mod.render.opengl.RenderContext;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.Texture;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.util.With;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL32;
import util.Matrix4;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** A model that can render both standard MC constructs and custom OpenGL */
public class StandardModel {
    private final List<Pair<BlockState, BakedModel>> models = new ArrayList<>() {
        @Override
        public boolean add(Pair<BlockState, BakedModel> o) {
            worldRenderer = null;
            return super.add(o);
        }
    };
    private final List<RenderFunction> custom = new ArrayList<>();

    /** Hacky way to turn an item into a blockstate, probably has some weird edge cases */
    private static BlockState itemToBlockState(cam72cam.mod.item.ItemStack stack) {
        Block block = Block.byItem(stack.internal.getItem());
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
                .map(x -> Block.byItem(x.internal.getItem()))
                .filter(x -> x.defaultMaterialColor() == color.internal.getMaterialColor())
                .map(Block::defaultBlockState)
                .findFirst().get();

        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getBlockModel(state);
        models.add(Pair.of(state, new BakedScaledModel(model, transform)));
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
        models.add(Pair.of(state, new BakedScaledModel(model, transform)));
        return this;
    }

    /** Add item (think dropped item) */
    public StandardModel addItem(ItemStack stack, Matrix4 transform) {
        custom.add((matrix, pt) -> {
            matrix.model_view().multiply(transform);

            try (With ctx = RenderContext.apply(matrix)) {
                boolean oldState = GL32.glGetBoolean(GL32.GL_BLEND);
                MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(worldRenderer);
                if (oldState) {
                    GL32.glEnable(GL32.GL_BLEND);
                } else {
                    GL32.glDisable(GL32.GL_BLEND);
                }

                Minecraft.getInstance().getItemRenderer().renderStatic(stack.internal, ItemTransforms.TransformType.NONE, 15728880, OverlayTexture.NO_OVERLAY, new PoseStack(), buffer, 0);
                buffer.endBatch();
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
    List<BakedQuad> getQuads(Direction side, Random rand) {
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

    private BufferBuilder worldRenderer = null;

    /** Render only the MC quads in this model */
    public void renderQuads(RenderState state) {
        if (models.isEmpty()) {
            return;
        }

        if (worldRenderer == null) {
            worldRenderer = new BufferBuilder(2048) {
                @Override
                public void discard() {
                    //super.discard();
                }
            };
            worldRenderer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);

            for (Pair<BlockState, BakedModel> model : models) {
                List<BakedQuad> quads = new ArrayList<>();

                int i = Minecraft.getInstance().getBlockColors().getColor(model.getLeft(), null, null, 0);
                float f = (float)(i >> 16 & 255) / 255.0F;
                float f1 = (float)(i >> 8 & 255) / 255.0F;
                float f2 = (float)(i & 255) / 255.0F;

                quads.addAll(model.getRight().getQuads(null, null, new Random()));
                for (Direction facing : Direction.values()) {
                    quads.addAll(model.getRight().getQuads(null, facing, new Random()));
                }

                quads.forEach(quad -> worldRenderer.putBulkData(new PoseStack().last(), quad, f, f1, f2, 1.0f, 12 << 4, OverlayTexture.NO_OVERLAY));
            }
            worldRenderer.end();
        }
        try (With ctx = RenderContext.apply(state.clone().texture(Texture.wrap(new Identifier(TextureAtlas.LOCATION_BLOCKS))))) {
            BufferUploader.end(worldRenderer);
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
    }

    /** Is there anything that's not MC standard in this model? */
    public boolean hasCustom() {
        return !custom.isEmpty();
    }
}
