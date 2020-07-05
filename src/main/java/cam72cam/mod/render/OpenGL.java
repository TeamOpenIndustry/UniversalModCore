package cam72cam.mod.render;

import cam72cam.mod.resource.Identifier;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.ARBImaging;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class OpenGL {
    private OpenGL() {};

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

    public static With bool(int opt, boolean newState) {
        GLBoolTracker t = new GLBoolTracker(opt, newState);
        return t::restore;
    }

    public static With texture(int texID) {
        GLBoolTracker t = new GLBoolTracker(GL11.GL_TEXTURE_2D, true);
        int currentTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texID);
        // TODO is it worth optimizing the case where texID already == currentTexture?
        return () -> {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, currentTexture);
            t.restore();
        };
    }

    public static With texture(Identifier identifier) {
        GLBoolTracker t = new GLBoolTracker(GL11.GL_TEXTURE_2D, true);
        int currentTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        Minecraft.getInstance().getTextureManager().bindTexture(identifier.internal);
        return () -> {
            GlStateManager.bindTexture(currentTexture);
            t.restore();
        };
    }

    public static With color(float r, float g, float b, float a) {
        GLBoolTracker color = new GLBoolTracker(GL11.GL_COLOR_MATERIAL, true);

        FloatBuffer orig = ByteBuffer.allocateDirect(4 * 16).asFloatBuffer();
        GL11.glGetFloatv(GL11.GL_CURRENT_COLOR, orig);

        GL11.glColor4f(r, g, b, a);
        return () -> {
            GL11.glColor4f(orig.get(0), orig.get(1), orig.get(2), orig.get(3));
            color.restore();
        };
    }

    public static With blend(int src, int dst) {
        GLBoolTracker blend = new GLBoolTracker(GL11.GL_BLEND, true);
        int origsrc = GL11.glGetInteger(GL11.GL_BLEND_SRC);
        int origdst = GL11.glGetInteger(GL11.GL_BLEND_DST);
        GL11.glBlendFunc(src, dst);
        return () -> {
            GL11.glBlendFunc(origsrc, origdst);
            blend.restore();
        };
    }

    public static With transparency(float r, float g, float b, float a) {
        With blend = blend(GL14.GL_CONSTANT_ALPHA, GL11.GL_ONE);
        FloatBuffer orig = ByteBuffer.allocateDirect(4 * 16).asFloatBuffer();
        GL11.glGetFloatv(ARBImaging.GL_BLEND_COLOR, orig);
        GL14.glBlendColor(r,g,b,a);
        return () -> {
            GL14.glBlendColor(orig.get(0), orig.get(1), orig.get(2), orig.get(3));
            blend.restore();
        };
    }

    public interface With extends AutoCloseable {
        default void close() {
            restore();
        }
        void restore();
    }
}
