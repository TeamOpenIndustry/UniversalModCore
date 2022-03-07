package cam72cam.mod.render.opengl;

import cam72cam.mod.render.OpenGL;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import util.Matrix4;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.*;

import static cam72cam.mod.render.opengl.Texture.NO_TEXTURE;

public class LegacyRenderContext implements RenderContext {
    // TODO does this break server side?
    private static final FloatBuffer fourFloatBuffer = ByteBuffer.allocateDirect(4 * 16).order(ByteOrder.nativeOrder()).asFloatBuffer();
    public static final LegacyRenderContext INSTANCE = new LegacyRenderContext();

    private LegacyRenderContext() {
    }

    public OpenGL.With apply(RenderState state) {
        List<Runnable> restore = new ArrayList<>();

        if (state.model_view != null || state.projection != null) {
            int oldMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
            restore.add(() -> GL11.glMatrixMode(oldMode));
        }
        if (state.model_view != null) {
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();
            multMatrix(state.model_view.copy().transpose());
            restore.add(() -> {
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                GL11.glPopMatrix();
            });
        }
        if (state.projection != null) {
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPushMatrix();
            multMatrix(state.projection.copy().transpose());
            restore.add(() -> {
                GL11.glMatrixMode(GL11.GL_PROJECTION);
                GL11.glPopMatrix();
            });
        }

        if (state.texture != null || state.lightmap != null || state.normals != null || state.specular != null) {
            int oldActive = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            restore.add(() -> GL13.glActiveTexture(oldActive));
        }

        if (state.lightmap != null) {
            float block = state.lightmap[0];
            float sky = state.lightmap[1];
            boolean vanillaEmissive = block == 1 && sky == 1 && ARBShaderObjects.glGetHandleARB(ARBShaderObjects.GL_PROGRAM_OBJECT_ARB) == 0;
            if (vanillaEmissive) {
                state.lighting(false);
                GL13.glActiveTexture(OpenGlHelper.lightmapTexUnit);
                boolean oldTexEnabled = GL11.glGetBoolean(GL11.GL_TEXTURE_2D);
                applyBool(GL11.GL_TEXTURE_2D, false);
                restore.add(() -> {
                    GL13.glActiveTexture(OpenGlHelper.lightmapTexUnit);
                    applyBool(GL11.GL_TEXTURE_2D, oldTexEnabled);
                });
            } else {
                int i = ((int)(sky * 15)) << 20 | ((int)(block*15)) << 4;
                int x = i % 65536;
                int y = i / 65536;
                float oldX = OpenGlHelper.lastBrightnessX;
                float oldY = OpenGlHelper.lastBrightnessY;
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, x, y);
                restore.add(() -> OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, oldX, oldY));
            }
        }

        if (state.texture != null) {
            GL13.glActiveTexture(OpenGlHelper.defaultTexUnit);
            boolean oldTexEnabled = GL11.glGetBoolean(GL11.GL_TEXTURE_2D);

            if (state.texture == NO_TEXTURE) {
                applyBool(GL11.GL_TEXTURE_2D, false);
                restore.add(() -> {
                    GL13.glActiveTexture(OpenGlHelper.defaultTexUnit);
                    applyBool(GL11.GL_TEXTURE_2D, oldTexEnabled);
                });
            } else {
                applyBool(GL11.GL_TEXTURE_2D, true);

                int oldTex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, state.texture.textureId);
                restore.add(() -> {
                    GL13.glActiveTexture(OpenGlHelper.defaultTexUnit);
                    applyBool(GL11.GL_TEXTURE_2D, oldTexEnabled);
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, oldTex);
                });
            }
        }

        if (state.normals != null) {
            // Normals
            GL13.glActiveTexture(GL13.GL_TEXTURE2);
            boolean oldNormalEnabled = GL11.glGetBoolean(GL11.GL_TEXTURE_2D);

            if (state.normals == NO_TEXTURE) {
                applyBool(GL11.GL_TEXTURE_2D, false);
                restore.add(() -> {
                    GL13.glActiveTexture(GL13.GL_TEXTURE2);
                    applyBool(GL11.GL_TEXTURE_2D, oldNormalEnabled);
                });
            } else {
                applyBool(GL11.GL_TEXTURE_2D, true);

                int oldNorm = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, state.normals.textureId);
                restore.add(() -> {
                    GL13.glActiveTexture(GL13.GL_TEXTURE2);
                    applyBool(GL11.GL_TEXTURE_2D, oldNormalEnabled);
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, oldNorm);
                });
            }
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
        }
        if (state.specular != null) {
            // Specular
            GL13.glActiveTexture(GL13.GL_TEXTURE3);
            boolean oldSpecularEnalbed = GL11.glGetBoolean(GL11.GL_TEXTURE_2D);

            if (state.specular == NO_TEXTURE) {
                applyBool(GL11.GL_TEXTURE_2D, false);
                restore.add(() -> {
                    GL13.glActiveTexture(GL13.GL_TEXTURE3);
                    applyBool(GL11.GL_TEXTURE_2D, oldSpecularEnalbed);
                });
            } else {
                applyBool(GL11.GL_TEXTURE_2D, true);

                int oldSpec = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, state.specular.textureId);
                restore.add(() -> {
                    GL13.glActiveTexture(GL13.GL_TEXTURE3);
                    applyBool(GL11.GL_TEXTURE_2D, oldSpecularEnalbed);
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, oldSpec);
                });
            }
            GL13.glActiveTexture(GL13.GL_TEXTURE0);

        }


        if (state.color != null) {
            boolean oldColorMaterial = GL11.glGetBoolean(GL11.GL_COLOR_MATERIAL);
            applyBool(GL11.GL_COLOR_MATERIAL, true);

            GL11.glGetFloat(GL11.GL_CURRENT_COLOR, fourFloatBuffer);
            float[] oldColor = new float[] {fourFloatBuffer.get(0), fourFloatBuffer.get(1), fourFloatBuffer.get(2), fourFloatBuffer.get(3)};
            GL11.glColor4f(state.color[0], state.color[1], state.color[2], state.color[3]);
            restore.add(() -> {
                GL11.glColor4f(oldColor[0], oldColor[1], oldColor[2], oldColor[3]);
                applyBool(GL11.GL_COLOR_MATERIAL, oldColorMaterial);
            });
        }

        state.bools.forEach((glId, value) -> {
            boolean oldValue = GL11.glGetBoolean(glId);
            applyBool(glId, value);
            restore.add(() -> applyBool(glId, oldValue));
        });

        if (state.smooth_shading != null) {
            int oldShading = GL11.glGetInteger(GL11.GL_SHADE_MODEL);
            GL11.glShadeModel(state.smooth_shading ? GL11.GL_SMOOTH : GL11.GL_FLAT);
            restore.add(() -> GL11.glShadeModel(oldShading));
        }

        if (state.blend != null) {
            restore.add(state.blend.apply());
        }

        Collections.reverse(restore);
        return () -> restore.forEach(Runnable::run);
    }

    private static FloatBuffer fbm = null;
    private static void multMatrix(Matrix4 matrix) {
        if (fbm == null) {
            // Can't static init since class is loaded server side
            fbm = BufferUtils.createFloatBuffer(16);
        }
        fbm.position(0);
        fbm.put(new float[]{
                (float) matrix.m00, (float) matrix.m01, (float) matrix.m02, (float) matrix.m03,
                (float) matrix.m10, (float) matrix.m11, (float) matrix.m12, (float) matrix.m13,
                (float) matrix.m20, (float) matrix.m21, (float) matrix.m22, (float) matrix.m23,
                (float) matrix.m30, (float) matrix.m31, (float) matrix.m32, (float) matrix.m33
        });
        fbm.flip();
        GL11.glMultMatrix(fbm);
    }

    public static void applyBool(int opt, boolean currState) {
        if (currState) {
            GL11.glEnable(opt);
        } else {
            GL11.glDisable(opt);
        }
    }
}
