package cam72cam.mod.model.common.material;

import cam72cam.mod.ModCore;
import cam72cam.mod.model.common.mesh.IModelBuilder;
import cam72cam.mod.resource.Identifier;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.util.Iterator;

public class Material {
    public final IModelBuilder builder;
    public final String name;
    public final float r, g, b, a;
    public int width, height;

    //Relative paths from model
    public String texAlbedo;
    public String texSpecular;
    public String texNormal;

    //For tex repacking
    public int copiesOnU = 1;
    public int copiesOnV = 1;

    public Material(IModelBuilder builder, String name) {
        this(builder, name, 1, 1, 1, 1);
    }

    public Material(IModelBuilder builder, String name, float r, float g, float b, float a) {
        this.builder = builder;
        this.name = name;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;

        this.width = 32;
        this.height = 32;
    }

    /** Reads the albedo texture's dimensions from its image metadata, falling back to the default size on failure. */
    public void populateSize() {
        if (this.texAlbedo != null) {
            Identifier relative = builder.getModelLoc().getRelative(this.texAlbedo);
            if (relative.canLoad()) {
                try (ImageInputStream iis = ImageIO.createImageInputStream(builder.open(relative))) {
                    Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
                    if (readers.hasNext()) {
                        ImageReader reader = readers.next();
                        try {
                            // Read size from texture metadata
                            reader.setInput(iis, true, true);
                            this.width = reader.getWidth(0);
                            this.height = reader.getHeight(0);
                        } finally {
                            reader.dispose();
                        }
                    }
                } catch (Exception ignored) {
                    ModCore.warn("Unable to populate texture at %s, do you have a broken pack? This material is set to default size of 32!", relative);
                }
            }
        }
    }

    public Material setAlbedo(String path) {
        this.texAlbedo = path;
        return this;
    }

    public Material defaultSpecular() {
        if (texAlbedo != null) {
            String ext = FilenameUtils.getExtension(texAlbedo);
            String path = FilenameUtils.getPath(texAlbedo);
            String name = FilenameUtils.getBaseName(texAlbedo);
            this.texSpecular = path + name + "_s." + ext;
        }
        return this;
    }
    public Material setSpecular(String path) {
        this.texSpecular = path;
        return this;
    }

    public Material defaultNormal() {
        if (texAlbedo != null) {
            String ext = FilenameUtils.getExtension(texAlbedo);
            String path = FilenameUtils.getPath(texAlbedo);
            String name = FilenameUtils.getBaseName(texAlbedo);
            this.texNormal = path + name + "_n." + ext;
        }
        return this;
    }
    public Material setNormal(String path) {
        this.texNormal = path;
        return this;
    }
}
