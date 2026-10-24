package cam72cam.mod.render.common;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.model.common.mesh.ModelGroup;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.util.With;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * An {@link IDrawable} that draws a selection of groups of a {@link ModelRenderer}.
 * A <code>null</code> or empty group collection means the entire model.
 */
public class GroupedDrawable implements IDrawable {
    private final Model model;
    private final String handle;
    private final ModelConfig config;
    private final Collection<String> groups;

    GroupedDrawable(ModelRenderer renderer, ModelConfig config, Collection<String> groups) {
        this.model = renderer.model;
        this.handle = renderer.handle;
        this.config = config != null ? config : ModelRenderer.DEFAULT_CONFIG;
        this.groups = groups;
    }

    @Override
    public void draw(RenderState state) {
        RenderContext ctx = RenderContext.get();
        if (!ctx.isUploaded(handle)) {
            ctx.uploadGeometry(handle, config.shouldWaitForLoad());
            if (!ctx.isUploaded(handle)) {
                return;
            }
        }

        RenderState s = state.clone();
        config.apply(s, model);

        try (With ignored = ctx.apply(s).and(ctx.bindGeometry(handle))) {
            if (groups == null || groups.isEmpty()) {
                ctx.drawGeometry(handle, 0, model.getVboData().length / model.getLayout().getStride());
            } else {
                // Model faces are ordered by group name at build time
                List<String> names = new ArrayList<>(groups);
                names.sort(Comparator.naturalOrder());

                int start = -1;
                int stop = -1;
                for (String name : names) {
                    ModelGroup group = model.getGroups().get(name);
                    if (group == null) {
                        continue;
                    }
                    if (start == -1) {
                        start = group.faceStart;
                        stop = group.faceEnd + 1;
                    } else if (group.faceStart == stop) {
                        stop = group.faceEnd + 1;
                    } else {
                        ctx.drawGeometry(handle, start * 3, (stop - start) * 3);
                        start = group.faceStart;
                        stop = group.faceEnd + 1;
                    }
                }
                if (start != -1) {
                    ctx.drawGeometry(handle, start * 3, (stop - start) * 3);
                }
            }
        }
    }

    @Override
    public String getRenderHandle() {
        return handle;
    }

    @Override
    public @Nullable Vec3d getCenter() {
        return (groups == null || groups.isEmpty()) ? null : model.centerOfGroups(groups);
    }
}
