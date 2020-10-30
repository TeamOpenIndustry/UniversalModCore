package cam72cam.mod.render;

import cam72cam.mod.resource.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * Optional, but recommended OpenGL helper library.
 *
 * Allows common GL operations to be safely wrapped in try blocks to prevent GFX bugs from early returns/exceptions
 */
public class OpenGL {
    private OpenGL() {}

    // This changes depending on LWJGL version
    public static void multMatrix(FloatBuffer fbm) {
        GL11.glMultMatrix(fbm);
    }

    public static With matrix(int mode) {
        int oldMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        GL11.glMatrixMode(mode);
        GL11.glPushMatrix();
        return () -> {
            GL11.glMatrixMode(mode);
            GL11.glPopMatrix();
            GL11.glMatrixMode(oldMode);
        };
    }

    public static With matrix() {
        GL11.glPushMatrix();
        return GL11::glPopMatrix;
    }

    private static void applyBool(int opt, boolean currState) {
        if (currState) {
            GL11.glEnable(opt);
        } else {
            GL11.glDisable(opt);
        }
    }

    public static With bool(int opt, boolean newState) {
        boolean oldState = GL11.glGetBoolean(opt);
        if (newState == oldState) {
            return () -> {};
        }
        applyBool(opt, newState);
        return () -> applyBool(opt, oldState);
    }

    public static With texture(int texID) {
        With t = bool(GL11.GL_TEXTURE_2D, true);
        int currentTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texID);
        // TODO is it worth optimizing the case where texID already == currentTexture?
        return () -> {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, currentTexture);
            t.restore();
        };
    }

    public static With texture(Identifier identifier) {
        With t = bool(GL11.GL_TEXTURE_2D, true);
        int currentTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        Minecraft.getMinecraft().getTextureManager().bindTexture(identifier.internal);
        return () -> {
            GlStateManager.bindTexture(currentTexture);
            t.restore();
        };
    }

    public static With color(float r, float g, float b, float a) {
        With color = bool(GL11.GL_COLOR_MATERIAL, true);

        FloatBuffer orig = ByteBuffer.allocateDirect(4 * 16).asFloatBuffer();
        GL11.glGetFloat(GL11.GL_CURRENT_COLOR, orig);

        GL11.glColor4f(r, g, b, a);
        return () -> {
            GL11.glColor4f(orig.get(0), orig.get(1), orig.get(2), orig.get(3));
            color.restore();
        };
    }

    public static With blend(int src, int dst) {
        With blend = bool(GL11.GL_BLEND, true);
        int origsrc = GL11.glGetInteger(GL11.GL_BLEND_SRC);
        int origdst = GL11.glGetInteger(GL11.GL_BLEND_DST);
        GL11.glBlendFunc(src, dst);
        return () -> {
            GL11.glBlendFunc(origsrc, origdst);
            blend.restore();
        };
    }

    public static With transparency(float r, float g, float b, float a) {
        With blend = blend(GL11.GL_CONSTANT_ALPHA, GL11.GL_ONE);
        FloatBuffer orig = ByteBuffer.allocateDirect(4 * 16).asFloatBuffer();
        GL11.glGetFloat(GL14.GL_BLEND_COLOR, orig);
        GL14.glBlendColor(r,g,b,a);
        return () -> {
            GL14.glBlendColor(orig.get(0), orig.get(1), orig.get(2), orig.get(3));
            blend.restore();
        };
    }

    @FunctionalInterface
    public interface With extends AutoCloseable {
        default void close() {
            restore();
        }
        void restore();
    }
}
