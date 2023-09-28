package cam72cam.mod.render.opengl;

import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.model.obj.VertexBuffer;
import cam72cam.mod.util.With;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class VBO {
    private static final List<VBO> vbos = new ArrayList<>();
    public static void registerClientEvents() {
        // free unused textures
        ClientEvents.TICK.subscribe(() -> {
            synchronized (vbos) {
                for (VBO vbo : vbos) {
                    if (vbo.vbo != null && System.currentTimeMillis() - vbo.lastUsed > 30 * 1000) {
                        vbo.free();
                    }
                }
            }
        });
    }

    private final Supplier<VertexBuffer> buffer;
    private final Consumer<RenderState> settings;


    //private int vbo;
    private net.minecraft.client.renderer.vertex.VertexBuffer vbo;
    private int length;
    private long lastUsed;
    private VertexBuffer vbInfo;

    public VBO(Supplier<VertexBuffer> buffer, Consumer<RenderState> settings) {
        this.buffer = buffer;
        //this.vbo = -1;
        this.settings = settings;

        synchronized (vbos) {
            vbos.add(this);
        }
    }

    private void init() {
        VertexBuffer vb = buffer.get();
        this.length = vb.data.length / (vb.stride);
        this.vbInfo = new VertexBuffer(0, vb.hasNormals);

        BufferBuilder test = new BufferBuilder(vb.data.length);
        test.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.ITEM);
        for (int i = 0; i < vb.data.length; i+=vb.stride) {
            for (VertexFormatElement element : test.getVertexFormat().getElements()) {
                switch (element.getUsage()) {
                    case POSITION:
                        test.pos(
                                vb.data[i+0 + vb.vertexOffset],
                                vb.data[i+1 + vb.vertexOffset],
                                vb.data[i+2 + vb.vertexOffset]
                        );
                        break;
                    case NORMAL:
                        test.normal(
                                vb.data[i+0 + vb.normalOffset],
                                vb.data[i+1 + vb.normalOffset],
                                vb.data[i+2 + vb.normalOffset]
                        );
                        break;
                    case COLOR:
                        test.color(
                                vb.data[i+0 + vb.colorOffset],
                                vb.data[i+1 + vb.colorOffset],
                                vb.data[i+2 + vb.colorOffset],
                                vb.data[i+3 + vb.colorOffset]
                        );
                        break;
                    case UV:
                        test.tex(
                                vb.data[i+0 + vb.textureOffset],
                                vb.data[i+1 + vb.textureOffset]
                        );
                        break;
                    case MATRIX:
                        break;
                    case BLEND_WEIGHT:
                        break;
                    case PADDING:
                        break;
                    case GENERIC:
                        break;
                }
            }
            test.endVertex();
        }
        test.finishDrawing();
        vbo = new net.minecraft.client.renderer.vertex.VertexBuffer(test.getVertexFormat());
        vbo.bufferData(test.getByteBuffer());
/*




        FloatBuffer buffer = GLAllocation.createDirectFloatBuffer(vb.data.length);
        buffer.put(vb.data);
        buffer.position(0);

        int oldVbo = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);

        vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, oldVbo);*/
    }

    public Binding bind(RenderState state) {
        return new Binding(state);
    }

    public class Binding implements With {
        private final With restore;

        protected Binding(RenderState state) {
            if (vbo == null) {
                init();
            }

            lastUsed = System.currentTimeMillis();

            settings.accept(state);

            int oldVbo = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
            GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT);

            this.restore = RenderContext.apply(state).and(() -> {
                GL11.glPopClientAttrib();
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, oldVbo);
            });
            vbo.bindBuffer();

            VertexFormat format = DefaultVertexFormats.ITEM;
            List<VertexFormatElement> elements = format.getElements();
            for (int i = 0; i < elements.size(); i++) {
                VertexFormatElement element = elements.get(i);
                int size = element.getElementCount();
                int type = element.getType().getGlConstant();
                int stride = format.getSize();
                long offset = format.getOffset(i);

                switch (element.getUsage()) {
                    case POSITION:
                        GL11.glVertexPointer(size, type, stride, offset);
                        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
                        break;
                    case NORMAL:
                        if (vbInfo.hasNormals) {
                            GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
                            GL11.glNormalPointer(type, stride, offset);
                        } else {
                            GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
                        }
                        break;
                    case COLOR:
                        GL11.glColorPointer(size, type, stride, offset);
                        GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
                        break;
                    case UV:
                        GL11.glTexCoordPointer(size, type, stride, offset);
                        GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                        break;
                    case MATRIX:
                        break;
                    case BLEND_WEIGHT:
                        break;
                    case PADDING:
                        break;
                    case GENERIC:
                        break;
                }
            }
        }

        @Override
        public void restore() {
            restore.close();
        }

        protected With push(Consumer<RenderState> mod) {
            RenderState state = new RenderState();
            mod.accept(state);
            return RenderContext.apply(state);
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
        synchronized (vbos) {
            if (vbo != null) {
                vbo.deleteGlBuffers();
                vbo = null;
            }
        }
    }
}
