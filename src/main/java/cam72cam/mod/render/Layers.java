package cam72cam.mod.render;

import friedrichlp.renderlib.render.ViewBoxes;
import friedrichlp.renderlib.tracking.RenderLayer;
import friedrichlp.renderlib.tracking.RenderManager;

public class Layers {
    public static RenderLayer ENTITY = RenderManager.addRenderLayer(ViewBoxes.ALWAYS);
    public static RenderLayer TILES = RenderManager.addRenderLayer(ViewBoxes.ALWAYS);
}
