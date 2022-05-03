package cam72cam.mod.render;

import cam72cam.mod.render.opengl.RenderState;

@FunctionalInterface
public interface RenderFunction {
    void render(RenderState state, float partialTicks);
}
