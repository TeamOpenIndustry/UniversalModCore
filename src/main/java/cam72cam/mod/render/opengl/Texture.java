package cam72cam.mod.render.opengl;

import cam72cam.mod.resource.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.SimpleTexture;

public class Texture {
    public final static Texture NO_TEXTURE = new Texture(-1);

    public final int textureId;

    public Texture(int textureId) {
        this.textureId = textureId;
    }

    public Texture(Identifier id) {
        ITextureObject tex = Minecraft.getMinecraft().getTextureManager().getTexture(id.internal);
        //noinspection ConstantConditions
        if (tex == null) {
            Minecraft.getMinecraft().getTextureManager().loadTexture(id.internal, new SimpleTexture(id.internal));
            tex = Minecraft.getMinecraft().getTextureManager().getTexture(id.internal);
        }
        this.textureId = tex.getGlTextureId();
    }
}
