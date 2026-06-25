package cam72cam.umc.api.render;

import cam72cam.umc.api.render.opengl.RenderState;

@FunctionalInterface
public interface RenderFunction {
    void render(RenderState state, float partialTicks);
}
