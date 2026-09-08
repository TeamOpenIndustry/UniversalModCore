package cam72cam.mod.mixin.feat.sprite_total_size;

import cam72cam.mod.mixin.accessor.ATextureAtlasSprite;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureAtlasSprite.class)
public class MixinTextureAtlasSprite implements ATextureAtlasSprite {
    @Unique
    public int atlasWidth;

    @Unique
    public int atlasHeight;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(ResourceLocation p_250211_, SpriteContents p_248526_, int totalX, int totalY, int startingX, int startingY, CallbackInfo ci) {
        this.atlasWidth = totalX;
        this.atlasHeight = totalY;
    }

    @Override
    public int getAtlasWidth() {
        return atlasWidth;
    }

    @Override
    public int getAtlasHeight() {
        return atlasHeight;
    }
}
