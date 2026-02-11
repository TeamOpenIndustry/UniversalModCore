package cam72cam.mod.mixin.feat.data_registry;

import cam72cam.mod.event.platform.RegisterBlockTagEvent;
import cam72cam.mod.event.platform.RegisterItemTagEvent;
import net.minecraft.resources.IResourceManager;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagCollection;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.ModLoader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(TagCollection.class)
public class MixinTagCollection {
    @Shadow
    @Final
    private String resourceLocationPrefix;

    @Inject(method = "lambda$reload$3", at = @At("RETURN"))
    public void postTagReload(IResourceManager manager, CallbackInfoReturnable<Map<ResourceLocation, Tag.Builder<?>>> cir) {
        Map<ResourceLocation, Tag.Builder<?>> map = cir.getReturnValue();
        switch (this.resourceLocationPrefix) {
            case "tags/blocks":
                RegisterBlockTagEvent blockTagEvent = new RegisterBlockTagEvent(map);
                ModLoader.get().postEvent(blockTagEvent);
                return;
            case "tags/items":
                RegisterItemTagEvent itemTagEvent = new RegisterItemTagEvent(map);
                ModLoader.get().postEvent(itemTagEvent);
                return;
            default:
                //Waiting for more...
        }
    }
}