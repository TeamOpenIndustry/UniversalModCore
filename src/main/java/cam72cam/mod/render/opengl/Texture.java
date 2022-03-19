package cam72cam.mod.render.opengl;

import cam72cam.mod.resource.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.SimpleTexture;

public interface Texture {
    Texture NO_TEXTURE = Texture.wrap(-1);

    int getId();

    static Texture wrap(int id) {
        return () -> id;
    }

    static Texture wrap(Identifier id) {
        ITextureObject tex = Minecraft.getMinecraft().getTextureManager().getTexture(id.internal);
        //noinspection ConstantConditions
        if (tex == null) {
            Minecraft.getMinecraft().getTextureManager().loadTexture(id.internal, new SimpleTexture(id.internal));
            tex = Minecraft.getMinecraft().getTextureManager().getTexture(id.internal);
        }
        return tex::getGlTextureId;
    }
}
