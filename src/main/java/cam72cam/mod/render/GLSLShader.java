package cam72cam.mod.render;

import cam72cam.mod.resource.Identifier;
import org.apache.commons.io.IOUtils;
import org.lwjgl.opengl.ARBFragmentShader;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/** Optional GLSL Shader wrapper */
public class GLSLShader {
    private final int program;

    /** Create a shader with vert and frag */
    public GLSLShader(Identifier vert, Identifier frag) {
        int vertShader = ARBShaderObjects.glCreateShaderObjectARB(ARBVertexShader.GL_VERTEX_SHADER_ARB);
        int fragShader = ARBShaderObjects.glCreateShaderObjectARB(ARBFragmentShader.GL_FRAGMENT_SHADER_ARB);
        ARBShaderObjects.glShaderSourceARB(vertShader, readShader(vert));
        ARBShaderObjects.glCompileShaderARB(vertShader);
        if (ARBShaderObjects.glGetObjectParameteriARB(vertShader, ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB) == GL11.GL_FALSE)
            throw new RuntimeException("Error creating shader: " + getLogInfo(vertShader));

        ARBShaderObjects.glShaderSourceARB(fragShader, readShader(frag));
        ARBShaderObjects.glCompileShaderARB(fragShader);
        if (ARBShaderObjects.glGetObjectParameteriARB(fragShader, ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB) == GL11.GL_FALSE)
            throw new RuntimeException("Error creating shader: " + getLogInfo(fragShader));

        program = ARBShaderObjects.glCreateProgramObjectARB();
        ARBShaderObjects.glAttachObjectARB(program, vertShader);
        ARBShaderObjects.glAttachObjectARB(program, fragShader);
        ARBShaderObjects.glLinkProgramARB(program);
        if (ARBShaderObjects.glGetObjectParameteriARB(program, ARBShaderObjects.GL_OBJECT_LINK_STATUS_ARB) == GL11.GL_FALSE) {
            throw new RuntimeException("Error creating shader: " + getLogInfo(program));
        }
        ARBShaderObjects.glValidateProgramARB(program);
        if (ARBShaderObjects.glGetObjectParameteriARB(program, ARBShaderObjects.GL_OBJECT_VALIDATE_STATUS_ARB) == GL11.GL_FALSE) {
            throw new RuntimeException("Error creating shader: " + getLogInfo(program));
        }
    }

    private static String getLogInfo(int obj) {
        return ARBShaderObjects.glGetInfoLogARB(obj, ARBShaderObjects.glGetObjectParameteriARB(obj, ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB));
    }

    /** Bind the shader, make sure you call restore() or put this in a try block */
    public OpenGL.With bind() {
        int oldProc = ARBShaderObjects.glGetHandleARB(ARBShaderObjects.GL_PROGRAM_OBJECT_ARB);
        ARBShaderObjects.glUseProgramObjectARB(program);
        return () -> ARBShaderObjects.glUseProgramObjectARB(oldProc);
    }

    private final Map<String, Integer> paramLocations = new HashMap<>();
    /** Set the param to the given values (up to 3) */
    public void paramFloat(String name, float... params) {
        Integer loc = paramLocations.computeIfAbsent(name, n -> ARBShaderObjects.glGetUniformLocationARB(program, n));

        switch (params.length) {
            case 1:
                ARBShaderObjects.glUniform1fARB(loc, params[0]);
                break;
            case 2:
                ARBShaderObjects.glUniform2fARB(loc, params[0], params[1]);
                break;
            case 3:
                ARBShaderObjects.glUniform3fARB(loc, params[0], params[1], params[2]);
                break;
        }
    }

    private String readShader(Identifier fname) {
        InputStream input;
        try {
            input = fname.getResourceStream();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error reading shader " + fname);
        }
        Scanner reader = new Scanner(input);
        String text = "";
        while (reader.hasNextLine()) {
            text = text + reader.nextLine();
        }
        reader.close(); // closes input
        return text;
    }
}
