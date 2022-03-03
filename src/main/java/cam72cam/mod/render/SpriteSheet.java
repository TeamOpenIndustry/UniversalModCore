package cam72cam.mod.render;

import cam72cam.mod.render.opengl.LegacyRenderContext;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.Texture;
import net.minecraft.client.renderer.texture.TextureUtil;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A custom sprite sheet which can span multiple texture sheets */
public class SpriteSheet {
    public final int spriteSize;
    private final Map<String, SpriteInfo> sprites = new HashMap<>();
    private final List<SpriteInfo> unallocated = new ArrayList<>();
    /** sprite width/height in px */
    public SpriteSheet(int spriteSize) {
        this.spriteSize = spriteSize;
    }

    /** Create new blank sheet and add slots to unallocated */
    private void allocateSheet() {
        int textureID = GL11.glGenTextures();
        try (OpenGL.With ctx = LegacyRenderContext.INSTANCE.apply(new RenderState().texture(new Texture(textureID)))) {
            int sheetSize = Math.min(1024, GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE));
            TextureUtil.allocateTexture(textureID, sheetSize, sheetSize);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            for (int uPx = 0; uPx < sheetSize; uPx += spriteSize) {
                for (int vPx = 0; vPx < sheetSize; vPx += spriteSize) {
                    float u = uPx / (float) sheetSize;
                    float uMax = (uPx + spriteSize) / (float) sheetSize;
                    float v = vPx / (float) sheetSize;
                    float vMax = (vPx + spriteSize) / (float) sheetSize;
                    unallocated.add(new SpriteInfo(u, uMax, uPx, v, vMax, vPx, textureID));
                }
            }
        }
    }

    /** Allocate a slot in the sheet and write pixels to it */
    public void setSprite(String id, ByteBuffer pixels) {
        if (!sprites.containsKey(id)) {
            if (unallocated.size() == 0) {
                allocateSheet();
            }
            sprites.put(id, unallocated.remove(0));
        }
        SpriteInfo sprite = sprites.get(id);

        try (OpenGL.With ctx = LegacyRenderContext.INSTANCE.apply(new RenderState().texture(new Texture(sprite.texID)))) {
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, sprite.uPx, sprite.vPx, spriteSize, spriteSize, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, pixels);
        }
    }

    /** Render the sprite represented by id (skip if unknown) */
    public void renderSprite(String id) {
        SpriteInfo sprite = sprites.get(id);
        if (sprite == null) {
            return;
        }
        RenderState state = new RenderState()
                .texture(new Texture(sprite.texID))
                .rotate(180, 1, 0, 0)
                .translate(0, -1, 0);
        try (OpenGL.With ctx = LegacyRenderContext.INSTANCE.apply(state)) {
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glColor4f(1, 1, 1, 1);
            GL11.glTexCoord2f(sprite.uMin, sprite.vMin);
            GL11.glVertex3f(0, 0, 0);
            GL11.glTexCoord2f(sprite.uMin, sprite.vMax);
            GL11.glVertex3f(0, 1, 0);
            GL11.glTexCoord2f(sprite.uMax, sprite.vMax);
            GL11.glVertex3f(1, 1, 0);
            GL11.glTexCoord2f(sprite.uMax, sprite.vMin);
            GL11.glVertex3f(1, 0, 0);
            GL11.glEnd();
        };
    }

    /** Remove a sprite from the sheet (does not reduce used GPU memory yet) */
    public void freeSprite(String id) {
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
        final int texID;

        private SpriteInfo(float u, float uMax, int uPx, float v, float vMax, int vPx, int texID) {
            this.uMin = u;
            this.uMax = uMax;
            this.uPx = uPx;
            this.vMin = v;
            this.vMax = vMax;
            this.vPx = vPx;
            this.texID = texID;
        }
    }
}
