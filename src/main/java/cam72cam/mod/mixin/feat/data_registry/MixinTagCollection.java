package cam72cam.mod.mixin.feat.data_registry;

import cam72cam.mod.event.platform.RegisterBlockTagEvent;
import cam72cam.mod.event.platform.RegisterItemTagEvent;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagLoader;
import net.neoforged.fml.ModLoader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

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

    @WrapMethod(method = "load")
    public Map<ResourceLocation, List<TagLoader.EntryWithSource>> onRegisterTag(ResourceManager p_144496_, Operation<Map<ResourceLocation, List<TagLoader.EntryWithSource>>> original) {
        Map<ResourceLocation, List<TagLoader.EntryWithSource>> map = original.call(p_144496_);
        switch (this.directory) {
            //Change me when minecraft version changes
            case "tags/block":
                RegisterBlockTagEvent blockTagEvent = new RegisterBlockTagEvent(map);
                ModLoader.postEvent(blockTagEvent);
                break;
            case "tags/item":
                RegisterItemTagEvent itemTagEvent = new RegisterItemTagEvent(map);
                ModLoader.postEvent(itemTagEvent);
                break;
            default:
                //Waiting for more...
        }
        return map;
    }
}