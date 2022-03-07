package cam72cam.mod.render;

import cam72cam.mod.ModCore;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.render.opengl.LegacyRenderContext;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.Texture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraftforge.fml.common.Loader;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/** Internal(ish) class for representing a GL texture */
public class GLTexture {
    // Thread Pools for reading and saving textures
    private static final LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(1);
    private static final ExecutorService saveImage = new ThreadPoolExecutor(5, 5, 60, TimeUnit.SECONDS, queue);
    private static final ExecutorService readImage = Executors.newFixedThreadPool(1);
    // All currently known textures
    private static final Map<String, GLTexture> textures = new HashMap<>();
    
    private final File texLoc;
    private final int cacheSeconds;
    private int width;
    private int height;
    private int glTexID;
    private long lastUsed;
    private IntBuffer pixels;
    private TextureState state;
    private RuntimeException internalError;

    private enum TextureState {
        NEW,
        WRITING,
        READING,
        READ,
        ALLOCATED,
        UNALLOCATED,
        ERROR
    }

    static {
        // free unused textures
        ClientEvents.TICK.subscribe(() -> {
            for (GLTexture texture : textures.values()) {
                if (texture.state == TextureState.ALLOCATED && System.currentTimeMillis() - texture.lastUsed > texture.cacheSeconds * 1000) {
                    texture.dealloc();
                }
            }
        });
    }

    /** Get a file for name in the UMC cache dir */
    public static File cacheFile(String name) {
        File cacheDir = Paths.get(Loader.instance().getConfigDir().getParentFile().getPath(), "cache", "universalmodcore").toFile();
        cacheDir.mkdirs();

        return new File(cacheDir, name);
    }

    public GLTexture(String name, BufferedImage image, int cacheSeconds, boolean upload) {
        this.texLoc = cacheFile(name);
        this.cacheSeconds = cacheSeconds;

        if (image != null) {
            this.width = image.getWidth();
            this.height = image.getHeight();

            transition(TextureState.NEW);

            transition(TextureState.WRITING);
            if (upload) {
                try {
                    ImageIO.write(image, "png", texLoc);
                } catch (IOException e) {
                    internalError = new RuntimeException(e);
                    transition(TextureState.ERROR);
                    texLoc.delete();
                    throw internalError;
                }
                transition(TextureState.UNALLOCATED);

                this.pixels = imageToPixels(image);
                transition(TextureState.READ);
                tryUpload();
            } else {
                while (queue.size() != 0) {
                    try {
                        Thread.sleep(1000);
                        ModCore.info("Waiting for free write slot...");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                saveImage.submit(() -> {
                    try {
                        ImageIO.write(image, "png", texLoc);
                        transition(TextureState.UNALLOCATED);
                    } catch (IOException e) {
                        internalError = new RuntimeException("Unable to save image " + texLoc, e);
                        transition(TextureState.ERROR);
                        texLoc.delete();
                        throw internalError;
                    }
                });
            }
        } else {
            transition(TextureState.UNALLOCATED);
            if (upload) {
                for (int i = 0; i< 100; i++) {
                    if (tryUpload()) {
                        break;
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        // PASS
                    }
                }
            }
        }

        textures.put(texLoc.toString(), this);
    }

    private void transition(TextureState state) {
        this.state = state;
        //ModCore.info(state.name() + " " + texLoc);
    }

    private IntBuffer imageToPixels(BufferedImage image) {
        // Will dump out inside a loading thread if prematurely free'd
        assert state == TextureState.READ;
        width = image.getWidth();
        height = image.getHeight();

        int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
        IntBuffer buffer = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * 4).asIntBuffer();
        buffer.put(pixels);
        buffer.flip();
        return buffer;
    }

    private int uploadTexture() {
        this.lastUsed = System.currentTimeMillis();
        int textureID = GL11.glGenTextures();
        try (OpenGL.With ctx = LegacyRenderContext.INSTANCE.apply(new RenderState().texture(new Texture(textureID)))) {
            TextureUtil.allocateTexture(textureID, width, height);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, pixels);
            //GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, width, height, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, pixels);
            pixels = null;
            transition(TextureState.ALLOCATED);
        }
        return textureID;
    }

    /** Can this texture be bound? */
    public boolean isLoaded() {
        return state == TextureState.ALLOCATED;
    }

    /** Try to read and then upload the texture */
    public boolean tryUpload() {
        switch (this.state) {
            case NEW:
            case WRITING:
            case READING:
                return false;
            case READ:
                this.glTexID = uploadTexture();
                return true;
            case ALLOCATED:
                return true;
            case UNALLOCATED:
                transition(TextureState.READING);
                readImage.submit(() -> {
                    try {
                        this.pixels = imageToPixels(ImageIO.read(texLoc));
                        transition(TextureState.READ);
                    } catch (Exception e) {
                        ModCore.warn("Unable to read file " + texLoc.toString() + ".  The cache file has been removed.  Please try launching again.  If this error happens multiple times, try removing your .minecraft/cache/ directory.");
                        transition(TextureState.ERROR);
                        internalError = new RuntimeException(texLoc.toString(), e);
                        texLoc.delete();
                        throw internalError;
                    }
                });
                return false;
            case ERROR:
                throw internalError;
        }

        throw new RuntimeException(this.state.toString());
    }

    /** Bind the texture, force waiting if specified */
    public void bind(RenderState ctx, boolean force) {
        lastUsed = System.currentTimeMillis();

        if (force) {
            // Wait up to 1 second for texture to load
            // Should be fine for the icons we use this with
            for (int i = 0; i < 100; i++) {
                if (tryUpload()) {
                    break;
                }
                try {
                    Thread.sleep((long) 10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        if (!tryUpload()) {
            return;
        }
        ctx.texture(new Texture(glTexID));
    }

    /** Completely free this texture */
    public void freeGL() {
        textures.remove(this.texLoc.toString());

        switch (state) {
            case ALLOCATED:
                dealloc();
            default:
                transition(TextureState.UNALLOCATED);
        }
    }

    /** free this texture from the GPU */
    public void dealloc() {
        if (this.state == TextureState.ALLOCATED) {
            GL11.glDeleteTextures(this.glTexID);
            transition(TextureState.UNALLOCATED);
        }
    }
}
