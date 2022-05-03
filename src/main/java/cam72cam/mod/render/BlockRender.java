package cam72cam.mod.render;

import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.block.BlockType;
import cam72cam.mod.block.tile.TileEntity;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.render.opengl.RenderState;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.world.IBlockAccess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registry for block rendering (and internal implementation)
 *
 * Currently only supports TE's, not standard blocks
 */
public class BlockRender {
    private static final List<Runnable> colors = new ArrayList<>();
    // BlockEntity type -> BlockEntity Renderer
    private static final Map<Class<? extends BlockEntity>, Function<BlockEntity, StandardModel>> renderers = new HashMap<>();
    // Internal hack for globally rendered TE's
    private static List<net.minecraft.tileentity.TileEntity> prev = new ArrayList<>();

    static {
        ClientEvents.TICK.subscribe(() -> {
            if (Minecraft.getMinecraft().theWorld == null) {
                return;
            }
            /*
            Find all UMC TEs
            Create new array to prevent CME's with poorly behaving mods
            TODO: Opt out of renderGlobal!
             */
            /*List tes = (List) new ArrayList(Minecraft.getMinecraft().theWorld.loadedTileEntityList).stream()
                    .filter()
                    .filter(x -> x instanceof TileEntity && ((TileEntity) x).isLoaded())
                    .collect(Collectors.toList());
            Minecraft.getMinecraft().renderGlobal.tileEntities.removeAll(prev);
            Minecraft.getMinecraft().renderGlobal.tileEntities.addAll(tes);
            prev = tes;*/
        });
    }

    /** Internal, do not use.  Is fired by UMC directly */
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
                model.renderCustom(new RenderState().translate(x, y, z), partialTicks);
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
                if (block.internal == blockIn) {
                    net.minecraft.tileentity.TileEntity tileRaw = world.getTileEntity(x, y, z);
                    if (tileRaw instanceof TileEntity) {
                        TileEntity tile = (TileEntity) tileRaw;

                        if (cls.isInstance(tile.instance())) {
                            StandardModel render = model.apply(cls.cast(tile.instance()));
                            if (render != null) {
                                render.renderQuads(world, x, y, z);
                            }
                        }
                    }
                    return true;
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

        BlockType.blocks.put(block, renderID);
        renderers.put(cls, (te) -> model.apply(cls.cast(te)));

        // TODO 1.7.10 block colors
    }
}
