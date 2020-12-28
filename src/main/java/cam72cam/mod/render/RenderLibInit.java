package cam72cam.mod.render;

import cam72cam.mod.resource.IdentifierFileContainer;
import friedrichlp.renderlib.RenderLibRegistry;
import friedrichlp.renderlib.RenderLibSettings;
import friedrichlp.renderlib.math.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLContext;

import java.io.File;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class RenderLibInit {
    private static boolean initialized = false;

    public static void initRenderLib(File configDir) {
        // Prevent initializing RenderLib twice
        if (initialized) {
            return;
        }
        initialized = true;

        RenderLibRegistry.Compatibility.MODEL_VIEW_PROJECTION_PROVIDER = () -> {
            FloatBuffer modelView = BufferUtils.createFloatBuffer(16);
            FloatBuffer projection = BufferUtils.createFloatBuffer(16);
            for(int i = 0; i < 16; i++) {
                modelView.put(0);
                projection.put(0);
            }
            modelView.flip();
            projection.flip();
            GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelView);
            GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
            return new Matrix4f(modelView).multiply(new Matrix4f(projection));
        };
        RenderLibRegistry.Compatibility.MODEL_VIEW_PROVIDER = () -> {
            FloatBuffer modelView = BufferUtils.createFloatBuffer(16);
            for(int i = 0; i < 16; i++) modelView.put(0);
            modelView.flip();
            GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelView);
            return new Matrix4f(modelView);
        };
        RenderLibRegistry.Compatibility.PROJECTION_PROVIDER = () -> {
            FloatBuffer modelView = BufferUtils.createFloatBuffer(16);
            for(int i = 0; i < 16; i++) modelView.put(0);
            modelView.flip();
            GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, modelView);
            return new Matrix4f(modelView);
        };
        RenderLibRegistry.Compatibility.GL_SET_MATRIX_PARAMETER = (program, loc, buf) -> ARBShaderObjects.glUniformMatrix4ARB(loc, false, buf);
        RenderLibRegistry.Compatibility.GL_GET_VERTEX_ATTRIB = (index, name) -> {
            IntBuffer params = BufferUtils.createIntBuffer(4);
            GL20.glGetVertexAttrib(index, name, params);
            return params.get(0);
        };
        RenderLibRegistry.Compatibility.GL_HAS_CONTEXT = () -> {
            try {
                GLContext.getCapabilities();
                return true;
            } catch (IllegalStateException e) {
                return false;
            }
        };

        RenderLibRegistry.FileContainer.register(IdentifierFileContainer.class);

        RenderLibSettings.General.MODEL_LOAD_LIMIT = 10;
        File cacheLoc = new File(configDir, "../cache/renderlib");
        RenderLibSettings.Caching.CACHE_LOCATION = cacheLoc.getAbsolutePath();
        RenderLibSettings.Caching.CACHE_VERSION = "1";
        RenderLibSettings.Rendering.CAMERA_HEIGHT_OFFSET = 1.62f;
        //RenderLibSettings.General.MODEL_UNLOAD_DELAY_MS = 20000.0f;
    }
}
