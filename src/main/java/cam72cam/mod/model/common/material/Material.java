package cam72cam.mod.model.common.material;

import cam72cam.mod.resource.Identifier;
import org.apache.commons.io.FilenameUtils;

public class Material {
    public final String name;
    public final float r, g, b, a;
    public int width, height;

    public Identifier baseColor;
    public Identifier specular;
    public Identifier normal;

    public Material(String name) {
        this(name, 1, 1, 1, 1);
    }

    public Material(String name, float r, float g, float b, float a) {
        this.name = name;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;

        this.width = 16;
        this.height = 16;
    }

    public Material texture(Identifier base) {
        this.baseColor = base;
        return this;
    }

    public Material defaultSpecular() {
        if (baseColor != null) {
            String ext = FilenameUtils.getExtension(baseColor.getPath());
            String name = FilenameUtils.getBaseName(baseColor.getPath());
            this.specular = baseColor.getRelative(name + "_s." + ext);
        }
        return this;
    }
    public Material specular(Identifier spec) {
        this.specular = spec;
        return this;
    }

    public Material defaultNormal() {
        if (baseColor != null) {
            String ext = FilenameUtils.getExtension(baseColor.getPath());
            String name = FilenameUtils.getBaseName(baseColor.getPath());
            this.normal = baseColor.getRelative(name + "_n." + ext);
        }
        return this;
    }
    public Material normal(Identifier normal) {
        this.normal = normal;
        return this;
    }
}
