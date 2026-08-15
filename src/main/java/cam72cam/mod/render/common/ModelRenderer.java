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

        public void draw(Collection<String> groups, Consumer<RenderState> mod) {
            if (!isLoaded()) {
                return;
            }
            try (With pus = super.push(mod)) {
                draw(groups);
            }
        }

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

    @Override
    public void free() {
        super.free();
        renderers.remove(model);
    }
}
