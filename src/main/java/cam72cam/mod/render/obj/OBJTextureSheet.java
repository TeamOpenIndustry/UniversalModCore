package cam72cam.mod.render.obj;

import cam72cam.mod.Config;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.render.OpenGL;
import cam72cam.mod.render.opengl.LegacyRenderContext;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.Texture;
import cam72cam.mod.serialization.ResourceCache;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class OBJTextureSheet {
    private final int width;
    private final int height;
    private final Supplier<ResourceCache.GenericByteBuffer> data;
    private Thread loader = null;
    private ByteBuffer buffer = null;
    private final int cacheSeconds;
    private long lastUsed;
    private Integer textureID;
    private OBJTextureSheet fallback;

    private static final List<OBJTextureSheet> textures = new ArrayList<>();

    static {
        // free unused textures
        ClientEvents.TICK.subscribe(() -> {
            for (OBJTextureSheet texture : textures) {
                if (texture.textureID != null && System.currentTimeMillis() - texture.lastUsed > texture.cacheSeconds * 1000 && (texture.loader == null || !texture.loader.isAlive())) {
                    texture.dealloc();
                }
            }
        });
    }


    OBJTextureSheet(int width, int height, Supplier<ResourceCache.GenericByteBuffer> data, int cacheSeconds) {
        this(width, height, data, cacheSeconds, null);
    }

    OBJTextureSheet(int width, int height, Supplier<ResourceCache.GenericByteBuffer> data, int cacheSeconds, OBJTextureSheet fallback) {
        this.width = width;
        this.height = height;
        this.textureID = null;
        this.data = data;
        this.cacheSeconds = cacheSeconds;
        this.lastUsed = 0;
        this.fallback = fallback;

        textures.add(this);
    }

    private void loadData() {
        byte[] raw = data.get().bytes();
        ByteBuffer buffer = ByteBuffer.allocateDirect(raw.length);
        buffer.put(raw);
        buffer.flip();
        synchronized (this) {
            this.buffer = buffer;
        }
    }

    private void createTexture() {
        textureID = GL11.glGenTextures();
        try (OpenGL.With ctx = LegacyRenderContext.INSTANCE.apply(new RenderState().texture(new Texture(textureID)))) {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        }
        loader = null;
        buffer = null;
    }

    private void threadedLoader() {
        synchronized (this) {
            if (loader != null) {
                // Loading thread in progress
                if (buffer != null) {
                    // We have the data ready
                    createTexture();
                }
            } else {
                // Start thread
                loader = new Thread(this::loadData);
                loader.setName("UMC-TextureLoader");
                loader.start();
            }
        }
    }

    private void directLoader() {
        loadData();
        createTexture();
    }

    Texture texture(boolean wait) {
        lastUsed = System.currentTimeMillis();

        if (textureID == null) {
            if (Config.ThreadedTextureLoading && fallback != null && !wait) {
                threadedLoader();
            } else {
                directLoader();
            }
        }
        return textureID == null ? fallback.texture(wait) : new Texture(this.textureID);
    }

    public void dealloc() {
        if (this.textureID != null) {
            GL11.glDeleteTextures(this.textureID);
            this.textureID = null;
            this.buffer = null;
            this.loader = null;
        }
    }

    void freeGL() {
        dealloc();
        textures.remove(this);
    }
}
