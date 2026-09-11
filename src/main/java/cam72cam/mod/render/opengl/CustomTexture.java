package cam72cam.mod.render.opengl;

import cam72cam.mod.Config;
import cam72cam.mod.ModCore;
import cam72cam.mod.event.ClientEvents;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.*;
import org.lwjgl.opengl.GL32;

import java.io.IOException;
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
    private GpuTextureView view;

    private static final List<CustomTexture> textures = new ArrayList<>();

    public static void registerClientEvents() {
        // free unused textures
        ClientEvents.TICK.subscribe(() -> {
            try {
                synchronized (textures) {
                    for (CustomTexture texture : textures) {
                        if (texture.view != null && System.currentTimeMillis() - texture.lastUsed > texture.cacheSeconds * 1000L && (texture.loader == null || !texture.loader.isDone())) {
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
        GpuTexture tex = RenderSystem.getDevice().createTexture("UMC",
                                               GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_TEXTURE_BINDING,
                                               TextureFormat.RGBA8, width, height, 1, 1);
        tex.setTextureFilter(FilterMode.NEAREST, FilterMode.NEAREST, false);
        tex.setAddressMode(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE);
        RenderSystem.getDevice().createCommandEncoder().writeToTexture(tex, buffer.asIntBuffer(), NativeImage.Format.RGBA,
                                                                       0, 0, 0, 0, width, height);
        this.view = RenderSystem.getDevice().createTextureView(tex);
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

        if (view == null) {
            if (sync) {
                directLoader();
            } else {
                return this;
            }
        }
        return () -> view;
    }

    public boolean isLoaded() {
        return view != null;
    }

    @Override
    public GpuTextureView getTexView() {
        lastUsed = System.currentTimeMillis();

        if (view == null) {
            if (Config.ThreadedTextureLoading) {
                threadedLoader();
            } else {
                directLoader();
            }
        }
        return view == null ? NO_TEXTURE.getTexView() : this.view;
    }

    public void dealloc() {
        synchronized (textures) {
            if (this.view != null) {
                this.view.close();
            }
            this.view = null;
            this.loader = null;
        }
    }
}
