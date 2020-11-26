package cam72cam.mod.render;

import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.block.*;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ILightReader;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/** A model that can render both standard MC constructs and custom OpenGL */
public class StandardModel {
    private List<Pair<BlockState, IBakedModel>> models = new ArrayList<>();
    private List<Consumer<Float>> custom = new ArrayList<>();

    /** Hacky way to turn an item into a blockstate, probably has some weird edge cases */
    private static BlockState itemToBlockState(cam72cam.mod.item.ItemStack stack) {
        Block block = Block.getBlockFromItem(stack.internal.getItem());
        BlockState gravelState = block.getDefaultState();//.getStateFromMeta(stack.internal.getMetadata());
        if (block instanceof LogBlock) {
            gravelState = gravelState.with(LogBlock.AXIS, Direction.Axis.Z);
        }
        return gravelState;
    }

    /** Add a block with a solid color */
    public StandardModel addColorBlock(Color color, Vec3d translate, Vec3d scale) {
        BlockState state = Fuzzy.CONCRETE.enumerate()
                .stream()
                .map(x -> Block.getBlockFromItem(x.internal.getItem()))
                .filter(x -> x.getMaterialColor(null, null, null) == color.internal.getMapColor())
                .map(Block::getDefaultState)
                .findFirst().get();

        IBakedModel model = Minecraft.getInstance().getBlockRendererDispatcher().getBlockModelShapes().getModel(state);
        models.add(Pair.of(state, new BakedScaledModel(model, scale, translate)));
        return this;
    }

    /** Add snow layers */
    public StandardModel addSnow(int layers, Vec3d translate) {
        layers = Math.max(1, Math.min(8, layers));
        BlockState state = Blocks.SNOW.getDefaultState().with(SnowBlock.LAYERS, layers);
        IBakedModel model = Minecraft.getInstance().getBlockRendererDispatcher().getBlockModelShapes().getModel(state);
        models.add(Pair.of(state, new BakedScaledModel(model, new Vec3d(1, 1, 1), translate)));
        return this;
    }

    /** Add item as a block (best effort) */
    public StandardModel addItemBlock(ItemStack bed, Vec3d translate, Vec3d scale) {
        BlockState state = itemToBlockState(bed);
        IBakedModel model = Minecraft.getInstance().getBlockRendererDispatcher().getBlockModelShapes().getModel(state);
        models.add(Pair.of(state, new BakedScaledModel(model, scale, translate)));
        return this;
    }

    /** Add item (think dropped item) */
    public StandardModel addItem(ItemStack stack, Vec3d translate, Vec3d scale) {
        custom.add((pt) -> {
            try (OpenGL.With matrix = OpenGL.matrix()) {
                GL11.glTranslated(translate.x, translate.y, translate.z);
                GL11.glScaled(scale.x, scale.y, scale.z);
                boolean oldState = GL11.glGetBoolean(GL11.GL_BLEND);
                IRenderTypeBuffer.Impl buffer = IRenderTypeBuffer.getImpl(new BufferBuilder(2048));
                if (oldState) {
                    GL11.glEnable(GL11.GL_BLEND);
                } else {
                    GL11.glDisable(GL11.GL_BLEND);
                }

                Minecraft.getInstance().getItemRenderer().renderItem(stack.internal, ItemCameraTransforms.TransformType.NONE, 15728880, OverlayTexture.NO_OVERLAY, new MatrixStack(), buffer);
                buffer.finish();
            }
        });
        return this;
    }

    /** Do whatever you want here! */
    public StandardModel addCustom(Runnable fn) {
        this.custom.add(pt -> fn.run());
        return this;
    }

    /** Do whatever you want here! (aware of partialTicks) */
    public StandardModel addCustom(Consumer<Float> fn) {
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

    /** Render this entire model */
    public void render() {
        render(0);
    }

    /** Render this entire model (partial tick aware) */
    public void render(float partialTicks) {
        renderCustom(partialTicks);
        renderQuads();
    }

    /** Render only the MC quads in this model */
    public void renderQuads() {
        if (models.isEmpty()) {
            return;
        }

        Minecraft.getInstance().getTextureManager().bindTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE);
        BufferBuilder worldRenderer = new BufferBuilder(2048);
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

            quads.forEach(quad -> worldRenderer.addQuad(new MatrixStack().getLast(), quad, f, f1, f2, 12 << 4, OverlayTexture.NO_OVERLAY));
        }

        worldRenderer.finishDrawing();
        WorldVertexBufferUploader.draw(worldRenderer);
    }

    /** Render the OpenGL parts directly */
    public void renderCustom() {
        renderCustom(0);
    }

    /** Render the OpenGL parts directly (partial tick aware) */
    public void renderCustom(float partialTicks) {
        custom.forEach(cons -> cons.accept(partialTicks));
    }

    /** Is there anything that's not MC standard in this model? */
    public boolean hasCustom() {
        return !custom.isEmpty();
    }
}
