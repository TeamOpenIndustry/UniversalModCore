package cam72cam.mod.render.obj;

import cam72cam.mod.Config;
import cam72cam.mod.model.obj.OBJModel;
import cam72cam.mod.render.OpenGL;
import cam72cam.mod.render.obj.OBJVBO.BoundOBJVBO;
import cam72cam.mod.serialization.ResourceCache;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static cam72cam.mod.model.obj.ImageUtils.scaleSize;

/**
 * VBA/VBO Backed object renderer
 */
public class OBJRender {
    private static final OBJTextureSheet defTex = new OBJTextureSheet(1, 1, () -> new ResourceCache.GenericByteBuffer(new int[] { 0x0000FF }), Integer.MAX_VALUE);
    public OBJModel model;
    public Map<String, OBJTextureSheet> textures = new HashMap<>();
    public Map<String, OBJTextureSheet> icons = new HashMap<>();
    private OBJVBO vbo;

    public OBJRender(OBJModel model) {
        this(model, 30);
    }
    public OBJRender(OBJModel model, int cacheSeconds) {
        this.model = model;
        for (String name : model.textures.keySet()) {
            Pair<Integer, Integer> size = scaleSize(model.textureWidth, model.textureHeight, Config.getMaxTextureSize()/8);
            if (icons.containsKey(name)) {
                this.icons.put(name, new OBJTextureSheet(size.getLeft(), size.getRight(), model.icons.get(name), cacheSeconds, defTex));
            }
            this.textures.put(name, new OBJTextureSheet(model.textureWidth, model.textureHeight, model.textures.get(name), cacheSeconds, this.icons.getOrDefault(name, defTex)));
        }
    }

    public OpenGL.With bindTexture() {
        return bindTexture(null);
    }

    public OpenGL.With bindTexture(boolean icon) {
        return bindTexture(null, icon);
    }

    public OpenGL.With bindTexture(String texName) {
        return bindTexture(texName, false);
    }

    public OpenGL.With bindTexture(String texName, boolean icon) {
        if (this.textures.get(texName) == null) {
            texName = ""; // Default
        }

        if (icon && icons.containsKey(texName)) {
            OBJTextureSheet tex = this.icons.get(texName);
            return tex.bind().and(OpenGL.shading(model.isSmoothShading));
        } else {
            OBJTextureSheet tex = this.textures.get(texName);
            return tex.bind().and(OpenGL.shading(model.isSmoothShading));
        }
    }

    public BoundOBJVBO bind() {
        return getVBO().bind();
    }

    public void draw() {
        try (BoundOBJVBO vbo = bind()) {
            vbo.draw();
        }
    }

    public void drawGroups(Collection<String> groups) {
        try (BoundOBJVBO vbo = bind()) {
            vbo.draw(groups);
        }
    }

    public OBJVBO getVBO() {
        if (vbo != null) {
            return vbo;
        }
        vbo = new OBJVBO(model);
        return vbo;
    }

    public void free() {
        for (OBJTextureSheet texture : textures.values()) {
            texture.freeGL();
        }
        for (OBJTextureSheet texture : icons.values()) {
            texture.freeGL();
        }
        if (vbo != null) {
            vbo.free();
        }
    }

}
