package cam72cam.mod.render.opengl;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.OptiFine;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.util.Lazy;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import util.Matrix4;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import javax.annotation.Nullable;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class RenderState {
    protected Matrix4 model_view = null;
    protected Matrix4 projection = null;
    protected Texture texture = null;
    protected Texture normals = null;
    protected Texture specular = null;
    protected float[] color = null;

    @Deprecated(since = "mc1.17")
    protected Boolean lighting = null;
    @Deprecated(since = "mc1.17")
    protected Boolean alpha_test = null;
    protected Boolean depth_test = null;
    @Deprecated(since = "mc1.17")
    protected Boolean rescale_normal = null;
    protected Boolean cull_face = null;

    protected Boolean depth_mask;
    protected Boolean smooth_shading = null;
    protected Boolean scissor_test = null;
    protected Rectangle2D scissor_range = null;
    protected float[] lightmap = null;
    protected BlendMode blend = null;
    protected OptiFine.Shaders shader;
    protected RenderContext.Stage stage = RenderContext.Stage.NONE;

    private static float[] mbuf = new float[16];

    //Avoid potential server-side load
    private static final Lazy<Consumer<RenderState>> clientInitializer = Lazy.of(() -> (state) -> {
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        if(!RenderSystem.isOnRenderThread()) {
            return;
        }

        RenderSystem.getModelViewMatrix().get(mbuf);
        state.model_view = new Matrix4(
                mbuf[0],
                mbuf[1],
                mbuf[2],
                mbuf[3],
                mbuf[4],
                mbuf[5],
                mbuf[6],
                mbuf[7],
                mbuf[8],
                mbuf[9],
                mbuf[10],
                mbuf[11],
                mbuf[12],
                mbuf[13],
                mbuf[14],
                mbuf[15]
        ).transpose();

        //Read-only
        ByteBuffer buffer = encoder.mapBuffer(RenderSystem.getProjectionMatrixBuffer().buffer(), true, false).data();
        buffer.asFloatBuffer().get(mbuf);
        state.projection = new Matrix4(
                mbuf[0],
                mbuf[1],
                mbuf[2],
                mbuf[3],
                mbuf[4],
                mbuf[5],
                mbuf[6],
                mbuf[7],
                mbuf[8],
                mbuf[9],
                mbuf[10],
                mbuf[11],
                mbuf[12],
                mbuf[13],
                mbuf[14],
                mbuf[15]
        ).transpose();
    });

    public RenderState() {
        if (FMLEnvironment.dist.isClient() && RenderSystem.isOnRenderThread()) {
            clientInitializer.get().accept(this);
        }
    }

    public RenderState(PoseStack stack) {
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        Matrix4f tmp = new Matrix4f(RenderSystem.getModelViewMatrix());
        tmp.mul(stack.last().pose());
        tmp.get(mbuf);
        this.model_view = new Matrix4(
                mbuf[0],
                mbuf[1],
                mbuf[2],
                mbuf[3],
                mbuf[4],
                mbuf[5],
                mbuf[6],
                mbuf[7],
                mbuf[8],
                mbuf[9],
                mbuf[10],
                mbuf[11],
                mbuf[12],
                mbuf[13],
                mbuf[14],
                mbuf[15]
        ).transpose();

        ByteBuffer buffer = encoder.mapBuffer(RenderSystem.getProjectionMatrixBuffer().buffer(), true, false).data();
        buffer.asFloatBuffer().get(mbuf);
        this.projection = new Matrix4(
                mbuf[0],
                mbuf[1],
                mbuf[2],
                mbuf[3],
                mbuf[4],
                mbuf[5],
                mbuf[6],
                mbuf[7],
                mbuf[8],
                mbuf[9],
                mbuf[10],
                mbuf[11],
                mbuf[12],
                mbuf[13],
                mbuf[14],
                mbuf[15]
        ).transpose();
    }

    private RenderState(RenderState ctx) {
        this.model_view = ctx.model_view != null ? ctx.model_view.copy() : null;
        this.projection = ctx.projection != null ? ctx.projection.copy() : null;
        this.texture = ctx.texture;
        this.normals = ctx.normals;
        this.specular = ctx.specular;
        this.color = ctx.color != null ? ctx.color.clone() : null;

        this.lighting = ctx.lighting;
        this.alpha_test = ctx.alpha_test;
        this.depth_test = ctx.depth_test;
        this.rescale_normal = ctx.rescale_normal;
        this.cull_face = ctx.cull_face;

        this.depth_mask = ctx.depth_mask;
        this.smooth_shading = ctx.smooth_shading;
        this.scissor_test = ctx.scissor_test;
        this.scissor_range = ctx.scissor_range != null ? ctx.scissor_range.getBounds2D() : null;
        this.lightmap = ctx.lightmap != null ? ctx.lightmap.clone() : null;
        this.blend = ctx.blend;
        this.shader = ctx.shader;
        this.stage = ctx.stage;
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
    public RenderState scale(double factor){
        return this.scale(factor, factor, factor);
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

    public RenderState normals(Texture norm) {
        this.normals = norm;
        return this;
    }

    public Texture getNormals() {
        return normals;
    }

    public RenderState specular(Texture spec) {
        this.specular = spec;
        return this;
    }

    public Texture getSpecular() {
        return specular;
    }

    public RenderState lighting(boolean lighting) {
        this.lighting = lighting;
        return this;
    }
    public RenderState alpha_test(boolean alpha_test) {
        this.alpha_test = alpha_test;
        return this;
    }
    public RenderState depth_test(boolean depth_test) {
        this.depth_test = depth_test;
        return this;
    }
    public RenderState scissor(boolean scissor, @Nullable Rectangle2D range) {
        this.scissor_test = scissor;
        this.scissor_range = range;
        return this;
    }
    public RenderState depth_mask(boolean depth_mask) {
        this.depth_mask = depth_mask;
        return this;
    }
    public RenderState smooth_shading(boolean smooth_shading) {
        this.smooth_shading = smooth_shading;
        return this;
    }
    public RenderState rescale_normal(boolean rescale_normal) {
        this.rescale_normal = rescale_normal;
        return this;
    }
    public RenderState cull_face(boolean cull_face) {
        this.cull_face = cull_face;
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
    public RenderState shader(OptiFine.Shaders shader) {
        this.shader = shader;
        return this;
    }
    public RenderState stage(RenderContext.Stage stage) {
        this.stage = stage != null ? stage : RenderContext.Stage.NONE;
        return this;
    }
    public RenderContext.Stage getStage() {
        return this.stage;
    }
}