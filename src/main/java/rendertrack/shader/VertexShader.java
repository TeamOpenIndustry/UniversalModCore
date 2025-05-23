package rendertrack.shader;

import cam72cam.mod.resource.Identifier;
import org.lwjgl.opengl.GL32;

/**
 * Wrapper and accessor class for GLSL vertex shaders.
 * @author Haxorouse
 */
public class VertexShader {
    //RT we need a way of storing the file location
    private final Identifier location;
    private final int vertexShader;

    public VertexShader(Identifier path) {
        location = path;
        vertexShader = GL32.glCreateShader(GL32.GL_VERTEX_SHADER);
    }

    /**
     * Loads the GLSL code for the shader, OGL shaders are compiled from strings at runtime.
     * @return the GLSL code of the shader as a string
     */
    public String load() {
        return null;
    }


}
