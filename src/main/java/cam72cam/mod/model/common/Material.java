package cam72cam.mod.model.common;

import cam72cam.mod.render.opengl.Texture;
import cam72cam.mod.resource.Identifier;

public class Material {
    public static final Material UNDEFINED = new Material("undefined", new Identifier(), null, null, new float[]{1, 1, 1, 1});

    public final String name;

    public final Identifier albedo;
    public final Identifier specular;
    public final Identifier normal;
    public final float r;
    public final float g;
    public final float b;
    public final float a;

    public Material(String name, Identifier albedo, Identifier specular, Identifier normal, float[] defaultColor) {
        this(name, albedo, specular, normal, defaultColor[0], defaultColor[1], defaultColor[2], defaultColor[3]);
    }

    public Material(String name, Identifier albedo, Identifier specular, Identifier normal, float r, float g, float b, float a) {
        this.name = name;
        this.albedo = albedo;
        this.specular = specular;
        this.normal = normal;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public boolean hasPBR() {
        return specular != null && normal != null;
    }
}
