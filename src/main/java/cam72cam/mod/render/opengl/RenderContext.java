package cam72cam.mod.render.opengl;

import cam72cam.mod.render.OpenGL;

public interface RenderContext {
    OpenGL.With apply(RenderState state);
}