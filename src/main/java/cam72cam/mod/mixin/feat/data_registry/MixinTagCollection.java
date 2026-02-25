package cam72cam.mod.mixin.feat.data_registry;

import cam72cam.mod.event.platform.RegisterBlockTagEvent;
import cam72cam.mod.event.platform.RegisterItemTagEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagLoader;
import net.neoforged.fml.ModLoader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

/**
 * Used for posting <code>RegisterBlockTagEvent</code> and <code>RegisterItemTagEvent</code>
 * @see RegisterBlockTagEvent
 * @see RegisterItemTagEvent
 */
@Mixin(TagLoader.class)
public class MixinTagCollection {
    @Shadow
    @Final
    private String directory;

    @Inject(method = "load", at = @At("RETURN"))
    public void onRegisterTag(ResourceManager p_144496_, CallbackInfoReturnable<Map<ResourceLocation, List<TagLoader.EntryWithSource>>> cir) {
        Map<ResourceLocation, List<TagLoader.EntryWithSource>> map = cir.getReturnValue();
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