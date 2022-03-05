package cam72cam.mod.render;

import cam72cam.mod.resource.Identifier;
import net.minecraft.client.renderer.OpenGlHelper;
import org.apache.commons.io.IOUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBFragmentShader;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.GL11;
import util.Matrix4;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

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

    public void paramInt(String name, int i) {
        int loc = paramLocations.computeIfAbsent(name, n -> ARBShaderObjects.glGetUniformLocationARB(program, n));
        ARBShaderObjects.glUniform1iARB(loc, i);
    }

    private final Map<String, Integer> paramLocations = new HashMap<>();
    /** Set the param to the given values (up to 4) */
    public void paramFloat(String name, float... params) {
        int loc = paramLocations.computeIfAbsent(name, n -> ARBShaderObjects.glGetUniformLocationARB(program, n));

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
            case 4:
                ARBShaderObjects.glUniform4fARB(loc, params[0], params[1], params[2], params[3]);
                break;
        }
    }

    private final FloatBuffer uniformFloatBuffer = BufferUtils.createFloatBuffer(16);
    public void paramMatrix(String name, Matrix4 m) {
        int loc = paramLocations.computeIfAbsent(name, n -> ARBShaderObjects.glGetUniformLocationARB(program, n));

        uniformFloatBuffer.position(0);
        uniformFloatBuffer.put(new float[]{
                (float) m.m00, (float) m.m01, (float) m.m02, (float) m.m03, (float) m.m10, (float) m.m11, (float) m.m12, (float) m.m13, (float) m.m20, (float) m.m21, (float) m.m22, (float) m.m23, (float) m.m30, (float) m.m31, (float) m.m32, (float) m.m33
        });
        uniformFloatBuffer.position(0);

        OpenGlHelper.glUniformMatrix4(loc, true, this.uniformFloatBuffer);
    }

    private String readShader(Identifier fname) {
        InputStream input;
        try {
            input = fname.getResourceStream();
            return IOUtils.toString(input, Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error reading shader " + fname);
        }
    }
}
