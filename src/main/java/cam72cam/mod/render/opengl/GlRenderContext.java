package cam72cam.mod.render.opengl;

import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.common.mesh.VAOLayout;
import cam72cam.mod.render.ShaderHelper;
import cam72cam.mod.render.common.RenderContext;
import cam72cam.mod.util.With;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import util.Matrix4;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

import static cam72cam.mod.render.opengl.Texture.NO_TEXTURE;

@ApiStatus.Internal
public class GlRenderContext extends RenderContext {
    private static final ExecutorService pool = new ThreadPoolExecutor(0, Runtime.getRuntime().availableProcessors(),
            5L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            runnable -> {
                Thread thread = new Thread(runnable);
                thread.setName("UMC-GeometryLoader");
                thread.setPriority(Thread.MIN_PRIORITY);
                return thread;
            });

    private final AtomicBoolean bound = new AtomicBoolean(false);

    private static final AtomicLong nextGeomHandle = new AtomicLong();
    private final Map<String, GlModelBuffer> bindings = new HashMap<>();

    private final IntBuffer intBuffer;
    private final FloatBuffer floatBuffer;

    public GlRenderContext() {
        // free unused geometry
        ClientEvents.TICK.subscribe(() -> {
            GlRenderContext ctx = (GlRenderContext) RenderContext.get();
            long now = System.currentTimeMillis();
            for (GlModelBuffer buffer : ctx.bindings.values()) {
                if (buffer.vbo != -1 && now - buffer.lastUsed > (long) (30 * 1000)) {
                    deleteBuffer(buffer);
                }
            }
        });

        intBuffer = BufferUtils.createIntBuffer(16);
        floatBuffer = BufferUtils.createFloatBuffer(16);
    }

    @Override
    public void flushEnqueued() {
        boolean oldBlend = GL11.glGetBoolean(GL11.GL_BLEND);
        GlStateManager.disableBlend();
        super.flushEnqueued();
        if (oldBlend) {
            GlStateManager.enableBlend();
        }
    }

    @Override
    public String setGeometryUploader(Supplier<float[]> vboData, VAOLayout layout) {
        String key = "geometry-" + nextGeomHandle.getAndIncrement();
        GlModelBuffer buffer = new GlModelBuffer();
        buffer.data = vboData;
        buffer.layout = layout;
        bindings.put(key, buffer);
        return key;
    }

