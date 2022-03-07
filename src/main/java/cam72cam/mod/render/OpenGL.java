package cam72cam.mod.render;

import org.lwjgl.opengl.GL11;

/**
 * Optional, but recommended OpenGL helper library.
 *
 * Allows common GL operations to be safely wrapped in try blocks to prevent GFX bugs from early returns/exceptions
 */
public class OpenGL {
    private OpenGL() {}

    public static int allocateTexture() {
        // Allows us to set some parameters that cause issues in newer MC versions
        return GL11.glGenTextures();
    }

    @FunctionalInterface
    public interface With extends AutoCloseable {
        default void close() {
            restore();
        }
        void restore();

        default With and(With other) {
            return () -> {
                this.close();
                other.close();
            };
        }
    }
}
