package cam72cam.mod.mixin.accessor;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public interface ATextureAtlasSprite {
    int getAtlasWidth();
    int getAtlasHeight();

    static ATextureAtlasSprite from(TextureAtlasSprite sprite) {
        return (ATextureAtlasSprite) sprite;
    }
}
