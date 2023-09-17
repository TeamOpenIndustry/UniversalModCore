package cam72cam.mod.render.opengl;

import cam72cam.mod.util.With;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL32;
import util.Matrix4;

import static cam72cam.mod.render.opengl.Texture.NO_TEXTURE;

public class RenderContext {
    private RenderContext() {
    }

    public static With apply(RenderState state) {
        ShaderInstance shader = RenderSystem.getShader();
        if (state.model_view != null) {
            Matrix4 model_view = state.model_view;
            shader.MODEL_VIEW_MATRIX.set(new Matrix4f(
                    (float) model_view.m00,
                    (float) model_view.m01,
                    (float) model_view.m02,
                    (float) model_view.m03,
                    (float) model_view.m10,
                    (float) model_view.m11,
                    (float) model_view.m12,
                    (float) model_view.m13,
                    (float) model_view.m20,
                    (float) model_view.m21,
                    (float) model_view.m22,
                    (float) model_view.m23,
                    (float) model_view.m30,
                    (float) model_view.m31,
                    (float) model_view.m32,
                    (float) model_view.m33
            ));
        }
        if (state.projection != null) {
            Matrix4 projection = state.projection;
            shader.PROJECTION_MATRIX.set(new Matrix4f(
                    (float) projection.m00,
                    (float) projection.m01,
                    (float) projection.m02,
                    (float) projection.m03,
                    (float) projection.m10,
                    (float) projection.m11,
                    (float) projection.m12,
                    (float) projection.m13,
                    (float) projection.m20,
                    (float) projection.m21,
                    (float) projection.m22,
                    (float) projection.m23,
                    (float) projection.m30,
                    (float) projection.m31,
                    (float) projection.m32,
                    (float) projection.m33
            ));
        }

        if (state.texture != NO_TEXTURE) {
            shader.setSampler("Sampler0", state.texture.getId());
            // TODO normal and spec
        }

        if (state.color != null && shader.COLOR_MODULATOR != null) {
            shader.COLOR_MODULATOR.set(state.color);
        }


        shader.apply();

        if (state.blend != null) {
            return () -> state.blend.apply().run();
        }
        return () -> {};
    }

    public static void applyBool(int opt, boolean currState) {
        if (currState) {
            GL32.glEnable(opt);
        } else {
            GL32.glDisable(opt);
        }
    }
}
