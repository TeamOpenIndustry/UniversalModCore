package cam72cam.mod.render.common;

import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.model.common.mesh.VAOLayout;
import cam72cam.mod.render.obj.OBJTextureSheet;
import cam72cam.mod.render.opengl.RenderContext;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.util.With;
import net.minecraft.client.renderer.GLAllocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ModelRender {
    /** When true, submits are drawn immediately (initial behaviour). */
    private static boolean IMMEDIATE = true;

    private static final List<DrawCall> OPAQUE = new ArrayList<>();
    private static final List<DrawCall> TRANSPARENT = new ArrayList<>();

    private final Model model;
    private int vbo = -1;
    private int vertexCount;

    public ModelRender(Model model) {
        this.model = model;
    }

    /** Draws all queued draw calls (opaque first, then transparent) and clears the queues. */
    public static void flush() {
        OPAQUE.forEach(DrawCall::draw);
        TRANSPARENT.forEach(DrawCall::draw);
        OPAQUE.clear();
        TRANSPARENT.clear();
    }

    public Binding binding() {
        return new Binding();
    }

    public class Binding {
        private int lod = -1;
        private String variant = "";
        private RenderState state = new RenderState();

        public Binding lod(int lod) {
            this.lod = lod;
            return this;
        }

        public Binding texture(String variant) {
            this.variant = variant;
            return this;
        }

        public Binding state(RenderState state) {
            this.state = state.clone();
            return this;
        }

        public void opaque() {
            submit(false);
        }

        public void transparent() {
            submit(true);
        }

        private void submit(boolean transparent) {
            DrawCall call = new DrawCall(ModelRender.this, variant, lod, state);
            (transparent ? TRANSPARENT : OPAQUE).add(call);
            if (IMMEDIATE) {
                flush();
            }
        }
    }

    private static class DrawCall {
        final ModelRender render;
        final String variant;
        final int lod;
        final RenderState state;

        DrawCall(ModelRender render, String variant, int lod, RenderState state) {
            this.render = render;
            this.variant = variant;
            this.lod = lod;
            this.state = state;
        }

        void draw() {
            render.draw(variant, lod, state);
        }
    }

    private void ensureVbo() {
        if (vbo == -1) {
            int oldVbo = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);

            float[] data = model.getVboData();
            vbo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            FloatBuffer buffer = GLAllocation.createDirectFloatBuffer(data.length);
            buffer.put(data);
            buffer.flip();
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
            vertexCount = data.length / (model.getLayout().getStride() / 4);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, oldVbo);
        }
    }

    private void draw(String variant, int lod, RenderState state) {
        ensureVbo();

        RenderState s = state.clone().smooth_shading(model.isSmoothShading).cull_face(false);
        applyTextures(s, variant, lod);

        VAOLayout layout = model.getLayout();
        int oldVbo = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT);

        try (With ctx = RenderContext.apply(s).and(() -> {
            GL11.glPopClientAttrib();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, oldVbo);
        })) {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            layout.setup();
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, vertexCount);
            layout.restore();
        }
    }

    private void applyTextures(RenderState s, String variant, int lod) {
        OBJTextureSheet albedo = pick(mapFor(model.getTextures(), variant), lod);
        if (albedo != null) {
            s.texture(albedo);
        }
        if (model.hasSpecular) {
            OBJTextureSheet spec = pick(mapFor(model.getSpeculars(), variant), lod);
            if (spec != null) {
                s.specular(spec);
            }
        }
        if (model.hasNormal) {
            OBJTextureSheet norm = pick(mapFor(model.getNormals(), variant), lod);
            if (norm != null) {
                s.normals(norm);
            }
        }
    }

    private static Map<Integer, OBJTextureSheet> mapFor(Map<String, Map<Integer, OBJTextureSheet>> all, String variant) {
        Map<Integer, OBJTextureSheet> map = all.get(variant);
        return map != null ? map : all.get("");
    }

    /** Largest sheet with size <= target, or the smallest when all are larger. */
    private static OBJTextureSheet pick(Map<Integer, OBJTextureSheet> lodMap, int target) {
        if (lodMap == null || lodMap.isEmpty()) {
            return null;
        }
        if (target <= 0) {
            return lodMap.get(Collections.max(lodMap.keySet()));
        }
        Integer best = null;
        for (Integer size : lodMap.keySet()) {
            if (size <= target && (best == null || size > best)) {
                best = size;
            }
        }
        return lodMap.get(best != null ? best : Collections.min(lodMap.keySet()));
    }

    public void free() {
        if (vbo != -1) {
            GL15.glDeleteBuffers(vbo);
            vbo = -1;
        }
    }
}
