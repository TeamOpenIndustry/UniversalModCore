package cam72cam.mod.render;

import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.block.BlockType;
import cam72cam.mod.block.BlockTypeEntity;
import cam72cam.mod.block.tile.TileEntity;
import cam72cam.mod.event.ClientEvents;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.world.IBlockAccess;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BlockRender {
    private static Map<BlockType, Integer> blocks = new HashMap<>();
    private static final List<Runnable> colors = new ArrayList<>();
    private static final Map<Class<? extends BlockEntity>, Function<BlockEntity, StandardModel>> renderers = new HashMap<>();
    private static List<net.minecraft.tileentity.TileEntity> prev = new ArrayList<>();

    static {
        ClientEvents.TICK.subscribe(() -> {
            if (Minecraft.getMinecraft().theWorld == null) {
                return;
            }
            List tes = (List) Minecraft.getMinecraft().theWorld.loadedTileEntityList.stream()
                    .filter(x -> x instanceof TileEntity && ((TileEntity) x).isLoaded())
                    .collect(Collectors.toList());
            Minecraft.getMinecraft().renderGlobal.tileEntities.removeAll(prev);
            Minecraft.getMinecraft().renderGlobal.tileEntities.addAll(tes);
            prev = tes;
        });
    }

    public static void onPostColorSetup() {
        colors.forEach(Runnable::run);

        ClientRegistry.bindTileEntitySpecialRenderer(TileEntity.class, new TileEntitySpecialRenderer() {
            @Override
            public void renderTileEntityAt(net.minecraft.tileentity.TileEntity teuncast, double x, double y, double z, float partialTicks) {
                TileEntity te = (TileEntity) teuncast;
                BlockEntity instance = te.instance();
                if (instance == null) {
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
                    GL11.glTranslated(x, y, z);
                    model.renderCustom(partialTicks);
                }
                GL11.glPopMatrix();
            }
        });
    }

    // TODO version for non TE blocks

    public static <T extends BlockEntity> void register(BlockType block, Function<T, StandardModel> model, Class<T> cls) {

        int renderID = RenderingRegistry.getNextAvailableRenderId();

        RenderingRegistry.registerBlockHandler(renderID, new ISimpleBlockRenderingHandler() {
            @Override
            public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
                // NOP
            }

            @Override
            public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block blockIn, int modelId, RenderBlocks renderer) {
                if (block instanceof BlockTypeEntity) {
                    net.minecraft.tileentity.TileEntity tile = world.getTileEntity(x, y, z);
                    if (cls.isInstance(tile)) {
                        StandardModel render = model.apply(cls.cast(tile));
                        if (render != null) {
                            render.renderQuads();
                        }
                    }
                } else {
                    // TODO
                }
                return false;
            }

            @Override
            public boolean shouldRender3DInInventory(int modelId) {
                return false;
            }

            @Override
            public int getRenderId() {
                return renderID;
            }
        });

        blocks.put(block, renderID);
        renderers.put(cls, (te) -> model.apply(cls.cast(te)));

        // TODO 1.7.10 block colors
    }

    public static int getRenderType(BlockType block) {
        return blocks.get(block);
    }
}
