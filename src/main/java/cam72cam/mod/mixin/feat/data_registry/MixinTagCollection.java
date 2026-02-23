package cam72cam.mod.mixin.feat.data_registry;

import cam72cam.mod.event.platform.RegisterBlockTagEvent;
import cam72cam.mod.event.platform.RegisterItemTagEvent;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.tags.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.ModLoader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * Used for posting <code>RegisterBlockTagEvent</code> and <code>RegisterItemTagEvent</code>
 * @see RegisterBlockTagEvent
 * @see RegisterItemTagEvent
 */
@Mixin(TagCollectionReader.class)
public class MixinTagCollection {
    @Shadow
    @Final
    private String directory;

    @Inject(method = "load", at = @At("HEAD"))
    public void onRegisterTag(Map<ResourceLocation, ITag.Builder> p_242226_1_, CallbackInfoReturnable<ITagCollection<?>> cir,
                              @Local(argsOnly = true) Map<ResourceLocation, ITag.Builder> map) {
        switch (this.directory) {
            //Change me when minecraft version changes
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