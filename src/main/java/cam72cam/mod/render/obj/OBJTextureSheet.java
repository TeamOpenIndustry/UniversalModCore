package cam72cam.mod.render.obj;

import cam72cam.mod.render.OpenGL;
import cam72cam.mod.serialization.ResourceCache;
import net.minecraft.client.renderer.texture.TextureUtil;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

public class OBJTextureSheet {
    private final int textureID;

    OBJTextureSheet(int width, int height, Supplier<ResourceCache.GenericByteBuffer> texPrefix, int cacheSeconds) {
        textureID = GL11.glGenTextures();

        try (OpenGL.With tex = OpenGL.texture(textureID)) {
            TextureUtil.allocateTexture(textureID, width, height);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

            byte[] raw = texPrefix.get().bytes();
            ByteBuffer buffer = ByteBuffer.allocateDirect(raw.length);
            buffer.put(raw);
            buffer.flip();

            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        }
    }

    void freeGL() {
        //GL11.glDeleteTextures(this.textureID);
    }

    public void dealloc() {
        //GL11.glDeleteTextures(this.textureID);
    }

    OpenGL.With bind() {
        return OpenGL.texture(this.textureID);
    }

    OpenGL.With bindIcon() {
        return bind();
    }
}
