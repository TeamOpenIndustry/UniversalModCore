package cam72cam.mod.render.opengl;

import cam72cam.mod.resource.Identifier;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

public interface Texture {
    Texture NO_TEXTURE = Texture.wrap(new Identifier(TextureManager.INTENTIONAL_MISSING_TEXTURE));

    GpuTextureView getTexView();

    static Texture wrap(Identifier id) {
        return new MinecraftTexture(id);
    }

    static GpuTextureView getTexView(ResourceLocation loc) {
        return Minecraft.getInstance().getTextureManager().getTexture(loc).getTextureView();
    }
}
