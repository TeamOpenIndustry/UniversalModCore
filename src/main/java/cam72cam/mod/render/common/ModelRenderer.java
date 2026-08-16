package cam72cam.mod.render.common;

import cam72cam.mod.model.common.mesh.GeneratedModel;
import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.model.common.mesh.ModelGroup;
import cam72cam.mod.model.common.mesh.VAOLayout;
import cam72cam.mod.model.obj.VertexBuffer;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.VBO;
import cam72cam.mod.util.With;
import org.lwjgl.opengl.GL11;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * GPU-side renderer (VBO) for a {@link Model}.
 *
 * <p>Renderers are cached per model via {@link #getRendererFor}, so a model is uploaded to
 * the GPU at most once. To draw, obtain a {@link Binding} with {@link #bind(ModelConfig, RenderState)}
 * (or {@link #bind(RenderState)} for default settings), then call one of the {@code draw}
 * methods. The returned binding must be closed with try-with-resources.</p>
 */
public class ModelRenderer extends VBO {
    private static final Map<Model, ModelRenderer> renderers = new ConcurrentHashMap<>();

    public final Model model;

    public ModelRenderer(Model model) {
        super(() -> new VertexBuffer(model.getVboData(), model.getLayout().has(VAOLayout.Usage.NORMAL)), s -> {});
        this.model = model;
    }

    public static ModelRenderer getRendererFor(Model model) {
        return renderers.computeIfAbsent(model, ModelRenderer::new);
    }

    @Override
    public Binding bind(RenderState state) {
        return bind(state, false);
    }

    @Override
    public Binding bind(RenderState state, boolean waitForLoad) {
        return new Binding(state, waitForLoad);
    }

    public Binding bind(ModelConfig config, RenderState state) {
        return bind(config, state, false);
    }

    public Binding bind(ModelConfig config, RenderState state, boolean waitForLoad) {
        config.apply(state, model);
        return new Binding(state, waitForLoad);
    }

    public class Binding extends VBO.Binding {
        protected Binding(RenderState state, boolean wait) {
            super(state, wait);
        }

        /**
         * Draws the named groups with the given state modifier applied during the draw.
         * @param groups Group names to draw
         * @param mod    Temporary render-state modifier applied while drawing
         */
        public void draw(Collection<String> groups, Consumer<RenderState> mod) {
            if (!isLoaded()) {
                return;
            }
            try (With pus = super.push(mod)) {
                draw(groups);
            }
        }

        /**
         * Draws the named groups.
         * @param groups Group names to be drawn
         */
        public void draw(Collection<String> groups) {
            if (!isLoaded()) {
                return;
            }

            if (model instanceof GeneratedModel) {
                draw();
                return;
            }

            // Model faces are ordered by group name at build time
            List<String> names = new ArrayList<>(groups);
            names.sort(String::compareTo);

            int start = -1;
            int stop = -1;
            for (String name : names) {
                ModelGroup info = model.getGroups().get(name);
                if (start == -1) {
                    start = info.faceStart;
                    stop = info.faceEnd + 1;
                } else if (info.faceStart == stop) {
                    stop = info.faceEnd + 1;
                } else {
                    GL11.glDrawArrays(GL11.GL_TRIANGLES, start * 3, (stop - start) * 3);
                    start = info.faceStart;
                    stop = info.faceEnd + 1;
                }
            }
            if (start != -1) {
                GL11.glDrawArrays(GL11.GL_TRIANGLES, start * 3, (stop - start) * 3);
            }
        }
    }

    /** Releases this renderer's GPU resources and deletes it from the per-model cache. */
    @Override
    public void free() {
        super.free();
        renderers.remove(model);
    }
}
