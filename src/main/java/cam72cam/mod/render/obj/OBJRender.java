package cam72cam.mod.render.obj;

import cam72cam.mod.Config;
import cam72cam.mod.model.obj.OBJModel;
import cam72cam.mod.render.OpenGL;
import cam72cam.mod.render.VBO;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static cam72cam.mod.model.obj.ImageUtils.scaleSize;

/**
 * VBA/VBO Backed object renderer
 */
public class OBJRender {
    public OBJModel model;
    public Map<String, OBJTextureSheet> textures = new HashMap<>();
    public Map<String, OBJTextureSheet> icons = new HashMap<>();
    private VBO vbo;

    public OBJRender(OBJModel model) {
        this(model, 30);
    }
    public OBJRender(OBJModel model, int cacheSeconds) {
        this.model = model;
        for (String name : model.textures.keySet()) {
            this.textures.put(name, new OBJTextureSheet(model.textureWidth, model.textureHeight, model.textures.get(name), cacheSeconds));
            Pair<Integer, Integer> size = scaleSize(model.textureWidth, model.textureHeight, Config.MaxTextureSize/8);
            this.icons.put(name, new OBJTextureSheet(size.getLeft(), size.getRight(), model.icons.get(name), cacheSeconds));
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

        if (icon) {
            OBJTextureSheet tex = this.icons.get(texName);
            return tex.bind();
        } else {
            OBJTextureSheet tex = this.textures.get(texName);
            return tex.bind();
        }
    }

    public VBO.BoundVBO bind() {
        return getVBO().bind();
    }

    public void draw() {
        try (VBO.BoundVBO vbo = bind()) {
            vbo.draw();
        }
    }

    public void drawGroups(Collection<String> groups) {
        try (VBO.BoundVBO vbo = bind()) {
            vbo.draw(groups);
        }
    }

    public VBO getVBO() {
        if (vbo != null) {
            return vbo;
        }
        vbo = new VBO(model);
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
