package cam72cam.mod.event.mixin;

import cam72cam.mod.ModCore;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.resource.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureManager.class)
public class TextureManagerMixin {
    @Inject(method = "<init>", at=@At(value = "RETURN"))
    public void init(ResourceManager manager, CallbackInfo info) {
        ModCore.instance.preInit();
    }
}
