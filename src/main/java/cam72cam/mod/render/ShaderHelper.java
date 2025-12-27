package cam72cam.mod.render;

/**Internal, don't use*/
public class ShaderHelper {
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
