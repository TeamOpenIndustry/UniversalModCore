package cam72cam.mod.render.opengl;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import static cam72cam.mod.render.opengl.LegacyRenderContext.applyBool;

public class BlendMode {
    private final boolean opaque;
    private int srcColor;
    private int dstColor;
    private Integer srcAlpha;
    private Integer dstAlpha;

    public static final BlendMode OPAQUE = new BlendMode(false);

    private BlendMode(boolean opaque) {
        this.opaque = opaque;
    }
    public BlendMode(int srcColor, int dstColor) {
        this(true);
        this.srcColor = srcColor;
        this.dstColor = dstColor;
        this.srcAlpha = null;
        this.dstAlpha = null;
    }
    public BlendMode(int srcColor, int dstColor, int srcAlpha, int dstAlpha) {
        this(srcColor, dstColor);
        this.srcAlpha = srcAlpha;
        this.dstAlpha = dstAlpha;
    }

    public Runnable apply() {
        boolean oldBlend = GL11.glGetBoolean(GL11.GL_BLEND);
        if (opaque) {
            applyBool(GL11.GL_BLEND, false);
            return () -> applyBool(GL11.GL_BLEND, oldBlend);
        } else {
            applyBool(GL11.GL_BLEND, true);
            int origSrcColor = GL11.glGetInteger(GL11.GL_BLEND_SRC);
            int origDstColor = GL11.glGetInteger(GL11.GL_BLEND_DST);

            boolean withAlpha = srcAlpha != null && dstAlpha != null;

            int origSrcAlpha = withAlpha ? GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA) : -1;
            int origDstAlpha = withAlpha ? GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA) : -1;
            if (withAlpha) {
                GL14.glBlendFuncSeparate(srcColor, dstColor, srcAlpha, dstAlpha);
            } else {
                GL11.glBlendFunc(srcColor, dstColor);
            }
            return () -> {
                if (withAlpha) {
                    GL14.glBlendFuncSeparate(origSrcColor, origDstColor, origSrcAlpha, origDstAlpha);
                } else {
                    GL11.glBlendFunc(origSrcColor, origDstColor);
                }
                applyBool(GL11.GL_BLEND, oldBlend);
            };
        }
    }
}
