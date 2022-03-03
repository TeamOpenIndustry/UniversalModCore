package cam72cam.mod.render;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.opengl.BlendMode;
import cam72cam.mod.render.opengl.LegacyRenderContext;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.Texture;
import cam72cam.mod.resource.Identifier;
import org.lwjgl.opengl.GL11;
import util.Matrix4;

import java.util.function.Consumer;

/**
 * Optional, but recommended OpenGL helper library.
 *
 * Allows common GL operations to be safely wrapped in try blocks to prevent GFX bugs from early returns/exceptions
 */
public class OpenGL {
    private OpenGL() {}

    public static class RenderContext implements AutoCloseable {
        public static final int NO_TEXTURE = -1;

        private final RenderState state;
        private final Runnable restore;

        public RenderContext() {
            state = new RenderState();
            this.restore = () -> {};
        }

        public RenderContext(Runnable restore) {
            this.state = new RenderState();
            this.restore = restore;
        }

        private RenderContext(RenderContext ctx) {
            this.state = ctx.state.clone();
            this.restore = () -> {};
        }

        public RenderContext copy() {
            return new RenderContext(this);
        }

        public void apply(Runnable fn) {
            apply((c) -> fn.run());
        }

        public void apply(Consumer<RenderContext> fn) {
            try (RenderContext ctx = apply()) {
                fn.accept(apply());
            }
        }

        public RenderContext apply() {
            With applied = LegacyRenderContext.INSTANCE.apply(state);
            return new RenderContext(applied::restore);
        }

        @Override
        public void close() {
            if (restore != null) {
                restore.run();
            }
        }

        public RenderContext color(float r, float g, float b, float a) {
            this.state.color(r, g, b, a);
            return this;
        }

        public Matrix4 model_view() {
            return state.model_view();
        }

        public RenderContext translate(Vec3d vec) {
            state.translate(vec);
            return this;
        }
        public RenderContext translate(double x, double y, double z) {
            state.translate(x, y, z);
            return this;
        }
        public RenderContext scale(Vec3d vec) {
            state.scale(vec);
            return this;
        }
        public RenderContext scale(double x, double y, double z) {
            state.scale(x, y, z);
            return this;
        }
        public RenderContext rotate(double degrees, double x, double y, double z) {
            state.rotate(degrees, x, y, z);
            return this;
        }

        public RenderContext texture(Identifier tex) {
            state.texture(new Texture(tex));
            return this;
        }
        public RenderContext texture(int tex) {
            state.texture(new Texture(tex));
            return this;
        }

        public RenderContext lighting(boolean lighting) {
            state.lighting(lighting);
            return this;
        }
        public RenderContext alpha_test(boolean alpha_test) {
            state.alpha_test(alpha_test);
            return this;
        }
        public RenderContext depth_test(boolean depth_test) {
            state.depth_test(depth_test);
            return this;
        }
        public RenderContext smooth_shading(boolean smooth_shading) {
            state.smooth_shading(smooth_shading);
            return this;
        }
        public RenderContext rescale_normal(boolean rescale_normal) {
            state.rescale_normal(rescale_normal);
            return this;
        }
        public RenderContext cull_face(boolean cull_face) {
            state.cull_face(cull_face);
            return this;
        }
        public RenderContext lightmap(float block, float sky) {
            state.lightmap(block, sky);
            return this;
        }
        public RenderContext blend(BlendMode blend) {
            state.blend(blend);
            return this;
        }
    }

    public static void applyBool(int opt, boolean currState) {
        if (currState) {
            GL11.glEnable(opt);
        } else {
            GL11.glDisable(opt);
        }
    }

    /*
    private static With transparency(float r, float g, float b, float a) {
        With blend = blend(GL11.GL_CONSTANT_ALPHA, GL11.GL_ONE);
        FloatBuffer orig = ByteBuffer.allocateDirect(4 * 16).order(ByteOrder.nativeOrder()).asFloatBuffer();
        GL11.glGetFloat(GL14.GL_BLEND_COLOR, orig);
        GL14.glBlendColor(r,g,b,a);
        return () -> {
            GL14.glBlendColor(orig.get(0), orig.get(1), orig.get(2), orig.get(3));
            blend.restore();
        };
    }
     */

    public static int allocateTexture() {
        // Allows us to set some parameters that cause issues in newer MC versions
        return GL11.glGenTextures();
    }

    @FunctionalInterface
    public interface With extends AutoCloseable {
        default void close() {
            restore();
        }
        void restore();

        default With and(With other) {
            return () -> {
                this.close();
                other.close();
            };
        }
    }
}
