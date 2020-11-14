package cam72cam.mod.render;

import cam72cam.mod.entity.Entity;

public interface IEntityRender<T extends Entity> {
    /** Called once per tick per entity */
    void render(T entity, float partialTicks);
}
