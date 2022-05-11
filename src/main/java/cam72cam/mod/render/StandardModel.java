package cam72cam.mod.render;

import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.VertexBuffer;
import cam72cam.mod.render.opengl.RenderContext;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.Texture;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.util.With;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.init.Blocks;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;
import util.Matrix4;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** A model that can render both standard MC constructs and custom OpenGL */
public class StandardModel {
    private List<Consumer<RenderInfo>> models = new ArrayList<>();

    private class RenderInfo {
        IBlockAccess world;
        int x;
        int y;
        int z;

        public RenderInfo(IBlockAccess world, int x, int y, int z) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
    private List<RenderFunction> custom = new ArrayList<>();

    private RenderBlocks renderBlocks = new RenderBlocks();

    /** Hacky way to turn an item into a blockstate, probably has some weird edge cases */
    private static Pair<Block, Integer> itemToBlockState(cam72cam.mod.item.ItemStack stack) {
        Block block = Block.getBlockFromItem(stack.internal.getItem());
        int meta = stack.internal.getMetadata();
        if (block instanceof BlockRotatedPillar) {
            meta = 2;
        }
        return Pair.of(block, meta);
    }

    /** Add a block with a solid color */
    public StandardModel addColorBlock(Color color, Matrix4 transform) {
        addItemBlock(new ItemStack(new net.minecraft.item.ItemStack(Blocks.stained_hardened_clay, 1, color.internal)), transform);
        return this;
    }

    /** Add snow layers */
    public StandardModel addSnow(int layers, Matrix4 transform) {
        addItemBlock(new ItemStack(new net.minecraft.item.ItemStack(Blocks.snow)), transform.copy().scale(1, Math.max(1, Math.min(8, layers))/8f, 1));
        return this;
    }

    /** Add item as a block (best effort) */
    public StandardModel addItemBlock(ItemStack stack, Matrix4 matrix4) {
        if (stack.isEmpty()) {
            return this;
        }
        Vec3d bmin = matrix4.apply(new Vec3d(0, 0, 0));
        Vec3d bmax = matrix4.apply(new Vec3d(1, 1, 1));
        models.add((pt) -> {
            Block block = Block.getBlockFromItem(stack.internal.getItem());
            if (block != null) {
                renderBlocks.blockAccess = pt.world;
                renderBlocks.setRenderBounds(bmin.x, bmin.y, bmin.z, bmax.x, bmax.y, bmax.z);
                renderBlocks.lockBlockBounds = true;
                renderBlocks.setOverrideBlockTexture(block.getIcon(0, stack.internal.getMetadata()));
                renderBlocks.renderBlockAllFaces(block, pt.x, pt.y, pt.z);
                renderBlocks.lockBlockBounds = false;
            }
        });
        return this;
    }

    /** Add item (think dropped item) */
    public StandardModel addItem(ItemStack stack, Matrix4 apply) {
        if (stack.isEmpty()) {
            return this;
        }
        custom.add((state, pt) -> {
            state = state.clone().texture(Texture.wrap(new Identifier(TextureMap.locationBlocksTexture)));
            state.model_view().multiply(apply);

            try (With ctx = RenderContext.apply(state)) {
                IItemRenderer ir = MinecraftForgeClient.getItemRenderer(stack.internal, IItemRenderer.ItemRenderType.ENTITY);
                if (ir != null) {
                    ir.renderItem(IItemRenderer.ItemRenderType.ENTITY, stack.internal);
                } else {
                    Block block = Block.getBlockFromItem(stack.internal.getItem());
                    if (block != null) {
                        renderBlocks.renderBlockAsItem(block, stack.internal.getMetadata(), 1.0f);
                    }
                }
            }
        });
        return this;
    }

    /** Do whatever you want here! */
    public StandardModel addCustom(RenderFunction fn) {
        this.custom.add(fn);
        return this;
    }

    /** Render this entire model */
    public void render(RenderState state) {
        render(state, 0);
    }

    /** Render this entire model (partial tick aware) */
    public void render(RenderState state, float partialTicks) {
        renderCustom(state, partialTicks);
        try (With ctx = RenderContext.apply(state.clone().texture(Texture.wrap(new Identifier(TextureMap.locationBlocksTexture))))) {
            GL11.glTranslated(0, -255, 0);
            Tessellator.instance.startDrawingQuads();
            renderQuads(Minecraft.getMinecraft().theWorld, 0, 255, 0);
            Tessellator.instance.draw();
        }
    }

    public void renderQuads(IBlockAccess world, int x, int y, int z) {
        models.forEach(a -> a.accept(new RenderInfo(world, x, y, z)));
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
