package cam72cam.mod.render.opengl;

import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.ModCore;
import cam72cam.mod.util.With;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL32;
import util.Matrix4;

import java.util.ArrayList;
import java.util.List;

import static cam72cam.mod.render.opengl.Texture.NO_TEXTURE;

public class RenderContext {
    private RenderContext() {
    }

    public static With apply(RenderState state) {
        RenderContext.checkError();
        List<Runnable> restore = new ArrayList<>();

        ShaderInstance shader = RenderSystem.getShader();
        restore.add(applyTransform(shader, state));
        Minecraft mc = Minecraft.getInstance();
        mc.gameRenderer.overlayTexture().setupOverlayColor();
        restore.add(() -> mc.gameRenderer.overlayTexture().teardownOverlayColor());

        if (state.texture != NO_TEXTURE && state.texture != null) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, state.texture.getId());
            shader.setSampler("Sampler0", state.texture.getId());
            // TODO normal and spec
            int oldTexture = RenderSystem.getShaderTexture(0);
            restore.add(() -> RenderSystem.setShaderTexture(0, oldTexture));
            RenderSystem.setShaderTexture(0, state.texture.getId());
        }

        if (state.color != null && shader.COLOR_MODULATOR != null) {
            shader.COLOR_MODULATOR.set(state.color);
            float[] oldColor = RenderSystem.getShaderColor();

            RenderSystem.setShaderColor(state.color[0], state.color[1], state.color[2], state.color[3]);
            restore.add(() -> RenderSystem.setShaderColor(oldColor[0], oldColor[1], oldColor[2], oldColor[3]));
        }

        if(state.bools.containsKey(GL11.GL_CULL_FACE)) {
            restore.add(applyCull(state.bools.get(GL11.GL_CULL_FACE)));
        }

        if(state.bools.containsKey(GL11.GL_DEPTH_TEST)) {
            restore.add(applyDepthTest(state.bools.get(GL11.GL_DEPTH_TEST)));
        }

        if (state.blend != null) {
            restore.add(applyBlend(state));
        }

        if(state.scissorRange != null){
            int scaleFactor = (int) Minecraft.getInstance().getWindow().getGuiScale();
            int screenHeight = GUIHelpers.getScreenHeight() * scaleFactor;

            int x = (int) state.scissorRange.getMinX() * scaleFactor;
            int y = (int) state.scissorRange.getMinY() * scaleFactor;
            int width = (int) state.scissorRange.getWidth() * scaleFactor;
            int height = (int) state.scissorRange.getHeight() * scaleFactor;

            //We set origin point at Top-Left corner but OpenGL takes Bottom-Left corner, so wraps y
            RenderSystem.enableScissor(x, screenHeight - y - height, width, height);
            restore.add(RenderSystem::disableScissor);
        }


        shader.apply();
        checkError();
        return () -> restore.forEach(Runnable::run);
    }

    private static Runnable applyTransform(ShaderInstance shader, RenderState state) {
        List<Runnable> restore = new ArrayList<>();
        if (state.model_view != null) {
            Matrix4f oldModelView = RenderSystem.getModelViewMatrix().copy();
            restore.add(() -> RenderSystem.getModelViewMatrix().load(oldModelView));
            Matrix4 model_view = state.model_view;
            Matrix4f target = new Matrix4f(new float[]{
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
            });
            shader.MODEL_VIEW_MATRIX.set(target);
            RenderSystem.getModelViewMatrix().load(target);
        }
        if (state.projection != null) {
            Matrix4f oldProjection = RenderSystem.getProjectionMatrix().copy();
            restore.add(() -> RenderSystem.getProjectionMatrix().load(oldProjection));
            Matrix4 projection = state.projection;
            Matrix4f target = new Matrix4f(new float[]{
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
            });
            shader.PROJECTION_MATRIX.set(target);
            RenderSystem.getProjectionMatrix().load(target);
        }
        return () -> restore.forEach(Runnable::run);
    }

    private static Runnable applyCull(boolean newState) {
        boolean oldState = GL11.glGetBoolean(GL11.GL_CULL_FACE);
        if(newState) {
            RenderSystem.enableCull();
        } else {
            RenderSystem.disableCull();
        }
        return () -> {
            if(oldState) {
                RenderSystem.enableCull();
            } else {
                RenderSystem.disableCull();
            }
        };
    }

    private static Runnable applyDepthTest(boolean newState) {
        boolean oldState = GL11.glGetBoolean(GL11.GL_DEPTH_TEST);
        if(newState) {
            RenderSystem.enableDepthTest();
        } else {
            RenderSystem.disableDepthTest();
        }
        return () -> {
            if(oldState) {
                RenderSystem.enableDepthTest();
            } else {
                RenderSystem.disableDepthTest();
            }
        };
    }

    private static Runnable applyBlend(RenderState state) {
        return state.blend.apply();
    }

    public static void applyBool(int opt, boolean currState) {
        if (currState) {
            GL32.glEnable(opt);
        } else {
            GL32.glDisable(opt);
        }
    }


    public static void checkError() {
        int err = GL32.glGetError();
        if (err != 0) {
            ModCore.error("We broke something: %s", err);
        }
    }
}
