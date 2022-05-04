package cam72cam.mod.render.opengl;

import cam72cam.mod.util.With;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBImaging;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.nio.FloatBuffer;
import java.util.function.Function;

import static cam72cam.mod.render.opengl.RenderContext.applyBool;

public class BlendMode {
    private Function<With, With> apply;

    private static FloatBuffer fourFloatBuffer;
    public static final BlendMode OPAQUE = new BlendMode(false);

    private BlendMode(boolean enabled) {
        apply = w -> {
            boolean oldBlend = GL11.glGetBoolean(GL11.GL_BLEND);
            applyBool(GL11.GL_BLEND, enabled);
            return w.and(() -> applyBool(GL11.GL_BLEND, oldBlend));
        };
    }
    public BlendMode(int srcColor, int dstColor) {
        this(true);
        apply = apply.andThen(w -> {
            int origSrcColor = GL11.glGetInteger(GL11.GL_BLEND_SRC);
            int origDstColor = GL11.glGetInteger(GL11.GL_BLEND_DST);
            GL11.glBlendFunc(srcColor, dstColor);
            return w.and(() -> GL11.glBlendFunc(origSrcColor, origDstColor));
        });
    }
    public BlendMode(int srcColor, int dstColor, int srcAlpha, int dstAlpha) {
        this(true);
        apply = apply.andThen(w -> {
            int origSrcColor = GL11.glGetInteger(GL11.GL_BLEND_SRC);
            int origDstColor = GL11.glGetInteger(GL11.GL_BLEND_DST);
            int origSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
            int origDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
            GL14.glBlendFuncSeparate(srcColor, dstColor, srcAlpha, dstAlpha);
            return w.and(() -> GL14.glBlendFuncSeparate(origSrcColor, origDstColor, origSrcAlpha, origDstAlpha));
        });
    }

    public BlendMode constantColor(float r, float g, float b, float a) {
        apply = apply.andThen(w -> {
            if (fourFloatBuffer == null) {
                fourFloatBuffer = BufferUtils.createFloatBuffer(16);
            }
            GL11.glGetFloatv(ARBImaging.GL_BLEND_COLOR, fourFloatBuffer);
            float[] oldColor = new float[] {fourFloatBuffer.get(0), fourFloatBuffer.get(1), fourFloatBuffer.get(2), fourFloatBuffer.get(3)};
            GL14.glBlendColor(r,g,b,a);
            return w.and(() -> GL14.glBlendColor(oldColor[0], oldColor[1], oldColor[2], oldColor[3]));
        });
        return this;
    }

    public Runnable apply() {
        return apply.apply(() -> {})::restore;
    }
}
