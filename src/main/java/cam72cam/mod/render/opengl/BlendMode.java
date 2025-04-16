package cam72cam.mod.render.opengl;

import cam72cam.mod.util.With;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBImaging;
import org.lwjgl.opengl.GL32;

import java.nio.FloatBuffer;
import java.util.function.Function;

import static cam72cam.mod.render.opengl.RenderContext.checkError;

public class BlendMode {
    public static final int GL_ZERO = GL32.GL_ZERO;
    public static final int GL_ONE = GL32.GL_ONE;
    public static final int GL_SRC_COLOR = GL32.GL_SRC_COLOR;
    public static final int GL_ONE_MINUS_SRC_COLOR = GL32.GL_ONE_MINUS_SRC_COLOR;
    public static final int GL_DST_COLOR = GL32.GL_DST_COLOR;
    public static final int GL_ONE_MINUS_DST_COLOR = GL32.GL_ONE_MINUS_DST_COLOR;
    public static final int GL_SRC_ALPHA = GL32.GL_SRC_ALPHA;
    public static final int GL_ONE_MINUS_SRC_ALPHA = GL32.GL_ONE_MINUS_SRC_ALPHA;
    public static final int GL_DST_ALPHA = GL32.GL_DST_ALPHA;
    public static final int GL_ONE_MINUS_DST_ALPHA = GL32.GL_ONE_MINUS_DST_ALPHA;
    public static final int GL_CONSTANT_COLOR = GL32.GL_CONSTANT_COLOR;
    public static final int GL_ONE_MINUS_CONSTANT_COLOR = GL32.GL_ONE_MINUS_CONSTANT_COLOR;
    public static final int GL_CONSTANT_ALPHA = GL32.GL_CONSTANT_ALPHA;
    public static final int GL_ONE_MINUS_CONSTANT_ALPHA = GL32.GL_ONE_MINUS_CONSTANT_ALPHA;


    private Function<With, With> apply;

    private static FloatBuffer fourFloatBuffer;
    public static final BlendMode OPAQUE = new BlendMode(false);

    private BlendMode(boolean enabled) {
        apply = w -> {
            RenderSystem.enableBlend();
            return w.and(RenderSystem::disableBlend);
        };
    }
    public BlendMode(int srcColor, int dstColor) {
        this(true);
        apply = apply.andThen(w -> {
            RenderSystem.blendFunc(srcColor, dstColor);
            return w.and(RenderSystem::defaultBlendFunc);
        });
    }
    public BlendMode(int srcColor, int dstColor, int srcAlpha, int dstAlpha) {
        this(true);
        apply = apply.andThen(w -> {
            RenderSystem.blendFuncSeparate(srcColor, dstColor, srcAlpha, dstAlpha);
            checkError();
            return w.and(RenderSystem::defaultBlendFunc);
        });
    }

    public BlendMode constantColor(float r, float g, float b, float a) {
        apply = apply.andThen(w -> {
            if (fourFloatBuffer == null) {
                fourFloatBuffer = BufferUtils.createFloatBuffer(16);
            }
            GL32.glGetFloatv(ARBImaging.GL_BLEND_COLOR, fourFloatBuffer);
            float[] oldColor = new float[] {fourFloatBuffer.get(0), fourFloatBuffer.get(1), fourFloatBuffer.get(2), fourFloatBuffer.get(3)};
            GL32.glBlendColor(r,g,b,a);
            return w.and(() -> GL32.glBlendColor(oldColor[0], oldColor[1], oldColor[2], oldColor[3]));
        });
        return this;
    }

    public Runnable apply() {
        return apply.apply(() -> {})::restore;
    }
}
