package cam72cam.mod.render;

import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.init.Blocks;
import net.minecraftforge.client.IItemRenderer;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class StandardModel {
    private List<Runnable> models = new ArrayList<>();
    private List<Consumer<Float>> custom = new ArrayList<>();

    private RenderBlocks renderBlocks = new RenderBlocks();

    private static Pair<Block, Integer> itemToBlockState(cam72cam.mod.item.ItemStack stack) {
        Block block = Block.getBlockFromItem(stack.internal.getItem());
        int meta = stack.internal.getItemDamage();
        if (block instanceof BlockRotatedPillar) {
            meta = 2;
        }
        return Pair.of(block, meta);
    }

    public StandardModel addColorBlock(Color color, Vec3d translate, Vec3d scale) {
        Block state = Blocks.wool;
        int meta = color.internal;

        models.add(() -> {
            renderBlocks.blockAccess = Minecraft.getMinecraft().theWorld;
            renderBlocks.setOverrideBlockTexture(state.getIcon(0, meta));
            renderBlocks.renderBlockAllFaces(state, 0, 0, 0);
        });

        return this;
    }

    public StandardModel addSnow(int layers, Vec3d translate) {
        models.add(() -> {
            renderBlocks.blockAccess = Minecraft.getMinecraft().theWorld;
            GL11.glPushMatrix();
            GL11.glScaled(1, Math.max(1, Math.min(8, layers))/8f, 1);
            GL11.glTranslated(0, (-1 + Math.max(1, Math.min(layers, 8))/8f)/2, 0);
            renderBlocks.renderBlockAllFaces(Blocks.snow, 0, 0, 0);
            GL11.glPopMatrix();
        });
        return this;
    }

    public StandardModel addItemBlock(ItemStack bed, Vec3d translate, Vec3d scale) {
        Pair<Block, Integer> info = itemToBlockState(bed);
        models.add(() -> {
            renderBlocks.blockAccess = Minecraft.getMinecraft().theWorld;
            GL11.glPushMatrix();
            GL11.glScaled(scale.x, scale.y, scale.z);
            GL11.glTranslated(translate.x, (-1 + scale.y)/2 + translate.y, translate.z);
            // TODO meta
            renderBlocks.renderBlockAllFaces(info.getKey(), 0, 0, 0);
            GL11.glPopMatrix();
        });
        return this;
    }

    public StandardModel addItem(ItemStack stack, Vec3d translate, Vec3d scale) {
        custom.add((pt) -> {
            GL11.glPushMatrix();
            {
                GL11.glTranslated(translate.x, translate.y, translate.z);
                GL11.glScaled(scale.x, scale.y, scale.z);
                RenderManager.instance.itemRenderer.renderItem(null, stack.internal, 0, IItemRenderer.ItemRenderType.ENTITY);
            }
            GL11.glPopMatrix();
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
        renderQuads();
    }

    public void renderQuads() {
        models.forEach(Runnable::run);
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
