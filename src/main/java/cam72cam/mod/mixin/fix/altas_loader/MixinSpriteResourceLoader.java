package cam72cam.mod.mixin.fix.altas_loader;

import cam72cam.mod.event.platform.TextureStitchEvent;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Trigger our own sprite loader
 */
@Mixin(SpriteSourceList.class)
public class MixinSpriteResourceLoader {
    @Inject(method = "load", at = @At("RETURN"))
    private static void inject(ResourceManager manager, ResourceLocation p_261709_, CallbackInfoReturnable<SpriteResourceLoader> cir,
                               @Local List<SpriteSource> list) {
        if(p_261709_.getPath().equals("blocks")) {
            //Only hack into main sprite
            TextureStitchEvent event = new TextureStitchEvent(list);
            net.neoforged.fml.ModLoader.get().postEvent(event);
        }
    }
}
