package cam72cam.mod.render.opengl;

import cam72cam.mod.ModCore;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.model.obj.VertexBuffer;
import cam72cam.mod.util.With;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.GlDebug;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL32;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class VBO {
    private static final List<VBO> vbos = new ArrayList<>();
    public static void registerClientEvents() {
        // free unused textures
        ClientEvents.TICK.subscribe(() -> {
            synchronized (vbos) {
                for (VBO vbo : vbos) {
                    if (vbo.vbo != -1 && System.currentTimeMillis() - vbo.lastUsed > 30 * 1000) {
                        vbo.free();
                    }
                }
            }
        });
    }

    private final Supplier<VertexBuffer> buffer;
    private final Consumer<RenderState> settings;

    private int vao;
    private int vbo;
    private int length;
    private long lastUsed;
    private VertexBuffer vbInfo;

    public VBO(Supplier<VertexBuffer> buffer, Consumer<RenderState> settings) {
        this.buffer = buffer;
        this.vao = -1;
        this.vbo = -1;
        this.settings = settings;

        synchronized (vbos) {
            vbos.add(this);
        }
    }

    private void init() {
        VertexBuffer vb = buffer.get();
        this.length = vb.data.length / (vb.stride);
        this.vbInfo = new VertexBuffer(0, vb.hasNormals);
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vb.data.length);
        buffer.put(vb.data);
        buffer.position(0);

        int oldVao = GL32.glGetInteger(GL32.GL_VERTEX_ARRAY_BUFFER_BINDING);// TODO this should be GL32
        int oldVbo = GL32.glGetInteger(GL32.GL_ARRAY_BUFFER_BINDING);

        vao = GL32.glGenVertexArrays();
        GL32.glBindVertexArray(vao);
        vbo = GL32.glGenBuffers();
        GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, vbo);
        GL32.glBufferData(GL32.GL_ARRAY_BUFFER, buffer, GL32.GL_STATIC_DRAW);

        GL32.glBindVertexArray(oldVao);
        GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, oldVbo);
    }

    public Binding bind(RenderState state) {
        return new Binding(state);
    }

    public class Binding implements With {
        private final With restore;
        private final RenderState state;

        protected Binding(RenderState state) {
            RenderContext.checkError();
            if (vbo == -1) {
                init();
            }
            RenderContext.checkError();

            lastUsed = System.currentTimeMillis();

            settings.accept(state);
            this.state = state;

            ShaderInstance oldShader = RenderSystem.getShader();
            int oldVao = GL32.glGetInteger(GL32.GL_VERTEX_ARRAY_BUFFER_BINDING);
            int oldVbo = GL32.glGetInteger(GL32.GL_ARRAY_BUFFER_BINDING);


            /*
            GL32.glEnableClientState(GL32.GL_VERTEX_ARRAY);
            GL32.glEnableClientState(GL32.GL_TEXTURE_COORD_ARRAY);
            GL32.glEnableClientState(GL32.GL_COLOR_ARRAY);
            if (vbInfo.hasNormals) {
                GL32.glEnableClientState(GL32.GL_NORMAL_ARRAY);
            } else {
                GL32.glDisableClientState(GL32.GL_NORMAL_ARRAY);
            }*/

            ShaderInstance shader = GameRenderer.getRendertypeCutoutShader();
            RenderSystem.setShader(() -> shader);
            GL32.glBindVertexArray(vao);
            GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, vbo);

            int stride = vbInfo.stride * Float.BYTES;

            ImmutableList<VertexFormatElement> elements = shader.getVertexFormat().getElements();
            for (int i = 0; i < elements.size(); i++) {
                VertexFormatElement element = elements.get(i);
                switch (element.getUsage()) {
                    case POSITION -> {
                        //element.setupBufferState(i, (long) vbInfo.vertexOffset * Float.BYTES, stride);
                        GL32.glEnableVertexAttribArray(i);
                        GL32.glVertexAttribPointer(i, 3, GL32.GL_FLOAT, false, stride, (long) vbInfo.vertexOffset * Float.BYTES);
                    }
                    case NORMAL -> {
                        if (vbInfo.hasNormals) {
                            GL32.glEnableVertexAttribArray(i);
                            GL32.glVertexAttribPointer(i, 3, GL32.GL_FLOAT, true, stride, (long) vbInfo.normalOffset * Float.BYTES);
                        }
                    }
                    case COLOR -> {
                        GL32.glEnableVertexAttribArray(i);
                        GL32.glVertexAttribPointer(i, 4, GL32.GL_FLOAT, true, stride, (long) vbInfo.colorOffset * Float.BYTES);
                    }
                    case UV -> {
                        for (Map.Entry<String, VertexFormatElement> entry : shader.getVertexFormat().getElementMapping().entrySet()) {
                            if (entry.getValue() == element) {
                                if (entry.getKey().equals("UV0")) {
                                    GL32.glEnableVertexAttribArray(i);
                                    GL32.glVertexAttribPointer(i, 2, GL32.GL_FLOAT, false, stride, (long) vbInfo.textureOffset * Float.BYTES);
                                } else if (entry.getKey().equals("UV1")) {
                                    // TODO
                                } else if (entry.getKey().equals("UV2")) {
                                    GL32.glDisableVertexAttribArray(i);
                                    int x = 255;
                                    int y = 255;
                                    if (state.lightmap != null) {
                                        x = (int) (state.lightmap[0] * 255);
                                        y = (int) (state.lightmap[1] * 255);
                                    }
                                    GL32.glVertexAttribI2i(i, x, y);
                                }
                            }
                        }
                    }
                }
            }
            RenderContext.checkError();

            this.restore = RenderContext.apply(state).and(() -> {
                RenderContext.checkError();
                shader.getVertexFormat().clearBufferState();

                RenderContext.checkError();

                //GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, oldVbo);
                //GL32.glBindBuffer(GL32.GL_ELEMENT_ARRAY_BUFFER, 0);
                //GL32.glBindVertexArray(oldVao);
                RenderSystem.setShader(() -> oldShader);
                BufferUploader.reset();
            });
        }

        @Override
        public void restore() {
            restore.close();
        }

        protected With push(Consumer<RenderState> mod) {
            RenderState state = this.state.clone();
            mod.accept(state);
            return RenderContext.apply(state);
        }

        /**
         * Draw the entire VB
         */
        public void draw() {
            GL32.glDrawArrays(GL32.GL_TRIANGLES, 0, length);
            RenderContext.checkError();
        }
    }

    /**
     * Clear this VB from standard and GPU memory
     */
    public void free() {
        synchronized (vbos) {
            if (vbo != -1) {
                GL32.glDeleteBuffers(vbo);
                GL32.glDeleteVertexArrays(vao);
                vbo = -1;
            }
        }
    }
}
