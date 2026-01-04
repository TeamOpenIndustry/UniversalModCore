package cam72cam.mod.render;

import org.lwjgl.opengl.ARBShaderObjects;

/** For Internal use only */
public class ShaderHelper {
    public static boolean isShaderPackEnabled() {
        return isIrisShaderEnabled() || isOptiFineEnabled();
    }

    public static boolean isOptiFineEnabled() {
        return OptiFine.isLoaded() && ARBShaderObjects.glGetHandleARB(ARBShaderObjects.GL_PROGRAM_OBJECT_ARB) != 0;
    }

    public static boolean isIrisShaderEnabled() {
        try {
            Class<?> clazz = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = clazz.getMethod("getInstance").invoke(null);
            return (boolean) instance.getClass().getMethod("isShaderPackInUse").invoke(instance);
        } catch (Throwable e) {
            return false;
        }
    }
}
