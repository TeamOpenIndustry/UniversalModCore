package cam72cam.mod.render.opengl;

import cam72cam.mod.ModCore;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.model.obj.VertexBuffer;
import cam72cam.mod.render.ShaderHelper;
import cam72cam.mod.util.With;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL32;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
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
        ClientEvents.REGISTER_SHADER.subscribe(event -> {
            event.registerShader(RenderContext.UMC_CORE);
        });
    }

    private final Supplier<VertexBuffer> buffer;
    private final Consumer<RenderState> settings;

    private int vao;
    private int vbo;
    private int length;
    private long lastUsed;
    private VertexBuffer vbInfo;

    private static final ExecutorService pool = new ThreadPoolExecutor(0, Runtime.getRuntime().availableProcessors(),
            5L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            runnable -> {
                Thread thread = new Thread(runnable);
                thread.setName("UMC-VertexBufferLoader");
                thread.setPriority(Thread.MIN_PRIORITY);
                return thread;
            });
    private Future<FloatBuffer> loader = null;

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
        if (loader != null) {
            if (loader.isDone()) {
                try {
                    int oldVao = GL32.glGetInteger(GL32.GL_VERTEX_ARRAY_BUFFER_BINDING);// TODO this should be GL32
                    int oldVbo = GL32.glGetInteger(GL32.GL_ARRAY_BUFFER_BINDING);

                    vao = GL32.glGenVertexArrays();
                    GL32.glBindVertexArray(vao);
                    vbo = GL32.glGenBuffers();
                    GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, vbo);
                    GL32.glBufferData(GL32.GL_ARRAY_BUFFER, loader.get(), GL32.GL_STATIC_DRAW);

                    GL32.glBindVertexArray(oldVao);
                    GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, oldVbo);
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException("Cannot create VBO: ", e);
                }
                loader = null;
            }
        } else {
            // Start thread
            loader = pool.submit(() -> {
                VertexBuffer vb = buffer.get();
                this.length = vb.data.length / (vb.stride);
                this.vbInfo = new VertexBuffer(0, vb.hasNormals);
                FloatBuffer buffer = BufferUtils.createFloatBuffer(vb.data.length);
                buffer.put(vb.data);
                buffer.position(0);
                return buffer;
            });
        }
    }

    public Binding bind(RenderState state) {
        return bind(state, false);
    }

    public Binding bind(RenderState state, boolean waitForLoad) {
        return new Binding(state, waitForLoad);
    }

    public class Binding implements With {
        private final With restore;
        private final RenderState state;

        public boolean isLoaded() {
            return vbo != -1;
        }


        protected Binding(RenderState state, boolean wait) {
            lastUsed = System.currentTimeMillis();

            if (!isLoaded()) {
                init();
            }
            RenderContext.checkError();
            lastUsed = System.currentTimeMillis();

            if (!wait) {
                if (!isLoaded()) {
                    restore = () -> {
                    };
                    this.state = null;
                    return;
                }
            } else {
                while (!isLoaded()) {
                    init();
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            settings.accept(state);
            this.state = state;

            CompiledShaderProgram oldShader = RenderSystem.getShader();

            RenderType renderType;
            ShaderProgram shader;
            renderType = switch (state.getStage()) {
                case GUI -> RenderType.gui();
                case ITEM_IN_WORLD, ITEM_SPRITE_TEX -> RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS);
                default -> ShaderHelper.isShaderPackEnabled()
                           ? RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS)
                           : RenderContext.UMC_CORE_RT;
            };

            shader = switch (state.getStage()) {
                //DirectDraw will set their shader respectively
                case GUI -> CoreShaders.POSITION_TEX_COLOR;
                case ITEM_IN_WORLD, ITEM_SPRITE_TEX -> CoreShaders.RENDERTYPE_ENTITY_CUTOUT;
                default -> ShaderHelper.isShaderPackEnabled()
                           ? CoreShaders.RENDERTYPE_ENTITY_CUTOUT
                           : RenderContext.UMC_CORE;
            };
            if (renderType != null) {
                renderType.setupRenderState();
            }

            RenderSystem.setShader(shader);

            GL32.glBindVertexArray(vao);
            GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, vbo);

            int stride = vbInfo.stride * Float.BYTES;

            //Could we have a better way?
            if (state.stage == RenderContext.Stage.GUI) {
                state.lightmap(0.9f, 0.9f)
                     .color(1, 1, 1, 1);
            }

            List<VertexFormatElement> elements = shader.vertexFormat().getElements();
            for (int i = 0; i < elements.size(); i++) {
                VertexFormatElement element = elements.get(i);
                switch (element.usage()) {
                    case POSITION -> {
                        //element.setupBufferState(i, (long) vbInfo.vertexOffset * Float.BYTES, stride);
                        GL32.glEnableVertexAttribArray(i);
                        GL32.glVertexAttribPointer(i, 3, GL32.GL_FLOAT, false, stride, (long) vbInfo.vertexOffset * Float.BYTES);
                    }
                    case NORMAL -> {
                        if (!vbInfo.hasNormals) continue;
                        GL32.glEnableVertexAttribArray(i);
                        GL32.glVertexAttribPointer(i, 3, GL32.GL_FLOAT, true, stride, (long) vbInfo.normalOffset * Float.BYTES);
                    }
                    case COLOR -> {
                        GL32.glEnableVertexAttribArray(i);
                        GL32.glVertexAttribPointer(i, 4, GL32.GL_FLOAT, true, stride, (long) vbInfo.colorOffset * Float.BYTES);
                    }
                    case UV -> {
                        for (Map.Entry<String, VertexFormatElement> entry : shader.vertexFormat().getElementMapping().entrySet()) {
                            if (entry.getValue() == element) {
                                switch (entry.getKey()) {
                                    case "UV0" -> {
                                        GL32.glEnableVertexAttribArray(i);
                                        GL32.glVertexAttribPointer(i, 2, GL32.GL_FLOAT, false, stride, (long) vbInfo.textureOffset * Float.BYTES);
                                    }
                                    case "UV1" -> {
                                        GL32.glDisableVertexAttribArray(i);
                                        GL32.glVertexAttribI2i(i, 0, 10);
                                    }
                                    case "UV2" -> {
                                        GL32.glDisableVertexAttribArray(i);
                                        //240 means full bright
                                        int x = RenderContext.FULL_BRIGHT;
                                        int y = RenderContext.FULL_BRIGHT;
                                        if (state.lightmap != null) {
                                            x = (int) (state.lightmap[0] * RenderContext.FULL_BRIGHT);
                                            y = (int) (state.lightmap[1] * RenderContext.FULL_BRIGHT);
                                        }
                                        GL32.glVertexAttribI2i(i, x, y);
                                    }
                                }
                            }
                        }
                    }
                    case GENERIC -> {
                        for (Map.Entry<String, VertexFormatElement> entry : shader.vertexFormat().getElementMapping().entrySet()) {
                            // Iris fields for proper normal rendering
                            if (entry.getValue() == element && "at_tangent".equals(entry.getKey())) {
                                GL32.glDisableVertexAttribArray(i);
                                GL32.glVertexAttrib4f(i, 1.0F, 0.0F, 0.0F, 1.0F);
                            }
                        }
                    }
                }
            }
            RenderContext.checkError();

            this.restore = RenderContext.apply(state).and(() -> {
                RenderContext.checkError();

                if (renderType != null) {
                    renderType.clearRenderState();
                }

                shader.vertexFormat().clearBufferState();

                RenderContext.checkError();

                RenderSystem.setShader(oldShader);
                BufferUploader.reset();
            });
        }

        @Override
        public void restore() {
            restore.close();
        }

        protected With push(Consumer<RenderState> mod) {
            if (!isLoaded()) {
                return () -> {};
            }
            RenderState state = this.state.clone();
            mod.accept(state);
            return RenderContext.apply(state);
        }

        /**
         * Draw the entire VB
         */
        public void draw() {
            if (!isLoaded()) {
                return;
            }
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
