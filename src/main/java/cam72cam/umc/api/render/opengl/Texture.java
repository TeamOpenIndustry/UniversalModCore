package cam72cam.umc.api.render.opengl;

import cam72cam.umc.api.resource.Identifier;

public interface Texture {
    Texture NO_TEXTURE = Texture.wrap(-1);

    int getId();

    static Texture wrap(int id) {
        return () -> id;
    }

    static Texture wrap(Identifier id) {
        return new MinecraftTexture(id);
    }
}
