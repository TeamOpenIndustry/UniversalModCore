package cam72cam.mod.render;

import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/** A model that can render both standard MC constructs and custom OpenGL */
public class StandardModel {
    private final List<Pair<BlockState, BakedModel>> models = new ArrayList<>();
    private final List<Consumer<Float>> custom = new ArrayList<>();

    /** Hacky way to turn an item into a blockstate, probably has some weird edge cases */
    private static BlockState itemToBlockState(cam72cam.mod.item.ItemStack stack) {
        Block block = Block.getBlockFromItem(stack.internal.getItem());
        return block.getDefaultState();
    }

    /** Add a block with a solid color */
    public StandardModel addColorBlock(Color color, Vec3d translate, Vec3d scale) {
        BlockState state = Fuzzy.CONCRETE.enumerate()
                .stream()
                .map(x -> Block.getBlockFromItem(x.internal.getItem()))
                .filter(x -> x.getMapColor(null, null, null) == color.internal.getMaterialColor())
                .map(Block::getDefaultState)
                .findFirst().get();
        BakedModel model = MinecraftClient.getInstance().getBlockRenderManager().getModel(state);
        models.add(Pair.of(state, new BakedScaledModel(model, scale, translate)));
        return this;
    }

    /** Add snow layers */
    public StandardModel addSnow(int layers, Vec3d translate) {
        layers = Math.max(1, Math.min(8, layers));
        BlockState state = Blocks.SNOW.getDefaultState().with(SnowBlock.LAYERS, layers);
        BakedModel model = MinecraftClient.getInstance().getBlockRenderManager().getModel(state);
        models.add(Pair.of(state, new BakedScaledModel(model, new Vec3d(1, 1, 1), translate)));
        return this;
    }

    /** Add item as a block (best effort) */
    public StandardModel addItemBlock(ItemStack bed, Vec3d translate, Vec3d scale) {
        BlockState state = itemToBlockState(bed);
        BakedModel model = MinecraftClient.getInstance().getBlockRenderManager().getModel(state);
        models.add(Pair.of(state, new BakedScaledModel(model, scale, translate)));
        return this;
    }

    /** Add item (think dropped item) */
    public StandardModel addItem(ItemStack stack, Vec3d translate, Vec3d scale) {
        custom.add((pt) -> {
            try (OpenGL.With matrix = OpenGL.matrix()) {
                GL11.glTranslated(translate.x, translate.y, translate.z);
                GL11.glScaled(scale.x, scale.y, scale.z);
                MinecraftClient.getInstance().getItemRenderer().renderItem(stack.internal, ModelTransformation.Type .NONE);
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
        for (Pair<BlockState, BakedModel> model : models) {
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
        List<BakedQuad> quads = new ArrayList<>();
        for (Pair<BlockState, BakedModel> model : models) {
            quads.addAll(model.getRight().getQuads(null, null, new Random()));
            for (Direction facing : Direction.values()) {
                quads.addAll(model.getRight().getQuads(null, facing, new Random()));
            }

        }
        if (quads.isEmpty()) {
            return;
        }

        MinecraftClient.getInstance().getTextureManager().bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);

        BufferBuilder worldRenderer = new BufferBuilder(2048);
        worldRenderer.begin(GL11.GL_QUADS, VertexFormats.POSITION_TEXTURE_COLOR_NORMAL);

        for (BakedQuad quad : quads) {
            worldRenderer.putVertexData(quad.getVertexData());
            /* TODO
            if (quad.hasColor()) {
                MinecraftClient.getInstance().getBlockColorMap().getColorMultiplier(state, null, null, 0)
                worldRenderer.setQuadColor(float_2 * float_1, float_3 * float_1, float_4 * float_1);
            } else {
                worldRenderer.setQuadColor(float_1, float_1, float_1);
            }
            */
            worldRenderer.setQuadColor(1, 1, 1);

            Vec3i vec3i_1 = quad.getFace().getVector();
            worldRenderer.postNormal((float)vec3i_1.getX(), (float)vec3i_1.getY(), (float)vec3i_1.getZ());
        }
        worldRenderer.end();
        new BufferRenderer().draw(worldRenderer);
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
