package cam72cam.mod.render.opengl;

import cam72cam.mod.ModCore;
import cam72cam.mod.util.With;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL32;
import util.Matrix4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static cam72cam.mod.render.opengl.Texture.NO_TEXTURE;

public class RenderContext {
    private RenderContext() {
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

    public static void checkError(String identifier) {
        int err = GL32.glGetError();
        if (err != 0) {
            ModCore.error("We broke something at %s: %s", identifier, err);
        }
    }

    public static With apply(RenderState state) {
        RenderContext.checkError("start of render context apply");
        List<Runnable> restore = new ArrayList<>();

        ShaderInstance shader = RenderSystem.getShader();
        //PoseStack posestack = RenderSystem.getModelViewStack();
        //posestack.pushPose();
        //restore.add(() -> RenderSystem.getModelViewStack().popPose());
        if (state.model_view != null) {
            Matrix4f oldModelView = RenderSystem.getModelViewMatrix().copy();
            restore.add(() -> RenderSystem.getModelViewMatrix().load(oldModelView));

            Matrix4 model_view = state.model_view;
            Matrix4f target = model_view.toMojMatrix4f();
            /*Matrix4f target = new Matrix4f(new float[]{
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
            });*/
            //posestack.mulPoseMatrix(target);
            shader.MODEL_VIEW_MATRIX.set(target);
            RenderSystem.getModelViewMatrix().load(target);
        }
        if (state.projection != null) {
            Matrix4f oldProjection = RenderSystem.getProjectionMatrix().copy();
            restore.add(() -> RenderSystem.getProjectionMatrix().load(oldProjection));

            Matrix4 projection = state.projection;
            Matrix4f target = projection.toMojMatrix4f();
            /*Matrix4f target = new Matrix4f(new float[]{
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
            });*/
            //posestack.mulPoseMatrix(target);
            shader.PROJECTION_MATRIX.set(target);
            RenderSystem.getProjectionMatrix().load(target);
        }
        //RenderSystem.applyModelViewMatrix();
        //restore.add(RenderSystem::applyModelViewMatrix);

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

        if(state.lightmap != null) {

        }

        // TODO 1.17.1
        checkError("before bools");
        //TODO this is broken, we're probably trying to use deprecated features again
        /*state.bools.forEach((glId, value) -> {
            boolean oldValue = GL11.glGetBoolean(glId);
            applyBool(glId, value);
            restore.add(() -> applyBool(glId, oldValue));
        });*/
        checkError("after bools");
        if (state.depth_mask != null) {
            boolean oldDepthMask = GL32.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            GL32.glDepthMask(state.depth_mask);
            restore.add(() -> GL32.glDepthMask(oldDepthMask));
        }
        //TODO should we be doing this here? set up a proper shader and apply that where appropriate
        if (state.smooth_shading != null) {
            //int oldShading = GL11.glGetInteger(GL11.GL_SHADE_MODEL);
            //GL11.glShadeModel(state.smooth_shading ? GL11.GL_SMOOTH : GL11.GL_FLAT);
            //restore.add(() -> GL11.glShadeModel(oldShading));
        }

        if (state.blend != null) {
            state.blend.apply();
            restore.add(() -> {
                state.blend.apply().run();
            });
        }

        shader.apply();
        //restore.add(() -> RenderSystem.getShader().close());
        checkError("end of render context apply");

        //Collections.reverse(restore);//this may or may not be needed
        return (With)() -> restore.forEach(Runnable::run);
    }

}
