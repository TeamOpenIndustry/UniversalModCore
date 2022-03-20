package cam72cam.mod.render.opengl;

import cam72cam.mod.Config;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.util.With;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public abstract class CustomTexture implements Texture {
    private final int width;
    private final int height;
    private final int cacheSeconds;
    private final Texture fallback;

    private Thread loader = null;
    private ByteBuffer buffer = null;
    private long lastUsed;
    private Integer textureID;

    private static final List<CustomTexture> textures = new ArrayList<>();

    static {
        // free unused textures
        ClientEvents.TICK.subscribe(() -> {
            for (CustomTexture texture : textures) {
                if (texture.textureID != null && System.currentTimeMillis() - texture.lastUsed > texture.cacheSeconds * 1000 && (texture.loader == null || !texture.loader.isAlive())) {
                    texture.dealloc();
                }
            }
        });
    }


    public CustomTexture(int width, int height, int cacheSeconds, Texture fallback) {
        textures.add(this);
        this.width = width;
        this.height = height;
        this.cacheSeconds = cacheSeconds;
        this.fallback = fallback;
    }

    protected abstract ByteBuffer getData();
    protected int internalGLFormat() {
        return GL11.GL_RGBA;
    }

    private void createTexture() {
        textureID = GL11.glGenTextures();
        try (With ctx = LegacyRenderContext.INSTANCE.apply(new RenderState().texture(Texture.wrap(textureID)))) {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, internalGLFormat(), width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        }
        loader = null;
        buffer = null;
    }

    private void loadData() {
        this.buffer = getData();
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

    public Texture synchronous(boolean sync) {
        lastUsed = System.currentTimeMillis();

        if (textureID == null) {
            if (sync) {
                directLoader();
            } else {
                return this;
            }
        }
        return () -> textureID;
    }

    @Override
    public int getId() {
        lastUsed = System.currentTimeMillis();

        if (textureID == null) {
            if (Config.ThreadedTextureLoading && fallback != null) {
                threadedLoader();
            } else {
                directLoader();
            }
        }
        return textureID == null ? fallback != null ? fallback.getId() : NO_TEXTURE.getId() : this.textureID;
    }

    public void dealloc() {
        if (this.textureID != null) {
            GL11.glDeleteTextures(this.textureID);
            this.textureID = null;
            this.buffer = null;
            this.loader = null;
        }
    }

    public void freeGL() {
        dealloc();
        textures.remove(this);
    }
}
