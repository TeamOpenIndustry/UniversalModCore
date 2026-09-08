package cam72cam.mod.render.opengl;

import cam72cam.mod.util.With;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBImaging;
import org.lwjgl.opengl.GL32;

import java.nio.FloatBuffer;
import java.util.function.Function;

import static cam72cam.mod.render.opengl.RenderContext.applyBool;
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

    boolean enabled;
    BlendFunction function;
    private Function<With, With> apply;

    private static FloatBuffer fourFloatBuffer;
    public static final BlendMode OPAQUE = new BlendMode(false);

    private BlendMode(boolean enabled) {
        this.enabled = enabled;
    }

    public BlendMode(int srcColor, int dstColor) {
        this(true);
        SourceFactor sColor = toSrcFactor(srcColor);
        DestFactor dColor = toDestFactor(dstColor);
        function = new BlendFunction(sColor, dColor, SourceFactor.SRC_ALPHA, DestFactor.DST_ALPHA);
    }

    public BlendMode(int srcColor, int dstColor, int srcAlpha, int dstAlpha) {
        this(true);
        SourceFactor sColor = toSrcFactor(srcColor);
        SourceFactor sAlpha = toSrcFactor(srcAlpha);
        DestFactor dColor = toDestFactor(dstColor);
        DestFactor dAlpha = toDestFactor(dstAlpha);
        function = new BlendFunction(sColor, dColor, sAlpha, dAlpha);
    }

    public BlendMode constantColor(float r, float g, float b, float a) {
        function = new BlendFunction(SourceFactor.CONSTANT_COLOR, DestFactor.CONSTANT_COLOR, SourceFactor.CONSTANT_ALPHA, DestFactor.CONSTANT_ALPHA);
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

    private SourceFactor toSrcFactor(int glConst) {
        return switch (glConst) {
            case GL_ZERO -> SourceFactor.ZERO;
            case GL_ONE -> SourceFactor.ONE;
            case GL_SRC_COLOR -> SourceFactor.SRC_COLOR;
            case GL_ONE_MINUS_SRC_COLOR -> SourceFactor.ONE_MINUS_SRC_COLOR;
            case GL_DST_COLOR -> SourceFactor.DST_COLOR;
            case GL_ONE_MINUS_DST_COLOR -> SourceFactor.ONE_MINUS_DST_COLOR;
            case GL_SRC_ALPHA -> SourceFactor.SRC_ALPHA;
            case GL_ONE_MINUS_SRC_ALPHA -> SourceFactor.ONE_MINUS_SRC_ALPHA;
            case GL_DST_ALPHA -> SourceFactor.DST_ALPHA;
            case GL_ONE_MINUS_DST_ALPHA -> SourceFactor.ONE_MINUS_DST_ALPHA;
            case GL_CONSTANT_COLOR -> SourceFactor.CONSTANT_COLOR;
            case GL_ONE_MINUS_CONSTANT_COLOR -> SourceFactor.ONE_MINUS_CONSTANT_COLOR;
            case GL_CONSTANT_ALPHA -> SourceFactor.CONSTANT_ALPHA;
            case GL_ONE_MINUS_CONSTANT_ALPHA -> SourceFactor.ONE_MINUS_CONSTANT_ALPHA;
            default -> SourceFactor.SRC_COLOR;
        };
    }

    private DestFactor toDestFactor(int glConst) {
        return switch (glConst) {
            case GL_ZERO -> DestFactor.ZERO;
            case GL_ONE -> DestFactor.ONE;
            case GL_SRC_COLOR -> DestFactor.SRC_COLOR;
            case GL_ONE_MINUS_SRC_COLOR -> DestFactor.ONE_MINUS_SRC_COLOR;
            case GL_DST_COLOR -> DestFactor.DST_COLOR;
            case GL_ONE_MINUS_DST_COLOR -> DestFactor.ONE_MINUS_DST_COLOR;
            case GL_SRC_ALPHA -> DestFactor.SRC_ALPHA;
            case GL_ONE_MINUS_SRC_ALPHA -> DestFactor.ONE_MINUS_SRC_ALPHA;
            case GL_DST_ALPHA -> DestFactor.DST_ALPHA;
            case GL_ONE_MINUS_DST_ALPHA -> DestFactor.ONE_MINUS_DST_ALPHA;
            case GL_CONSTANT_COLOR -> DestFactor.CONSTANT_COLOR;
            case GL_ONE_MINUS_CONSTANT_COLOR -> DestFactor.ONE_MINUS_CONSTANT_COLOR;
            case GL_CONSTANT_ALPHA -> DestFactor.CONSTANT_ALPHA;
            case GL_ONE_MINUS_CONSTANT_ALPHA -> DestFactor.ONE_MINUS_CONSTANT_ALPHA;
            default -> DestFactor.SRC_COLOR;
        };
    }
}
