package cam72cam.mod.render;

import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemStack;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.block.*;
import cam72cam.mod.render.opengl.RenderContext;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.Texture;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.util.With;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.Direction;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;
import util.Matrix4;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** A model that can render both standard MC constructs and custom OpenGL */
public class StandardModel {
    private final List<Pair<BlockState, IBakedModel>> models = new ArrayList<Pair<BlockState, IBakedModel>>() {
        @Override
        public boolean add(Pair<BlockState, IBakedModel> o) {
            worldRendererBuffer = null;
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

        IBakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getBlockModel(state);
        models.add(Pair.of(state, new BakedScaledModel(model, transform)));
        return this;
    }

    /** Add snow layers */
    public StandardModel addSnow(int layers, Matrix4 transform) {
        layers = Math.max(1, Math.min(8, layers));
        BlockState state = Blocks.SNOW.defaultBlockState().setValue(SnowBlock.LAYERS, layers);
        IBakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getBlockModel(state);
        models.add(Pair.of(state, new BakedScaledModel(model, transform)));
        return this;
    }

    /** Add item as a block (best effort) */
    public StandardModel addItemBlock(ItemStack bed, Matrix4 transform) {
        BlockState state = itemToBlockState(bed);
        IBakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getBlockModel(state);
        models.add(Pair.of(state, new BakedScaledModel(model, transform)));
        return this;
    }

    /** Add item (think dropped item) */
    public StandardModel addItem(ItemStack stack, Matrix4 transform) {
        custom.add((matrix, pt) -> {
            matrix.model_view().multiply(transform);

            try (With ctx = RenderContext.apply(matrix)) {
                boolean oldState = GL11.glGetBoolean(GL11.GL_BLEND);
                IRenderTypeBuffer.Impl buffer = IRenderTypeBuffer.immediate(itemRenderer);
                if (oldState) {
                    GL11.glEnable(GL11.GL_BLEND);
                } else {
                    GL11.glDisable(GL11.GL_BLEND);
                }

                Minecraft.getInstance().getItemRenderer().renderStatic(stack.internal, ItemCameraTransforms.TransformType.NONE, 15728880, OverlayTexture.NO_OVERLAY, new MatrixStack(), buffer);
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
        for (Pair<BlockState, IBakedModel> model : models) {
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
    private com.mojang.datafixers.util.Pair<BufferBuilder.DrawState, ByteBuffer> worldRendererBuffer = null;

    /** Render only the MC quads in this model */
    public void renderQuads(RenderState state) {
        if (models.isEmpty()) {
            return;
        }

        if (worldRendererBuffer == null) {
            worldRenderer = new BufferBuilder(2048);
            worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

            for (Pair<BlockState, IBakedModel> model : models) {
                List<BakedQuad> quads = new ArrayList<>();

                int i = Minecraft.getInstance().getBlockColors().getColor(model.getLeft(), null, null, 0);
                float f = (float)(i >> 16 & 255) / 255.0F;
                float f1 = (float)(i >> 8 & 255) / 255.0F;
                float f2 = (float)(i & 255) / 255.0F;

                quads.addAll(model.getRight().getQuads(null, null, new Random()));
                for (Direction facing : Direction.values()) {
                    quads.addAll(model.getRight().getQuads(null, facing, new Random()));
                }

                quads.forEach(quad -> worldRenderer.addVertexData(new MatrixStack().last(), quad, f, f1, f2, 1.0f, 12 << 4, OverlayTexture.NO_OVERLAY));
            }

            worldRenderer.end();
            worldRendererBuffer = worldRenderer.popNextBuffer();
        }
        try (With ctx = RenderContext.apply(state.clone().texture(Texture.wrap(new Identifier(AtlasTexture.LOCATION_BLOCKS))))) {
            WorldVertexBufferUploader.end(new BufferBuilder(0) {
                @Override
                public com.mojang.datafixers.util.Pair<DrawState, ByteBuffer> popNextBuffer() {
                    // java is fun...
                    return worldRendererBuffer;
                }
            });
        }
    }

    /** Render the OpenGL parts directly
     * @param state*/
    public void renderCustom(RenderState state) {
        renderCustom(state, 0);
    }

    private BufferBuilder itemRenderer = null;
    /** Render the OpenGL parts directly (partial tick aware) */
    public void renderCustom(RenderState state, float partialTicks) {
        if (itemRenderer == null) {
            // This is not the best method, but is rarely used?  To be revisited
            itemRenderer = new BufferBuilder(256);
        }
        custom.forEach(cons -> cons.render(state.clone(), partialTicks));
        if (itemRenderer.building()) {
            itemRenderer.end();
            try (With ctx = RenderContext.apply(state.clone().texture(Texture.wrap(new Identifier(AtlasTexture.LOCATION_BLOCKS))))) {
                WorldVertexBufferUploader.end(itemRenderer);
            }
        }
    }

    /** Is there anything that's not MC standard in this model? */
    public boolean hasCustom() {
        return !custom.isEmpty();
    }
}
