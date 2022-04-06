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
            } catch (ClassNotFoundException e) {
                isLoaded = false;
            }
        }
        return isLoaded;
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
            int oldProg = ARBShaderObjects.glGetHandleARB(ARBShaderObjects.GL_PROGRAM_OBJECT_ARB);
            try {
                if (clsShaders == null) {
                    clsShaders = Class.forName("net.optifine.shaders.Shaders");
                    clsProgram = Class.forName("net.optifine.shaders.Program");
                    useProgram = clsShaders.getDeclaredMethod("useProgram", clsProgram);
                }
                if (program == null) {
                    program = clsShaders.getDeclaredField(name).get(null);
                }
                useProgram.invoke(null, program);
            } catch (Exception ex) {
                // Ignore me
            }
            return () -> ARBShaderObjects.glUseProgramObjectARB(oldProg);
        }
    }
}
