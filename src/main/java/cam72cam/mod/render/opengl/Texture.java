package cam72cam.mod.render.opengl;

import cam72cam.mod.resource.Identifier;

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
