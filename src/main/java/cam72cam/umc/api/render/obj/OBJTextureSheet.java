package cam72cam.umc.api.render.obj;

import cam72cam.umc.api.render.opengl.CustomTexture;
import cam72cam.umc.api.serialization.ResourceCache;
import net.minecraft.client.renderer.GLAllocation;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

public class OBJTextureSheet extends CustomTexture {
    private final Supplier<ResourceCache.GenericByteBuffer> data;

    public OBJTextureSheet(int width, int height, Supplier<ResourceCache.GenericByteBuffer> data, int cacheSeconds) {
        super(width, height, cacheSeconds);
        this.data = data;
    }

    @Override
    protected ByteBuffer getData() {
        byte[] raw = data.get().bytes();
        ByteBuffer buffer = GLAllocation.createDirectByteBuffer(raw.length);
        buffer.put(raw);
        buffer.flip();
        return buffer;
    }
}
