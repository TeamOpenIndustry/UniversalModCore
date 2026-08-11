package cam72cam.mod.model.common.material;

import cam72cam.mod.render.opengl.Texture;

import java.util.function.Supplier;

public class Material {
    public final String name;
    public final float r, g, b, a;

    private Supplier<Texture> texture;
    private Texture cached;

    public Material(String name) {
        this(name, 1, 1, 1, 1);
    }

    public Material(String name, float r, float g, float b, float a) {
        this.name = name;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    /** Texture is resolved lazily so the asset is only loaded when the model is actually rendered. */
    public Material texture(Supplier<Texture> texture) {
        this.texture = texture;
        return this;
    }

    public Texture getTexture() {
        if (cached == null && texture != null) {
            cached = texture.get();
        }
        return cached;
    }
}
