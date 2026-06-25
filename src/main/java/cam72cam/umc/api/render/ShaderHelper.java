package cam72cam.umc.api.render;

import org.lwjgl.opengl.ARBShaderObjects;

/** For Internal use only */
public class ShaderHelper {
    public static boolean isShaderPackEnabled() {
        return isOptiFineEnabled();
    }

    public static boolean isOptiFineEnabled() {
        return OptiFine.isLoaded() && ARBShaderObjects.glGetHandleARB(ARBShaderObjects.GL_PROGRAM_OBJECT_ARB) != 0;
    }
}
