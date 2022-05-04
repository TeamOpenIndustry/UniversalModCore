package cam72cam.mod.render;

import cam72cam.mod.entity.Entity;
import cam72cam.mod.render.opengl.RenderState;

public interface IEntityRender<T extends Entity> {
    /** Called once per tick per entity */
    void render(T entity, RenderState state, float partialTicks);
    void postRender(T entity, RenderState state, float partialTicks);
}
