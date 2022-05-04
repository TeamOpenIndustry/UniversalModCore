package cam72cam.mod.render.opengl;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.util.With;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;

import java.util.ArrayList;
import java.util.List;

public class DirectDraw {
    private final List<VertexBuilder> verts = new ArrayList<>();

    public void draw(RenderState state) {
        BufferBuilder builder = new BufferBuilder(1024);
        ShaderInstance shader = RenderSystem.getShader();
        RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
        try (With ctx = RenderContext.apply(state)) {
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
            for (VertexBuilder vert : verts) {
                vert.draw(builder);
            }
            builder.end();
            BufferUploader.end(builder);
        }
        RenderSystem.setShader(() -> shader);
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
        private Float u;
        private Float v;
        private Float j;
        private Float k;
        private Float l;
        private Float r;
        private Float g;
        private Float b;
        private Float a;

        private VertexBuilder(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public VertexBuilder uv(double u, double v) {
            this.u = (float)u;
            this.v = (float)v;
            return this;
        }

        public VertexBuilder normal(double j, double k, double l) {
            this.j = (float)j;
            this.k = (float)k;
            this.l = (float)l;
            return this;
        }

        public VertexBuilder color(double r, double g, double b, double a) {
            this.r = (float)r;
            this.g = (float)g;
            this.b = (float)b;
            this.a = (float)a;
            return this;
        }

        private void draw(BufferBuilder builder) {
            VertexConsumer part = builder.vertex(x, y, z);
            if (u != null) {
                part = part.uv(u, v);
            } else {
                part.uv(0, 0);
            }
            if (r != null) {
                part = part.color(r, g, b, a);
            } else {
                part = part.color(1, 1, 1, 1);
            }
            if (j != null) {
                part = part.normal(j, k, l);
            } else {
                part = part.normal(1, 1, 1);
            }
            part.endVertex();
        }
    }
}
