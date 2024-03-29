package cam72cam.mod.render;

import cam72cam.mod.util.With;
import org.lwjgl.opengl.ARBShaderObjects;

import java.lang.reflect.Method;

public class OptiFine {
    private OptiFine() {

    }

    private static Boolean isLoaded = null;
    public static boolean isLoaded() {
        if (isLoaded == null) {
            try {
                Class.forName("Config");
                isLoaded = true;
            } catch (Exception e) {
                isLoaded = false;
            }
        }
        return isLoaded;
    }

    public static With overrideFastRender(boolean state) {
        /* I don't think this is needed in 1.17.+
        if (isLoaded()) {
            try {
                boolean isFastRender = (boolean)Class.forName("Config").getDeclaredMethod("isFastRender").invoke(null);
                if (isFastRender != state) {
                    BooleanOption setting = (BooleanOption) AbstractOption.class.getDeclaredField("FAST_RENDER").get(null);
                    setting.toggle(Minecraft.getInstance().options);
                    return () -> setting.toggle(Minecraft.getInstance().options); // invert
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }*/
        return () -> {};
    }

    public enum Shaders {
        Terrain("ProgramTerrain"),
        Block("ProgramBlock"),
        BeaconBeam("ProgramBeacon"),
        Entities("ProgramEntities"),
        Hand("ProgramHand"),
        ;

        private final String name;
        private static Class<?> clsShaders;
        private static Class<?> clsProgram;
        private static Method useProgram;
        private Object program;

        Shaders(String programName) {
            this.name = programName;
        }

        public With bind() {
            if (!isLoaded()) {
                return () -> {};
            }
            try {
                if (clsShaders == null) {
                    clsShaders = Class.forName("net.optifine.shaders.Shaders");
                    clsProgram = Class.forName("net.optifine.shaders.Program");
                    useProgram = clsShaders.getDeclaredMethod("useProgram", clsProgram);
                }
                if (program == null) {
                    program = clsShaders.getDeclaredField(name).get(null);
                }
                int oldProg = ARBShaderObjects.glGetHandleARB(ARBShaderObjects.GL_PROGRAM_OBJECT_ARB);
                useProgram.invoke(null, program);
                return () -> ARBShaderObjects.glUseProgramObjectARB(oldProg);
            } catch (Exception ex) {
                // Ignore me
            }
            return () -> {};
        }
    }
}