    @Override
    public void uploadGeometry(String handle, boolean waitForLoad) {
        GlModelBuffer buffer = bindings.get(handle);
        if (buffer == null || buffer.vbo != -1) {
            return;
        }

        if (buffer.loader == null) {
            buffer.loader = pool.submit(() -> {
                // Baked inside the lambda; only the resulting direct buffer survives.
                float[] data = buffer.data.get();
                FloatBuffer fb = GLAllocation.createDirectFloatBuffer(data.length);
                fb.put(data);
                fb.position(0);
                return fb;
            });
        }

        if (waitForLoad) {
            try {
                buffer.loader.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while loading geometry", e);
            } catch (ExecutionException e) {
                buffer.loader = null;
                throw new RuntimeException("Cannot create geometry buffer", e);
            }
        } else if (!buffer.loader.isDone()) {
            return;
        }

        try {
            FloatBuffer data = buffer.loader.get();
            int oldVbo = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
            buffer.vbo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer.vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, GL15.GL_STATIC_DRAW);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, oldVbo);
            buffer.vertexCount = data.limit() / buffer.layout.getStride();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Cannot create geometry buffer", e);
        }
        buffer.loader = null;
    }

    @Override
    public boolean isUploaded(String key) {
        GlModelBuffer buffer = bindings.get(key);
        return buffer != null && buffer.vbo != -1;
    }

    @Override
    public With bindGeometry(String handle) {
        if (bound.get()) {
            throw new IllegalStateException("UMC Model is already bound!");
        }
        if(!isUploaded(handle)) {
            throw new IllegalStateException("VBO data is used before fully loaded!");
        }

        GlModelBuffer buffer = bindings.get(handle);
        buffer.lastUsed = System.currentTimeMillis();

        bound.set(true);
        int oldVbo = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer.vbo);
        return ((With) () -> {
            GL11.glPopClientAttrib();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, oldVbo);
            bound.set(false);
        }).and(setupVertexFormat(buffer.layout));
    }

    @Override
    public int getVertexCount(String handle) {
        return bindings.containsKey(handle) ? bindings.get(handle).vertexCount : -1;
    }

    @Override
    public void drawGeometry(String key, int first, int count) {
        GL11.glDrawArrays(GL11.GL_TRIANGLES, first, count);
    }

    @Override
    public void freeGeometry(String key) {
        deleteBuffer(bindings.get(key));
    }

    @Override
    public void deleteGeometry(String key) {
        deleteBuffer(bindings.remove(key));
    }

    private static void deleteBuffer(GlModelBuffer buffer) {
        if (buffer == null) {
            return;
        }
        if (buffer.vbo != -1) {
            GL15.glDeleteBuffers(buffer.vbo);
            buffer.vbo = -1;
        }
        buffer.loader = null;
    }

    @Override
    protected With setupVertexFormat(VAOLayout layout) {
        layout.setup();
        return layout::restore;
    }

    @Override
    public With apply(RenderState state) {
        List<Runnable> restore = new ArrayList<>();

        if (state.model_view != null || state.projection != null) {
            //int oldMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
            //restore.add(() -> GL11.glMatrixMode(oldMode));
            // Sane default and removes a few % on render overhead
            restore.add(() -> GL11.glMatrixMode(GL11.GL_MODELVIEW));
        }
        if (state.model_view != null) {
            GL11.glPushMatrix();
            multMatrix(state.model_view.copy().transpose());
            restore.add(GL11::glPopMatrix);
        }
        if (state.projection != null) {
            // Since we use the projection matrix so little, we assume that we are always defaulted to MODELVIEW
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPushMatrix();
            multMatrix(state.projection.copy().transpose());
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            restore.add(() -> {
                GL11.glMatrixMode(GL11.GL_PROJECTION);
                GL11.glPopMatrix();
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
            });
        }

        if (state.texture != null || state.lightmap != null || state.normals != null || state.specular != null) {
            int oldActive = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            restore.add(() -> GL13.glActiveTexture(oldActive));
        }

        if (state.shader != null) {
            With bound = state.shader.bind();
            restore.add(bound::restore);
        }

        boolean shaderActive = ShaderHelper.isShaderPackEnabled();

        if (state.lightmap != null) {
            float block = state.lightmap[0];
            float sky = state.lightmap[1];
            boolean vanillaEmissive = block == 1 && sky == 1 && !shaderActive;
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
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, state.texture.getId());
                restore.add(() -> {
                    GL13.glActiveTexture(OpenGlHelper.defaultTexUnit);
                    applyBool(GL11.GL_TEXTURE_2D, oldTexEnabled);
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, oldTex);
                });
            }
        }

        if (shaderActive) {

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
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, state.normals.getId());
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
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, state.specular.getId());
                    restore.add(() -> {
                        GL13.glActiveTexture(GL13.GL_TEXTURE3);
                        applyBool(GL11.GL_TEXTURE_2D, oldSpecularEnalbed);
                        GL11.glBindTexture(GL11.GL_TEXTURE_2D, oldSpec);
                    });
                }
                GL13.glActiveTexture(GL13.GL_TEXTURE0);

            }
        }

        if (state.color != null) {
            boolean oldColorMaterial = GL11.glGetBoolean(GL11.GL_COLOR_MATERIAL);
            applyBool(GL11.GL_COLOR_MATERIAL, true);

            floatBuffer.clear();
            GL11.glGetFloat(GL11.GL_CURRENT_COLOR, floatBuffer);
            float oldR = floatBuffer.get(0), oldG = floatBuffer.get(1), oldB = floatBuffer.get(2), oldA = floatBuffer.get(3);
            GL11.glColor4f(state.color[0], state.color[1], state.color[2], state.color[3]);

            restore.add(() -> {
                GL11.glColor4f(oldR, oldG, oldB, oldA);
                applyBool(GL11.GL_COLOR_MATERIAL, oldColorMaterial);
            });
        }

        if (state.lighting != null) {
            boolean oldValue = GL11.glGetBoolean(GL11.GL_LIGHTING);
            applyBool(GL11.GL_LIGHTING, state.lighting);
            restore.add(() -> applyBool(GL11.GL_LIGHTING, oldValue));
        }

        if (state.alpha_test != null) {
            boolean oldValue = GL11.glGetBoolean(GL11.GL_ALPHA_TEST);
            applyBool(GL11.GL_ALPHA_TEST, state.alpha_test);
            restore.add(() -> applyBool(GL11.GL_ALPHA_TEST, oldValue));
        }

        if (state.depth_test != null) {
            boolean oldValue = GL11.glGetBoolean(GL11.GL_DEPTH_TEST);
            applyBool(GL11.GL_DEPTH_TEST, state.depth_test);
            restore.add(() -> applyBool(GL11.GL_DEPTH_TEST, oldValue));
        }

        if (state.rescale_normal != null) {
            boolean oldValue = GL11.glGetBoolean(GL12.GL_RESCALE_NORMAL);
            applyBool(GL12.GL_RESCALE_NORMAL, state.rescale_normal);
            restore.add(() -> applyBool(GL12.GL_RESCALE_NORMAL, oldValue));
        }

        if (state.cull_face != null) {
            boolean oldValue = GL11.glGetBoolean(GL11.GL_CULL_FACE);
            applyBool(GL11.GL_CULL_FACE, state.cull_face);
            restore.add(() -> applyBool(GL11.GL_CULL_FACE, oldValue));
        }

        if (state.depth_mask != null) {
            boolean oldDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            GL11.glDepthMask(state.depth_mask);
            restore.add(() -> GL11.glDepthMask(oldDepthMask));
        }

        if (state.smooth_shading != null) {
            int oldShading = GL11.glGetInteger(GL11.GL_SHADE_MODEL);
            GL11.glShadeModel(state.smooth_shading ? GL11.GL_SMOOTH : GL11.GL_FLAT);
            restore.add(() -> GL11.glShadeModel(oldShading));
        }

        if (state.scissor_test != null) {
            boolean oldValue = GL11.glGetBoolean(GL11.GL_SCISSOR_TEST);
            applyBool(GL11.GL_SCISSOR_TEST, state.scissor_test);
            if (state.scissor_test && state.scissor_range != null) {
                int scaleFactor = new ScaledResolution(Minecraft.getMinecraft()).getScaleFactor();
                int screenHeight = GUIHelpers.getScreenHeight() * scaleFactor;

                int x = (int) state.scissor_range.getMinX() * scaleFactor;
                int y = (int) state.scissor_range.getMinY() * scaleFactor;
                int width = (int) state.scissor_range.getWidth() * scaleFactor;
                int height = (int) state.scissor_range.getHeight() * scaleFactor;

                intBuffer.clear();
                GL11.glGetInteger(GL11.GL_SCISSOR_BOX, intBuffer);
                int oldX = intBuffer.get(0), oldY = intBuffer.get(1), oldWidth = intBuffer.get(2), oldHeight = intBuffer.get(3);
                restore.add(() -> GL11.glScissor(oldX, oldY, oldWidth, oldHeight));

                //We set origin point at Top-Left corner but OpenGL takes Bottom-Left corner, so wraps y
                GL11.glScissor(x, screenHeight - y - height, width, height);
            }
            restore.add(() -> applyBool(GL11.GL_SCISSOR_TEST, oldValue));
        }

        if (state.blend != null) {
            restore.add(state.blend.apply());
        }

        Collections.reverse(restore);
        return () -> restore.forEach(Runnable::run);
    }

    @Override
    protected @Nullable ToDoubleFunction<Vec3d> getDepthSorter() {
        intBuffer.clear();
        GL11.glGetInteger(GL11.GL_VIEWPORT, intBuffer);
        if (intBuffer.get(2) <= 0 || intBuffer.get(3) <= 0) {
            return null;
        }

        // Snapshot the matrix
        floatBuffer.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, floatBuffer);
        float[] m = new float[16];
        floatBuffer.rewind();
        floatBuffer.get(m);

        return pos -> -(m[2] * pos.x + m[6] * pos.y + m[10] * pos.z);
    }

    public static void applyBool(int opt, boolean currState) {
        if (currState) {
            GL11.glEnable(opt);
        } else {
            GL11.glDisable(opt);
        }
    }

    private void multMatrix(Matrix4 matrix) {
        floatBuffer.position(0);
        floatBuffer.put(new float[]{
                (float) matrix.m00, (float) matrix.m01, (float) matrix.m02, (float) matrix.m03,
                (float) matrix.m10, (float) matrix.m11, (float) matrix.m12, (float) matrix.m13,
                (float) matrix.m20, (float) matrix.m21, (float) matrix.m22, (float) matrix.m23,
                (float) matrix.m30, (float) matrix.m31, (float) matrix.m32, (float) matrix.m33
        });
        floatBuffer.flip();
        GL11.glMultMatrix(floatBuffer);
    }

    private static final class GlModelBuffer {
        int vbo = -1;
        int vertexCount = -1;
        long lastUsed = System.currentTimeMillis();
        Supplier<float[]> data;
        VAOLayout layout;
        Future<FloatBuffer> loader;
    }
}
