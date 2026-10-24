package cam72cam.mod.render.common;

import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.render.opengl.BlendMode;
import cam72cam.mod.render.opengl.RenderState;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Enqueue proxy for a {@link Model}.<br>
 * Renderers are cached per model via {@link #getRendererFor}. The geometry is uploaded lazily by the
 * active {@link RenderContext}; callers only enqueue {@link GroupedDrawable}s.
 */
public class ModelRenderer {
    private static final Map<Model, ModelRenderer> renderers = new ConcurrentHashMap<>();
    public static final ModelConfig DEFAULT_CONFIG = new ModelConfig();

    public Model model;
    final String handle;

    private ModelRenderer(Model model) {
        this.model = model;
        this.handle = RenderContext.get().setGeometryUploader(model::getVboData, model.getLayout());
    }

    public static ModelRenderer getRendererFor(Model model) {
        return renderers.computeIfAbsent(model, ModelRenderer::new);
    }

    /**
     * Enqueue a (set of) opaque part to the batch.
     * @param config Material configuration (texture LOD/variant) to apply
     * @param state  Base render state
     * @param groups Group names to draw, or <code>null</code> for the entire model
     * @param mod    Temporary render-state modifier applied while drawing
     */
    public void enqueueOpaque(ModelConfig config, RenderState state, Collection<String> groups, Consumer<RenderState> mod) {
        RenderState applied = state.clone();
        mod.accept(applied);
        RenderContext.get().enqueueOpaque(new GroupedDrawable(this, config, groups), applied);
    }

    /** Enqueue the entire model as opaque. */
    public void enqueueOpaque(RenderState state) {
        enqueueOpaque(DEFAULT_CONFIG, state, null, s -> {});
    }

    /** Enqueue a (set of) opaque part to the batch. */
    public void enqueueOpaque(RenderState state, Collection<String> groups) {
        enqueueOpaque(DEFAULT_CONFIG, state, groups, s -> {});
    }

    /**
     * Enqueue a (set of) transparent part to the batch.
     * @param config Material configuration (texture LOD/variant) to apply
     * @param state  Base render state
     * @param groups Group names to draw, or <code>null</code> for the entire model
     * @param mod    Temporary render-state modifier applied while drawing
     */
    public void enqueueTransparent(ModelConfig config, RenderState state, Collection<String> groups, Consumer<RenderState> mod) {
        RenderState applied = state.clone();
        mod.accept(applied);
        if (groups == null) {
            groups = this.model.groups();
        }
        applied.blend(new BlendMode(BlendMode.GL_SRC_ALPHA, BlendMode.GL_ONE_MINUS_SRC_ALPHA));
        for (String str : groups) {
            RenderContext.get().enqueueTransparent(new GroupedDrawable(this, config, Collections.singleton(str)), applied);
        }
    }

    /** Enqueue the entire model as transparent. */
    public void enqueueTransparent(RenderState state) {
        enqueueTransparent(DEFAULT_CONFIG, state, null, s -> {});
    }

    /** Enqueue a (set of) transparent part to the batch. */
    public void enqueueTransparent(RenderState state, Collection<String> groups) {
        enqueueTransparent(DEFAULT_CONFIG, state, groups, s -> {});
    }

    /** Flush all deferred calls */
    public static void endModel() {
        RenderContext.get().flushEnqueued();
    }

    public static void release(Model holder) {
        if (renderers.containsKey(holder)) {
            renderers.get(holder).free();
        }
    }

    /** Releases this renderer, its geometry and its entry in the per-model cache. */
    public void free() {
        renderers.remove(model);
        RenderContext.get().deleteGeometry(handle);
        this.model = null;
    }
}
