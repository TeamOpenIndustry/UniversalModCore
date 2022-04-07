package cam72cam.mod.render.opengl;

import cam72cam.mod.Config;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.util.With;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public abstract class CustomTexture implements Texture {
    private final int width;
    private final int height;
    private final int cacheSeconds;

    private static final ExecutorService pool = Executors.newFixedThreadPool(1, runnable -> {
        Thread thread = new Thread(runnable);
        thread.setName("UMC-TextureLoader");
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });

    private Future<ByteBuffer> loader = null;
    private long lastUsed;
    private Integer textureID;

    private static final List<CustomTexture> textures = new ArrayList<>();

    public static void registerClientEvents() {
        // free unused textures
        ClientEvents.TICK.subscribe(() -> {
            synchronized (textures) {
                for (CustomTexture texture : textures) {
                    if (texture.textureID != null && System.currentTimeMillis() - texture.lastUsed > texture.cacheSeconds * 1000 && (texture.loader == null || !texture.loader.isDone())) {
                        texture.dealloc();
                    }
                }
            }
        });
    }


    public CustomTexture(int width, int height, int cacheSeconds) {
        textures.add(this);
        this.width = width;
        this.height = height;
        this.cacheSeconds = cacheSeconds;
    }

    protected abstract ByteBuffer getData();
    protected int internalGLFormat() {
        return GL11.GL_RGBA;
    }

    private void createTexture(ByteBuffer buffer) {
        textureID = GL11.glGenTextures();
        try (With ctx = RenderContext.apply(new RenderState().texture(Texture.wrap(textureID)))) {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, internalGLFormat(), width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        }
    }

    private void threadedLoader() {
        synchronized (this) {
            if (loader != null) {
                if (loader.isDone()) {
                    try {
                        createTexture(loader.get());
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                    loader = null;
                }
            } else {
                // Start thread
                loader = pool.submit(this::getData);
            }
        }
    }

    private void directLoader() {
        createTexture(getData());
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

    public boolean isLoaded() {
        return textureID != null;
    }

    @Override
    public int getId() {
        lastUsed = System.currentTimeMillis();

        if (textureID == null) {
            if (Config.ThreadedTextureLoading) {
                threadedLoader();
            } else {
                directLoader();
            }
        }
        return textureID == null ? NO_TEXTURE.getId() : this.textureID;
    }

    public void dealloc() {
        if (this.textureID != null) {
            GL11.glDeleteTextures(this.textureID);
            this.textureID = null;
            this.loader = null;
        }
    }
}
