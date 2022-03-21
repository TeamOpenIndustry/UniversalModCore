package cam72cam.mod.render.opengl;

import cam72cam.mod.ModCore;
import cam72cam.mod.resource.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;

import java.awt.image.BufferedImage;
import java.io.IOException;

public interface Texture {
    Texture NO_TEXTURE = Texture.wrap(-1);

    int getId();

    static Texture wrap(int id) {
        return () -> id;
    }

    static Texture wrap(Identifier id) {
        TextureManager texManager = Minecraft.getMinecraft().getTextureManager();

        ITextureObject tex = texManager.getTexture(id.internal);
        //noinspection ConstantConditions
        if (tex == null) {
            try {
                IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(id.internal);
                texManager.loadTexture(id.internal, new SimpleTexture(id.internal));
                tex = texManager.getTexture(id.internal);
            } catch (Exception ex) {
                // Pass
            }

            if (tex == null) {
                // Fallback to zips
                texManager.loadTexture(id.internal, new AbstractTexture() {
                    @Override
                    public void loadTexture(IResourceManager resourceManager) throws IOException {
                        BufferedImage bufferedimage = TextureUtil.readBufferedImage(id.getResourceStream());
                        TextureUtil.uploadTextureImageAllocate(this.getGlTextureId(), bufferedimage, false, false);
                    }
                });
                tex = texManager.getTexture(id.internal);
            }
        }
        return tex::getGlTextureId;
    }
}
