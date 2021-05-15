package cam72cam.mod.render.obj;

import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.render.OpenGL;
import cam72cam.mod.resource.Identifier;
import com.mojang.blaze3d.platform.TextureUtil;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import net.minecraft.util.math.MathHelper;
import cam72cam.mod.serialization.ResourceCache;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class OBJTextureSheet {
    private final int width;
    private final int height;
    private final Supplier<ResourceCache.GenericByteBuffer> data;
    private final int cacheSeconds;
    private long lastUsed;
    private Integer textureID;

    private static final List<OBJTextureSheet> textures = new ArrayList<>();

    static {
        // free unused textures
        ClientEvents.TICK.subscribe(() -> {
            for (OBJTextureSheet texture : textures) {
                if (texture.textureID != null && System.currentTimeMillis() - texture.lastUsed > texture.cacheSeconds * 1000) {
                    texture.dealloc();
                }
            }
        });
    }

    OBJTextureSheet(int width, int height, Supplier<ResourceCache.GenericByteBuffer> data, int cacheSeconds) {
        this.width = width;
        this.height = height;
        this.textureID = null;
        this.data = data;
        this.cacheSeconds = cacheSeconds;
        this.lastUsed = 0;

        textures.add(this);
    }

    OpenGL.With bind() {
        lastUsed = System.currentTimeMillis();

        if (textureID == null) {
            textureID = GL11.glGenTextures();

            try (OpenGL.With tex = OpenGL.texture(textureID)) {
                TextureUtil.prepareImage(textureID, width, height);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

                byte[] raw = data.get().bytes();
                ByteBuffer buffer = ByteBuffer.allocateDirect(raw.length);
                buffer.put(raw);
                buffer.flip();

                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
            }
        }
        return OpenGL.texture(this.textureID);
    }

    public void dealloc() {
        if (this.textureID != null) {
            GL11.glDeleteTextures(this.textureID);
            this.textureID = null;
        }
    }

    void freeGL() {
        dealloc();
        textures.remove(this);
    }
}
