package cam72cam.mod.model.obj;

import cam72cam.mod.Config;
import cam72cam.mod.ModCore;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.ResourceCache;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class OBJBuilder {

    private final VertexBuffer vbo;
    private final List<OBJGroup> groups;
    private final OBJTexturePacker packer;
    private final int textureWidth;
    private final int textureHeight;
    private boolean smoothShading;

    public OBJBuilder(Identifier modelLoc, ResourceCache.ResourceProvider input, float scale, float darken, Collection<String> variants) throws IOException {
        if (variants == null) {
            variants = Collections.singleton("");
        }
        if (variants.isEmpty()) {
            variants.add("");
        }

        OBJParser parser = new OBJParser(new ByteArrayInputStream(input.apply(modelLoc)), scale);
        this.groups = parser.getGroups();
        this.vbo = parser.getBuffer();
        this.smoothShading = parser.isSmoothShading();

        if (Config.getMaxTextureSize() <= 0) {
            packer = null;
            textureWidth = -1;
            textureHeight = -1;
            return;
        }
        List<Material> materials = new ArrayList<>();
        for (String materialPath : parser.getMaterialLibraries()) {
            materials.addAll(MTLParser.parse(new ByteArrayInputStream(input.apply(modelLoc.getRelative(materialPath)))));
        }
        Map<String, Material> materialLookup = materials.stream().collect(Collectors.toMap(m -> m.name, m -> m));

        String[] faceMaterials = parser.getFaceMaterials();
        int colorOffset = vbo.colorOffset;
        int textureOffset = vbo.textureOffset;
        float mult = 1 - darken * 5;
        for (String materialName : faceMaterials) {
            if (materialName != null) {
                Material material = materialLookup.get(materialName);
                if (material == null) {
                    ModCore.warn("Unknown material '%s' in %s", materialName, modelLoc);
                    colorOffset += vbo.stride * 3;
                    textureOffset += vbo.stride * 3;
                    continue;
                }
                material.used = true;

                float vminU = 0;
                float vminV = 0;
                float vmaxU = 0;
                float vmaxV = 0;

                for (int point = 0; point < 3; point++) {
                    int pointOffset = point * vbo.stride;

                    vbo.data[colorOffset + pointOffset + 0] = material.hasTexture() ? material.KdR * mult : 1;
                    vbo.data[colorOffset + pointOffset + 1] = material.hasTexture() ? material.KdG * mult : 1;
                    vbo.data[colorOffset + pointOffset + 2] = material.hasTexture() ? material.KdB * mult : 1;
                    vbo.data[colorOffset + pointOffset + 3] = material.KdA;

                    float u = vbo.data[textureOffset + pointOffset + 0];
                    float v = vbo.data[textureOffset + pointOffset + 1];

                    //System.out.println(String.format("u: %s v:%s", u, v));
                    if (point == 0) {
                        vminU = u;
                        vmaxU = u;
                        vminV = v;
                        vmaxV = v;
                    } else {
                        vminU = Math.min(vminU, u);
                        vmaxU = Math.max(vmaxU, u);
                        vminV = Math.min(vminV, v);
                        vmaxV = Math.max(vmaxV, v);
                    }
                }
                if (material.hasTexture() && vminU != OBJParser.UNSPECIFIED && vminV != OBJParser.UNSPECIFIED) {
                    int offsetU = (int) Math.floor(vminU);
                    int offsetV = (int) Math.floor(vminV);
                    // "Normalize" uv coordinates to start between 0 and 1 and repeat into positive integer space
                    for (int point = 0; point < 3; point++) {
                        int pointOffset = point * vbo.stride;
                        vbo.data[textureOffset + pointOffset + 0] -= offsetU;
                        vbo.data[textureOffset + pointOffset + 1] -= offsetV;
                    }

                    material.copiesU = Math.max(material.copiesU, (int) Math.ceil(vmaxU - offsetU));
                    material.copiesV = Math.max(material.copiesV, (int) Math.ceil(vmaxV - offsetV));
                } else {
                    for (int point = 0; point < 3; point++) {
                        int pointOffset = point * vbo.stride;
                        vbo.data[textureOffset + pointOffset + 0] = 0.5f;
                        vbo.data[textureOffset + pointOffset + 1] = 0.5f;
                    }
                }
            }
            colorOffset += vbo.stride * 3;
            textureOffset += vbo.stride * 3;
        }

        OBJTexturePacker packer = new OBJTexturePacker(
                modelLoc,
                modelLoc::getRelative,
                path -> new ByteArrayInputStream(input.apply(modelLoc.getRelative(path))),
                materialLookup.values(),
                variants
        );
        this.packer = packer;
        textureOffset = vbo.textureOffset;
        for (String materialName : faceMaterials) {
            if (materialName != null) {
                OBJTexturePacker.UVConverter converter = packer.converters.get(materialName);
                if (converter != null) {
                    for (int point = 0; point < 3; point++) {
                        int pointOffset = point * vbo.stride;
                        vbo.data[textureOffset + pointOffset + 0] = converter.convertU(vbo.data[textureOffset + pointOffset + 0]);
                        // This is where we flip V
                        vbo.data[textureOffset + pointOffset + 1] = converter.convertV(vbo.data[textureOffset + pointOffset + 1]);
                    }
                }
            }
            textureOffset += vbo.stride * 3;
        }
        this.textureWidth = packer.getWidth();
        this.textureHeight = packer.getHeight();
    }

    public VertexBuffer vertexBufferObject() {
        return vbo;
    }
    public Map<String, Supplier<BufferedImage>> getTextures() {
        return packer != null ? packer.textures : Collections.emptyMap();
    }
    public Map<String, Supplier<BufferedImage>> getNormals() {
        return packer != null ? packer.normals : Collections.emptyMap();
    }
    public Map<String, Supplier<BufferedImage>> getSpeculars() {
        return packer != null ? packer.speculars : Collections.emptyMap();
    }
    public List<OBJGroup> getGroups() {
        return groups;
    }

    public int getTextureWidth() {
        return textureWidth;
    }

    public int getTextureHeight() {
        return textureHeight;
    }
    public boolean isSmoothShading() {
        return smoothShading;
    }
}
