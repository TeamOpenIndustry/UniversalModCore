package cam72cam.mod.render.opengl;

import cam72cam.mod.model.obj.VertexBuffer;
import cam72cam.mod.render.GLSLShader;
import cam72cam.mod.render.OpenGL;
import cam72cam.mod.resource.Identifier;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class VBO {
    private final Supplier<VertexBuffer> buffer;
    private final Consumer<RenderState> settings;
    private GLSLShader shader = null;

    //private int vao;
    private int vbo;
    private int length;
    private VertexBuffer vbInfo;
    private boolean useModern = false;

    public VBO(Supplier<VertexBuffer> buffer, Consumer<RenderState> settings) {
        this.buffer = buffer;
        this.vbo = -1;
        this.settings = settings;
    }

    private void init() {
        if (shader == null) {
            this.shader = new GLSLShader(
                    new Identifier("universalmodcore:shaders/std.vert"),
                    new Identifier("universalmodcore:shaders/std.frag")
            );
        }

        VertexBuffer vb = buffer.get();
        this.length = vb.data.length / (vb.stride);
        this.vbInfo = new VertexBuffer(0, vb.hasNormals);
        ByteBuffer buffer = ByteBuffer.allocateDirect(vb.data.length * Float.BYTES).order(ByteOrder.nativeOrder());
        buffer.asFloatBuffer().put(vb.data);

        //int oldVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int oldVbo = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);

        //vao = GL30.glGenVertexArrays();
        //GL30.glBindVertexArray(vao);

        vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);

        //GL30.glBindVertexArray(oldVao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, oldVbo);
    }

    public Binding bind(RenderState state) {
        return new Binding(state);
    }

    public class Binding implements OpenGL.With {
        private final RenderState state;
        private OpenGL.With restore;

        protected Binding(RenderState state) {
            if (vbo == -1) {
                init();
            }

            this.state = state.clone();
            settings.accept(state);

            if (useModern) {
                modern(state);
            } else {
                legacy(state);
            }
        }

        protected void legacy(RenderState state) {
            int oldVbo = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
            GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT);

            this.restore = LegacyRenderContext.INSTANCE.apply(state).and(() -> {
                GL11.glPopClientAttrib();
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, oldVbo);
            });


            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
            if (vbInfo.hasNormals) {
                GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
            } else {
                GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
            }

            //GL11.glColor4f(1, 1, 1, 1);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

            int stride = vbInfo.stride * Float.BYTES;
            GL11.glVertexPointer(3, GL11.GL_FLOAT, stride, (long) vbInfo.vertexOffset * Float.BYTES);
            GL11.glTexCoordPointer(2, GL11.GL_FLOAT, stride, (long) vbInfo.textureOffset * Float.BYTES);
            GL11.glColorPointer(4, GL11.GL_FLOAT, stride, (long) vbInfo.colorOffset * Float.BYTES);
            if (vbInfo.hasNormals) {
                GL11.glNormalPointer(GL11.GL_FLOAT, stride, (long) vbInfo.normalOffset * Float.BYTES);
            }
        }

        protected void modern(RenderState state) {
            checkError("BIND");
            //int oldVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
            int oldVbo = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
            GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT);
            int oldTex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, state.texture.textureId);
            restore = shader.bind().and(() -> {
                GL11.glPopClientAttrib();
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, oldVbo);
                //GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, oldVao);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, oldTex);
            });

            checkError("SETUP0");

            //GL30.glBindVertexArray(vao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

            int stride = vbInfo.stride * Float.BYTES;
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, stride, vbInfo.vertexOffset * Float.BYTES);
            GL20.glEnableVertexAttribArray(2);
            GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, stride, vbInfo.textureOffset * Float.BYTES);
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 4, GL11.GL_FLOAT, false, stride, vbInfo.colorOffset * Float.BYTES);
            if (vbInfo.hasNormals) {
                GL20.glEnableVertexAttribArray(3);
                GL20.glVertexAttribPointer(3, 3, GL11.GL_FLOAT, false, stride, vbInfo.normalOffset * Float.BYTES);
            } else {
                GL20.glDisableVertexAttribArray(3);
            }

            checkError("SETUP1");

            shader.paramInt("Sampler0", OpenGlHelper.defaultTexUnit - GL13.GL_TEXTURE0);
            shader.paramInt("Sampler1", OpenGlHelper.lightmapTexUnit - GL13.GL_TEXTURE0);
            restore = restore.and(sync(state));
            checkError("SETUP2");
        }

        @Override
        public void restore() {
            checkError("UNBIND");
            restore.close();
            checkError("RESTORE");
        }

        private void checkError(String label) {
            int err = GL11.glGetError();
            while (err != GL11.GL_NO_ERROR) {
                System.out.println("GL ---> " + label + ":" + err);
                err = GL11.glGetError();
            }
        }

        private OpenGL.With sync(RenderState state) {
            float x = OpenGlHelper.lastBrightnessX;
            float y = OpenGlHelper.lastBrightnessY;

            // given lightmap
            float[] lightmap = state.lightmap;

            // default lightmap
            if (lightmap == null) {
                lightmap = this.state.lightmap;
            }

            if (lightmap == null) {
                // disabled lighting
                if (!state.bools.getOrDefault(GL11.GL_LIGHTING, false)) {
                    lightmap = new float[]{1, 1};
                }
            }

            if (lightmap != null) {
                float block = lightmap[0];
                float sky = lightmap[1];
                int i = ((int)(sky * 15)) << 20 | ((int)(block*15)) << 4;
                x = i % 65536;
                y = i / 65536;
            }
            shader.paramFloat("lightmapCoord", x, y);
            shader.paramMatrix("ModelViewMat", state.model_view());
            shader.paramMatrix("ProjMat", state.projection());
            shader.paramFloat("colorMult", state.color != null ? state.color : new float[] {1, 1, 1, 1});

            if (state.bools.containsKey(GL11.GL_CULL_FACE) || state.blend != null) {
                RenderState lrs = new RenderState();
                if (state.bools.containsKey(GL11.GL_CULL_FACE)) {
                    lrs.bools.put(GL11.GL_CULL_FACE, state.bools.get(GL11.GL_CULL_FACE));
                }
                if (state.blend != null) {
                    lrs.blend = state.blend;
                }
                return LegacyRenderContext.INSTANCE.apply(lrs);
            }
            return () -> {};
        }

        protected OpenGL.With push(Consumer<RenderState> mod) {
            if (useModern) {
                RenderState sub = state.clone();
                mod.accept(sub);
                OpenGL.With subrest = sync(sub);
                return subrest.and(() -> sync(state));
            } else {
                RenderState state = new RenderState();
                mod.accept(state);
                return LegacyRenderContext.INSTANCE.apply(state);
            }
        }

        /**
         * Draw the entire VB
         */
        public void draw() {
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, length);
        }
    }

    /**
     * Clear this VB from standard and GPU memory
     */
    public void free() {
        if (vbo != -1) {
            GL15.glDeleteBuffers(vbo);
            vbo = -1;
        }
    }
}
