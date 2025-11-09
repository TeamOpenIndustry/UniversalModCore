package cam72cam.mod.render.opengl;

import cam72cam.mod.ModCore;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.util.With;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL32;

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
        if (state.model_view != null) {
            Matrix4f oldModelView = RenderSystem.getModelViewMatrix().copy();
            restore.add(() -> RenderSystem.getModelViewMatrix().load(oldModelView));
            Matrix4f target = state.model_view.convertToMoj();
            RenderSystem.getModelViewMatrix().load(target);
        }

        if (state.projection != null) {
            Matrix4f oldProjection = RenderSystem.getProjectionMatrix().copy();
            restore.add(() -> RenderSystem.getProjectionMatrix().load(oldProjection));
            Matrix4f target = state.projection.convertToMoj();
            RenderSystem.getProjectionMatrix().load(target);
        }

        if (state.texture != NO_TEXTURE && state.texture != null) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, state.texture.getId());
            // TODO normal and spec
            int oldTexture = RenderSystem.getShaderTexture(0);
            restore.add(() -> RenderSystem.setShaderTexture(0, oldTexture));
            RenderSystem.setShaderTexture(0, state.texture.getId());
        }

        {
            float[] color = state.color;
            if (color == null) {
                color = new float[]{1.0F, 1.0F, 1.0F, 1.0F};
            }
            float[] oldColor = RenderSystem.getShaderColor();
            RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
            restore.add(() -> RenderSystem.setShaderColor(oldColor[0], oldColor[1], oldColor[2], oldColor[3]));
        }
        /* TODO 1.17.1
        state.bools.forEach((glId, value) -> {
            boolean oldValue = GL11.glGetBoolean(glId);
            applyBool(glId, value);
            restore.add(() -> applyBool(glId, oldValue));
        });
        if (state.depth_mask != null) {
            boolean oldDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            GL11.glDepthMask(state.depth_mask);
            restore.add(() -> GL11.glDepthMask(oldDepthMask));
        }

        if (state.smooth_shading != null) {
            int oldShading = GL11.glGetInteger(GL11.GL_SHADE_MODEL);
            GL11.glShadeModel(state.smooth_shading ? GL11.GL_SMOOTH : GL11.GL_FLAT);
            restore.add(() -> GL11.glShadeModel(oldShading));
        }*/

        if(state.bools.containsKey(GL11.GL_CULL_FACE)) {
            boolean olcState = GL11.glGetBoolean(GL11.GL_CULL_FACE);
            if(state.bools.get(GL11.GL_CULL_FACE)) {
                RenderSystem.enableCull();
            } else {
                RenderSystem.disableCull();
            }
            restore.add(() -> {
                if(olcState) {
                    RenderSystem.enableCull();
                } else {
                    RenderSystem.disableCull();
                }
            });
        }

        if(state.bools.containsKey(GL11.GL_DEPTH_TEST)) {
            boolean olcState = GL11.glGetBoolean(GL11.GL_DEPTH_TEST);
            if(state.bools.get(GL11.GL_DEPTH_TEST)) {
                RenderSystem.enableDepthTest();
            } else {
                RenderSystem.disableDepthTest();
            }
            restore.add(() -> {
                if(olcState) {
                    RenderSystem.enableDepthTest();
                } else {
                    RenderSystem.disableDepthTest();
                }
            });
        }

        if(state.depth_mask != null) {
            //TODO overlapping cloud
            RenderSystem.depthMask(state.depth_mask);
            restore.add(() -> RenderSystem.depthMask(true));
        }

        if (state.blend != null) {
            restore.add(state.blend.apply());
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

        //TODO Better lighting
        Matrix4f matrix4f = new Matrix4f();
        matrix4f.setIdentity();
        Lighting.setupLevel(matrix4f);
        applyShaderFields(shader);

        shader.apply();
        restore.add(shader::clear);
        checkError();
        return () -> restore.forEach(Runnable::run);
    }

    private static void applyShaderFields(ShaderInstance shader) {
        if (shader.MODEL_VIEW_MATRIX != null) {
            shader.MODEL_VIEW_MATRIX.set(RenderSystem.getModelViewMatrix());
        }

        if (shader.PROJECTION_MATRIX != null) {
            shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
        }

        if (shader.COLOR_MODULATOR != null) {
            shader.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
        }

        for (int i = 0; i < 8; ++i) {
            int o = RenderSystem.getShaderTexture(i);
            shader.setSampler("Sampler" + i, o);
        }

        if (shader.FOG_START != null) {
            shader.FOG_START.set(RenderSystem.getShaderFogStart());
        }

        if (shader.FOG_END != null) {
            shader.FOG_END.set(RenderSystem.getShaderFogEnd());
        }

        if (shader.FOG_COLOR != null) {
            shader.FOG_COLOR.set(RenderSystem.getShaderFogColor());
        }

        if (shader.TEXTURE_MATRIX != null) {
            shader.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
        }

        if (shader.GAME_TIME != null) {
            shader.GAME_TIME.set(RenderSystem.getShaderGameTime());
        }

        if (shader.SCREEN_SIZE != null) {
            Window window = Minecraft.getInstance().getWindow();
            shader.SCREEN_SIZE.set((float)window.getWidth(), (float)window.getHeight());
        }

        RenderSystem.setupShaderLights(shader);
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
