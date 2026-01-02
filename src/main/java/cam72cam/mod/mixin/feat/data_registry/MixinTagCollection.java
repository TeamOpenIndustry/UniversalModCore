package cam72cam.mod.mixin.feat.data_registry;

import cam72cam.mod.event.platform.RegisterBlockTagEvent;
import cam72cam.mod.event.platform.RegisterItemTagEvent;
import net.minecraft.resources.IResourceManager;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagCollection;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
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

    @Inject(method = "lambda$reload$3", at = @At("RETURN"), remap = false)
    public void onRegisterTag(IResourceManager p_lambda$reload$3_1_, CallbackInfoReturnable<Map<ResourceLocation, Tag.Builder<?>>> cir) {
        Map<ResourceLocation, Tag.Builder<?>> map = cir.getReturnValue();
        if (this.resourceLocationPrefix.contains("block")) {
            RegisterBlockTagEvent event = new RegisterBlockTagEvent(map);
            ModLoader.get().postEvent(event);
        } else if (this.resourceLocationPrefix.contains("item")) {
            RegisterItemTagEvent event = new RegisterItemTagEvent(map);
            ModLoader.get().postEvent(event);
        }
    }
}
