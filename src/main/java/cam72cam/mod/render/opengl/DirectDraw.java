package cam72cam.mod.render.opengl;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.util.With;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class DirectDraw {
    private static final Tessellator INTERNAL = new Tessellator(256*1024);
    private final List<VertexBuilder> verts = new ArrayList<>();

    public void draw(RenderState state) {
        try (With ctx = RenderContext.apply(state)) {
            BufferBuilder builder = INTERNAL.getBuffer();
            builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            //TODO
            // 1.color
            // 2.optional normal
            for (VertexBuilder vert : verts) {
                vert.draw(builder, state);
            }
            INTERNAL.draw();
        }
    }

    public VertexBuilder vertex(double x, double y, double z) {
        VertexBuilder target = new VertexBuilder(x, y, z);
        verts.add(target);
        return target;
    }

    public VertexBuilder vertex(Vec3d pos) {
        return vertex(pos.x, pos.y, pos.z);
    }

    public static class VertexBuilder {
        private final double x;
        private final double y;
        private final double z;
        private Double u;
        private Double v;
        private Double j;
        private Double k;
        private Double l;
        private Double r;
        private Double g;
        private Double b;
        private Double a;

        private VertexBuilder(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public VertexBuilder uv(double u, double v) {
            this.u = u;
            this.v = v;
            return this;
        }

        public VertexBuilder normal(double j, double k, double l) {
            this.j = j;
            this.k = k;
            this.l = l;
            return this;
        }

        public VertexBuilder color(double r, double g, double b, double a) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            return this;
        }

        private void draw(BufferBuilder buffer, RenderState state) {
            buffer.pos(x, y, z);
            if (u != null) {
                buffer.tex(u, v);
            } else {
                buffer.tex(0, 0);
            }
            if (r != null) {
                buffer.color(r.floatValue(), g.floatValue(), b.floatValue(), a.floatValue());
            } else if (state.color != null) {
                buffer.color(state.color[0], state.color[1], state.color[2], state.color[3]);
            } else {
                buffer.color(1f, 1f, 1f, 1f);
            }
            if (j != null) {
                buffer.normal(j.floatValue(), k.floatValue(), l.floatValue());
            } else {
                buffer.normal(0, 0, 0);
            }
            buffer.endVertex();
        }
    }
}
