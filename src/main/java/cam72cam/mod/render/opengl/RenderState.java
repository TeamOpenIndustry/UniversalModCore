package cam72cam.mod.render.opengl;

import cam72cam.mod.math.Vec3d;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import util.Matrix4;

import java.util.HashMap;
import java.util.Map;

public class RenderState {
    protected Matrix4 model_view = null;
    protected Matrix4 projection = null;
    protected Texture texture = null;
    protected float[] color = null;
    protected Map<Integer, Boolean> bools = new HashMap<>();
    protected Boolean smooth_shading = null;
    protected float[] lightmap = null;
    protected BlendMode blend = null;

    /*

    RenderState state = new RenderState()
    state.translate(1, 2, 3)

    BoundVBO vbo = model.bind(state) // apply state and clone
    vbo.draw(state -> {
        state.lighting(true);
        return groups
    });
    vbo.draw(groups)




     */

    public RenderState() {
    }

    private RenderState(RenderState ctx) {
        this.model_view = ctx.model_view != null ? ctx.model_view.copy() : null;
        this.projection = ctx.projection != null ? ctx.projection.copy() : null;
        this.texture = ctx.texture;
        this.color = ctx.color != null ? ctx.color.clone() : null;
        this.bools = new HashMap<>(ctx.bools);
        this.smooth_shading = ctx.smooth_shading;
        this.lightmap = ctx.lightmap != null ? ctx.lightmap.clone() : null;
        this.blend = ctx.blend;
    }

    public RenderState clone() {
        return new RenderState(this);
    }

    public RenderState color(float r, float g, float b, float a) {
        color = new float[] {r, g, b, a};
        return this;
    }

    public Matrix4 model_view() {
        if (model_view == null) {
            model_view = new Matrix4();
        }
        return model_view;
    }

    public Matrix4 projection() {
        if (projection == null) {
            projection = new Matrix4();
        }
        return projection;
    }

    public RenderState translate(Vec3d vec) {
        return this.translate(vec.x, vec.y, vec.z);
    }
    public RenderState translate(double x, double y, double z) {
        this.model_view().translate(x, y, z);
        return this;
    }
    public RenderState scale(Vec3d vec) {
        return this.scale(vec.x, vec.y, vec.z);
    }
    public RenderState scale(double x, double y, double z) {
        this.model_view().scale(x, y, z);
        return this;
    }
    public RenderState rotate(double degrees, double x, double y, double z) {
        this.model_view().rotate(Math.toRadians(degrees), x, y, z);
        return this;
    }

    public RenderState texture(Texture tex) {
        this.texture = tex;
        return this;
    }

    public RenderState lighting(boolean lighting) {
        this.bools.put(GL11.GL_LIGHTING, lighting);
        return this;
    }
    public RenderState alpha_test(boolean alpha_test) {
        this.bools.put(GL11.GL_ALPHA_TEST, alpha_test);
        return this;
    }
    public RenderState depth_test(boolean depth_test) {
        this.bools.put(GL11.GL_DEPTH_TEST, depth_test);
        return this;
    }
    public RenderState smooth_shading(boolean smooth_shading) {
        this.smooth_shading = smooth_shading;
        return this;
    }
    public RenderState rescale_normal(boolean rescale_normal) {
        this.bools.put(GL12.GL_RESCALE_NORMAL, rescale_normal);
        return this;
    }
    public RenderState cull_face(boolean cull_face) {
        this.bools.put(GL11.GL_CULL_FACE, cull_face);
        return this;
    }
    public RenderState lightmap(float block, float sky) {
        this.lightmap = new float[] {block, sky};
        return this;
    }
    public RenderState blend(BlendMode blend) {
        this.blend = blend;
        return this;
    }
}
