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
        Runnable render = () -> {
            BufferBuilder builder = Tesselator.getInstance()
                                              .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            ShaderInstance shader = RenderSystem.getShader();
            //As IR doesn't use normal() at all I think we could change here to meet 1.19 need
            //TODO 1.19.4 figure out why
//        RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);

            //Add missing state
            if (state.color != null) {
                for (VertexBuilder vert : verts) {
                    if (vert.r == null) {
                        vert.color(state.color[0], state.color[1], state.color[2], state.color[3]);
                    }
                }
            }

            try (With ctx = RenderContext.apply(state)) {
//            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
                for (VertexBuilder vert : verts) {
                    vert.draw(builder);
                }
                BufferUploader.draw(builder.buildOrThrow());
            }
            RenderSystem.setShader(() -> shader);
        };
        if (state.stage != RenderContext.Stage.ENTITY) {
            render.run();
        } else {
            RenderContext.addDeferred(render);
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
        private final float x;
        private final float y;
        private final float z;
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
            this.x = (float) x;
            this.y = (float) y;
            this.z = (float) z;
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
            this.r = (float) Math.max(r, 0);
            this.g = (float) Math.max(g, 0);
            this.b = (float) Math.max(b, 0);
            this.a = (float) Math.max(a, 0);
            return this;
        }

        private void draw(BufferBuilder builder) {
            VertexConsumer part = builder.addVertex(x, y, z);
            if (u != null) {
                part = part.setUv(u, v);
            } else {
                part.setUv(0, 0);
            }
            if (r != null) {
                part = part.setColor(r, g, b, a);
            } else {
                part = part.setColor(1, 1, 1, 1);
            }
            if (j != null) {
                part = part.setNormal(j, k, l);
            } else {
                part = part.setNormal(1, 1, 1);
            }
            part.setLight(15 << 16 | 15);
//            part.endVertex();
        }
    }
}
