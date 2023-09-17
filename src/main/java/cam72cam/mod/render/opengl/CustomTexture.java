package cam72cam.mod.render.opengl;

import cam72cam.mod.Config;
import cam72cam.mod.ModCore;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.util.With;
import org.lwjgl.opengl.GL32;

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
            try {
                synchronized (textures) {
                    for (CustomTexture texture : textures) {
                        if (texture.textureID != null && System.currentTimeMillis() - texture.lastUsed > texture.cacheSeconds * 1000 && (texture.loader == null || !texture.loader.isDone())) {
                            texture.dealloc();
                        }
                    }
                }
            } catch (Exception ex) {
                ModCore.catching(ex);
            }
        });
    }


    public CustomTexture(int width, int height, int cacheSeconds) {
        synchronized (textures) {
            textures.add(this);
        }
        this.width = width;
        this.height = height;
        this.cacheSeconds = cacheSeconds;
    }

    protected abstract ByteBuffer getData();
    protected int internalGLFormat() {
        return GL32.GL_RGBA;
    }

    private void createTexture(ByteBuffer buffer) {
        textureID = GL32.glGenTextures();
        try (With ctx = RenderContext.apply(new RenderState().texture(Texture.wrap(textureID)))) {
            GL32.glPixelStorei(GL32.GL_UNPACK_SWAP_BYTES, GL32.GL_FALSE);
            GL32.glPixelStorei(GL32.GL_UNPACK_LSB_FIRST, GL32.GL_FALSE);
            GL32.glPixelStorei(GL32.GL_UNPACK_ROW_LENGTH, 0);
            GL32.glPixelStorei(GL32.GL_UNPACK_SKIP_ROWS, 0);
            GL32.glPixelStorei(GL32.GL_UNPACK_SKIP_PIXELS, 0);
            GL32.glPixelStorei(GL32.GL_UNPACK_ALIGNMENT, 4);

            GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_MIN_FILTER, GL32.GL_NEAREST);
            GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_MAG_FILTER, GL32.GL_NEAREST);
            GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_WRAP_S, GL32.GL_CLAMP_TO_EDGE);
            GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_WRAP_T, GL32.GL_CLAMP_TO_EDGE);

            GL32.glTexImage2D(GL32.GL_TEXTURE_2D, 0, internalGLFormat(), width, height, 0, GL32.GL_RGBA, GL32.GL_UNSIGNED_BYTE, buffer);
        }
    }

    private void threadedLoader() {
        synchronized (textures) {
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
        synchronized (textures) {
            if (this.textureID != null) {
                GL32.glDeleteTextures(this.textureID);
                this.textureID = null;
                this.loader = null;
            }
        }
    }
}
