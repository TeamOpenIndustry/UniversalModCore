package cam72cam.mod.render;

import cam72cam.mod.render.opengl.DirectDraw;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.*;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A custom sprite sheet which can span multiple texture sheets
 */
public class SpriteSheet {
    public final int spriteSize;
    private final Map<Identifier, SpriteInfo> sprites = new HashMap<>();
    private final List<SpriteInfo> unallocated = new ArrayList<>();
    private GpuTextureView view;
    private NativeImage holder;

    /**
     * sprite width/height in px
     */
    public SpriteSheet(int spriteSize) {
        this.spriteSize = spriteSize;
    }

    /**
     * Create new blank sheet and add slots to unallocated
     */
    private void allocateSheet() {
        int sheetSize = Math.min(1024, GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE));
        GpuTexture tex = RenderSystem.getDevice().createTexture("UMC",
                                                                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
                                                                TextureFormat.RGBA8, sheetSize, sheetSize, 1, 1);
        tex.setTextureFilter(FilterMode.NEAREST, FilterMode.NEAREST, false);
        tex.setAddressMode(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE);
        this.view = RenderSystem.getDevice().createTextureView(tex);
        this.holder = new NativeImage(sheetSize, sheetSize, false);

        for (int uPx = 0; uPx < sheetSize; uPx += spriteSize) {
            for (int vPx = 0; vPx < sheetSize; vPx += spriteSize) {
                float u = uPx / (float) sheetSize;
                float uMax = (uPx + spriteSize) / (float) sheetSize;
                float v = vPx / (float) sheetSize;
                float vMax = (vPx + spriteSize) / (float) sheetSize;
                unallocated.add(new SpriteInfo(u, uMax, uPx, v, vMax, vPx, view, sheetSize));
            }
        }
    }

    /**
     * Allocate a slot in the sheet and write pixels to it
     */
    public void setSprite(Identifier id, ByteBuffer pixels) {
        if (!sprites.containsKey(id)) {
            if (unallocated.isEmpty()) {
                allocateSheet();
            }
            sprites.put(id, unallocated.remove(0));
        }
        SpriteInfo sprite = sprites.get(id);
        byte[] data = new byte[pixels.capacity()];
        pixels.get(data);

        int idx = 0;
        for (int x = sprite.uPx; x < sprite.uPx + spriteSize; x++) {
            for (int y = sprite.vPx; y < sprite.vPx + spriteSize; y++) {
                int col = data[idx++] >> 8 | data[idx++] >> 8 | data[idx++] >> 8 | data[idx++] << 24;
                holder.setPixelABGR(x, y, col);
            }
        }
    }

    /**
     * Render the sprite represented by id (skip if unknown)
     */
    public void renderSprite(Identifier id, RenderState state) {
        SpriteInfo sprite = sprites.get(id);
        if (sprite == null) {
            return;
        }
        state.texture(() -> sprite.view)
                .rotate(180, 1, 0, 0)
                .translate(0, -1, 0);
        DirectDraw buffer = new DirectDraw();
        buffer.vertex(0, 0, 0).color(1, 1, 1, 1).uv(sprite.uMin, sprite.vMin);
        buffer.vertex(0, 1, 0).color(1, 1, 1, 1).uv(sprite.uMin, sprite.vMax);
        buffer.vertex(1, 1, 0).color(1, 1, 1, 1).uv(sprite.uMax, sprite.vMax);
        buffer.vertex(1, 0, 0).color(1, 1, 1, 1).uv(sprite.uMax, sprite.vMin);
        buffer.draw(state);
    }

    /**
     * Remove a sprite from the sheet (does not reduce used GPU memory yet)
     */
    public void freeSprite(Identifier id) {
        unallocated.add(sprites.remove(id));
        // TODO shrink number of sheets?
    }

    private static class SpriteInfo {
        final float uMin;
        final float uMax;
        final int uPx;
        final float vMin;
        final float vMax;
        final int vPx;
        final GpuTextureView view;
        private final int sheetSize;

        private SpriteInfo(float u, float uMax, int uPx, float v, float vMax, int vPx, GpuTextureView view, int sheetSize) {
            this.uMin = u;
            this.uMax = uMax;
            this.uPx = uPx;
            this.vMin = v;
            this.vMax = vMax;
            this.vPx = vPx;
            this.view = view;
            this.sheetSize = sheetSize;
        }
    }
}
