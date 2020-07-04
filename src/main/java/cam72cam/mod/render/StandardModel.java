package cam72cam.mod.render;

import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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
    private List<Consumer<Float>> custom = new ArrayList<>();

    private RenderBlocks renderBlocks = new RenderBlocks();

    private static Pair<Block, Integer> itemToBlockState(cam72cam.mod.item.ItemStack stack) {
        Block block = Block.getBlockFromItem(stack.internal.getItem());
        int meta = stack.internal.getMetadata();
        if (block instanceof BlockRotatedPillar) {
            meta = 2;
        }
        return Pair.of(block, meta);
    }

    public StandardModel addColorBlock(Color color, Vec3d translate, Vec3d scale) {
        addItemBlock(new ItemStack(Blocks.wool, 1, color.internal), translate, scale);
        return this;
    }

    public StandardModel addSnow(int layers, Vec3d translate) {
        addItemBlock(new ItemStack(Blocks.snow), translate, new Vec3d(1, Math.max(1, Math.min(8, layers))/8f, 1));
        return this;
    }

    public StandardModel addItemBlock(ItemStack stack, Vec3d translate, Vec3d scale) {
        if (stack.isEmpty()) {
            return this;
        }
        models.add((pt) -> {
            Block block = Block.getBlockFromItem(stack.internal.getItem());
            if (block != null) {
                renderBlocks.blockAccess = pt.world;
                renderBlocks.setRenderBounds(0 + translate.x, 0 + translate.y, 0 + translate.z, scale.x + translate.x, scale.y + translate.y, scale.z + translate.z);
                renderBlocks.lockBlockBounds = true;
                renderBlocks.setOverrideBlockTexture(block.getIcon(0, stack.internal.getMetadata()));
                renderBlocks.renderBlockAllFaces(block, pt.x, pt.y, pt.z);
                renderBlocks.lockBlockBounds = false;
            }
        });
        return this;
    }

    public StandardModel addItem(ItemStack stack, Vec3d translate, Vec3d scale) {
        if (stack.isEmpty()) {
            return this;
        }
        custom.add((pt) -> {
            try (OpenGL.With matrix = OpenGL.matrix(); OpenGL.With tex = OpenGL.bool(GL11.GL_TEXTURE_2D, true)) {
                GL11.glTranslated(translate.x, translate.y, translate.z);
                GL11.glScaled(scale.x, scale.y, scale.z);
                IItemRenderer ir = MinecraftForgeClient.getItemRenderer(stack.internal, IItemRenderer.ItemRenderType.ENTITY);
                if (ir != null) {
                    ir.renderItem(IItemRenderer.ItemRenderType.ENTITY, stack.internal);
                } else {
                    Block block = Block.getBlockFromItem(stack.internal.getItem());
                    if (block != null) {
                        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
                        renderBlocks.renderBlockAsItem(block, stack.internal.getMetadata(), 1.0f);
                    }
                }
            }
        });
        return this;
    }

    public StandardModel addCustom(Runnable fn) {
        this.custom.add(pt -> fn.run());
        return this;
    }

    public StandardModel addCustom(Consumer<Float> fn) {
        this.custom.add(fn);
        return this;
    }

    public void render() {
        render(0);
    }

    public void render(float partialTicks) {
        renderCustom(partialTicks);
        GL11.glPushMatrix();
        {
            GLBoolTracker tex = new GLBoolTracker(GL11.GL_TEXTURE_2D, true);
            GL11.glTranslated(0, -255, 0);
            Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
            Tessellator.instance.startDrawingQuads();
            renderQuads(Minecraft.getMinecraft().theWorld, 0, 255, 0);
            Tessellator.instance.draw();
            tex.restore();
        }
        GL11.glPopMatrix();
    }

    public void renderQuads(IBlockAccess world, int x, int y, int z) {
        models.forEach(a -> a.accept(new RenderInfo(world, x, y, z)));
    }

    public void renderCustom() {
        renderCustom(0);
    }

    public void renderCustom(float partialTicks) {
        custom.forEach(cons -> cons.accept(partialTicks));
    }

    public boolean hasCustom() {
        return !custom.isEmpty();
    }
}
