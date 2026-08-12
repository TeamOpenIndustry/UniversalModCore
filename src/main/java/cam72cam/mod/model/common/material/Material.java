package cam72cam.mod.model.common.material;

import cam72cam.mod.resource.Identifier;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.util.Iterator;

public class Material {
    public final String name;
    public final float r, g, b, a;
    public int width, height;

    public Identifier modelLoc;
    //Relative paths from model
    public String texAlbedo;
    public String texSpecular;
    public String texNormal;

    public int copiesOnU = 1;
    public int copiesOnV = 1;

    public Material(Identifier modelLoc, String name) {
        this(modelLoc, name, 1, 1, 1, 1);
    }

    public Material(Identifier modelLoc, String name, float r, float g, float b, float a) {
        this.modelLoc = modelLoc;
        this.name = name;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;

        this.width = 16;
        this.height = 16;
    }

    public void populateSize() {
        if (texAlbedo != null) {
            Identifier relative = modelLoc.getRelative(texAlbedo);
            if (relative.canLoad()) {
                try (ImageInputStream iis = ImageIO.createImageInputStream(
                    relative.getLastResourceStream())) {
                    Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
                    if (readers.hasNext()) {
                        ImageReader reader = readers.next();
                        try {
                            reader.setInput(iis, true, true);
                            this.width = reader.getWidth(0);
                            this.height = reader.getHeight(0);
                        } finally {
                            reader.dispose();
                        }
                    }
                } catch (Exception ignored) {
                    //Unable to load, fall back to 16*16
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
            this.texNormal = path + name + "_s." + ext;
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
