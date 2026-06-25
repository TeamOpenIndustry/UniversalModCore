package cam72cam.umc.api.render;

import cam72cam.umc.api.entity.Entity;
import cam72cam.umc.api.render.opengl.RenderState;

public interface IEntityRender<T extends Entity> {
    /** Called once per tick per entity */
    void render(T entity, RenderState state, float partialTicks);
    void postRender(T entity, RenderState state, float partialTicks);
}
