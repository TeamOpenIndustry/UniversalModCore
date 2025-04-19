package cam72cam.mod.render.opengl;

import cam72cam.mod.Config;
import cam72cam.mod.ModCore;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.resource.Identifier;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL32;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public abstract class CustomTexture implements Texture {
    private final int width;
    private final int height;
    private final int cacheSeconds;

    public ResourceLocation dynamicLocation;
    public static TextureManager MANAGER;

    private static final ExecutorService pool = Executors.newFixedThreadPool(1, runnable -> {
        Thread thread = new Thread(runnable);
        thread.setName("UMC-TextureLoader");
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });

    private Future<ByteBuffer> loader = null;
    private long lastUsed;
    private Integer glID;

    public Identifier textureLocation;

    private static final List<CustomTexture> textures = new ArrayList<>();

    public static void registerClientEvents() {
        // free unused textures
        ClientEvents.TICK.subscribe(() -> {
            try {
                synchronized (textures) {
                    for (CustomTexture texture : textures) {
                        if (texture.glID != null
                                && System.currentTimeMillis() - texture.lastUsed > texture.cacheSeconds * 1000
                                && (texture.loader == null || !texture.loader.isDone())) {
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
        if(MANAGER == null){
            MANAGER = Minecraft.getInstance().getTextureManager();
        }

        IntBuffer intBuffer = buffer.asIntBuffer();
        int length = intBuffer.limit() - intBuffer.position();
        int[] ints = new int[length];
        intBuffer.get(ints);
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        bufferedImage.setRGB(0, 0, width, height, ints, 0, width);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            ImageIO.write(bufferedImage, "PNG", stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        NativeImage image;
        try {
            ByteBuffer byteBuffer = BufferUtils.createByteBuffer(stream.toByteArray().length);
            byteBuffer.put(stream.toByteArray());
            byteBuffer.position(0);
            image = NativeImage.read(NativeImage.Format.RGBA, byteBuffer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        AbstractTexture abstractTexture = new DynamicTexture(image);
        abstractTexture.setFilter(false, false);
        this.glID = abstractTexture.getId();
        textureLocation = new Identifier(ModCore.MODID, "tex" + glID);

        MANAGER.register(textureLocation.internal, abstractTexture);
//        try (With ctx = RenderContext.apply(new RenderState().texture(Texture.wrap(glID)))) {
//            GL32.glPixelStorei(GL32.GL_UNPACK_SWAP_BYTES, GL32.GL_FALSE);
//            GL32.glPixelStorei(GL32.GL_UNPACK_LSB_FIRST, GL32.GL_FALSE);
//            GL32.glPixelStorei(GL32.GL_UNPACK_ROW_LENGTH, 0);
//            GL32.glPixelStorei(GL32.GL_UNPACK_SKIP_ROWS, 0);
//            GL32.glPixelStorei(GL32.GL_UNPACK_SKIP_PIXELS, 0);
//            GL32.glPixelStorei(GL32.GL_UNPACK_ALIGNMENT, 4);
//
//            GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_MIN_FILTER, GL32.GL_NEAREST);
//            GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_MAG_FILTER, GL32.GL_NEAREST);
//            GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_WRAP_S, GL32.GL_CLAMP_TO_EDGE);
//            GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_WRAP_T, GL32.GL_CLAMP_TO_EDGE);
//
//            GL32.glTexImage2D(GL32.GL_TEXTURE_2D, 0, internalGLFormat(), width, height, 0, GL32.GL_RGBA, GL32.GL_UNSIGNED_BYTE, buffer);
//        }
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

        if (glID == null) {
            if (sync) {
                directLoader();
            } else {
                return this;
            }
        }
        return () -> glID;
    }

    public boolean isLoaded() {
        return glID != null;
    }

    @Override
    public int getId() {
        lastUsed = System.currentTimeMillis();

        if (glID == null) {
            if (Config.ThreadedTextureLoading) {
                threadedLoader();
            } else {
                directLoader();
            }
        }
        return glID == null ? NO_TEXTURE.getId() : this.glID;
    }

    public void dealloc() {
        synchronized (textures) {
            if (this.glID != null) {
                MANAGER.release(this.textureLocation.internal);
                this.glID = null;
                this.textureLocation = null;
                this.loader = null;
            }
        }
    }
}
