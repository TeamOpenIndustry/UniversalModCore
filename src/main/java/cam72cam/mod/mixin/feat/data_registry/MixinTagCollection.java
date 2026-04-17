package cam72cam.mod.mixin.feat.data_registry;

import cam72cam.mod.event.platform.RegisterBlockTagEvent;
import cam72cam.mod.event.platform.RegisterItemTagEvent;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.resources.IResourceManager;
import net.minecraft.tags.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.ModLoader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

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

    @WrapMethod(method = "prepare")
    public CompletableFuture<Map<ResourceLocation, ITag.Builder>> register(IResourceManager p_242224_1_, Executor p_242224_2_, Operation<CompletableFuture<Map<ResourceLocation, ITag.Builder>>> original) {
        CompletableFuture<Map<ResourceLocation, ITag.Builder>> target = original.call(p_242224_1_, p_242224_2_);
        target.thenAccept(map -> {
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
        });

        return target;
    }
}