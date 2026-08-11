package cam72cam.mod.model.common.format;

import cam72cam.mod.model.common.material.Material;
import cam72cam.mod.model.common.mesh.IModelBuilder;
import cam72cam.mod.resource.Identifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class OBJParser {
    public static void parse(Identifier modelLoc, IModelBuilder builder) {
        Map<String, Material> materials = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(modelLoc.getLastResourceStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] args = line.split(" ");
                switch (args[0]) {
                    case "mtllib":
                        parseMTL(modelLoc, args[1], materials);
                        break;
                    case "usemtl":
                        builder.setCurrentMaterial(materials.computeIfAbsent(args.length > 1 ? line.substring(7) : "undefined", Material::new));
                        break;
                    case "o":
                    case "g":
                        builder.newModelGroup(args.length > 1 ? line.substring(2) : "default");
                        break;
                    case "v":
                        builder.addIndexedVert(Float.parseFloat(args[1]), Float.parseFloat(args[2]), Float.parseFloat(args[3]));
                        break;
                    case "vt":
                        builder.addIndexedUv(Float.parseFloat(args[1]), Float.parseFloat(args[2]));
                        break;
                    case "vn":
                        builder.addIndexedNormal(Float.parseFloat(args[1]), Float.parseFloat(args[2]), Float.parseFloat(args[3]));
                        break;
                    case "f":
                        IModelBuilder.IFaceBuilder face = builder.newFace();
                        for (int i = 1; i < args.length; i++) {
                            String[] part = args[i].split("/");
                            int pos = part.length > 0 && !part[0].isEmpty() ? Integer.parseInt(part[0]) - 1 : -1;
                            int uv = part.length > 1 && !part[1].isEmpty() ? Integer.parseInt(part[1]) - 1 : -1;
                            int nrm = part.length > 2 && !part[2].isEmpty() ? Integer.parseInt(part[2]) - 1 : -1;
                            face.addVert(pos, uv, nrm);
                        }
                        face.end();
                        break;
                    case "s":
                        if (args.length == 2 && args[1].equals("1")) {
                            // Technically this should be for every group, but this is a close enough approximation
                            builder.doSmoothShading();
                        }
                        break;
                    case "l":
                        // Ignore
                        // TODO might be able to use this for details
                        break;
                    default:
                        //System.out.println("OBJ: ignored line '" + line + "'");
                        break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse OBJ " + modelLoc, e);
        }
    }

    private static void parseMTL(Identifier modelLoc, String path, Map<String, Material> materials) {
        Identifier mtlId = modelLoc.getRelative(path);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(mtlId.getLastResourceStream(), StandardCharsets.UTF_8))) {
            String name = null;
            float r = 1, g = 1, b = 1, a = 1;
            String texKd = null;
            String texNs = null;
            String texBump = null;

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split(" ");
                switch (parts[0]) {
                    case "newmtl":
                        if (name != null) {
                            materials.put(name, buildMaterial(modelLoc, name, r, g, b, a, texKd, texNs, texBump));
                        }
                        name = line.substring(7);
                        r = g = b = a = 1;
                        texKd = null;
                        break;
                    case "Ka":
                        break;
                    case "Kd":
                        r = Float.parseFloat(parts[1]);
                        g = Float.parseFloat(parts[2]);
                        b = Float.parseFloat(parts[3]);
                        a = parts.length > 4 ? Float.parseFloat(parts[4]) : 1;
                        break;
                    case "Ks":
                        break;
                    case "map_Kd":
                        texKd = parts[1];
                        break;
                    case "map_Ns":
                        texNs = parts[1];
                        break;
                    case "map_Bump":
                        texBump = parts[1];
                        break;
                    case "Ns":
                        //Ignore
                        break;
                    case "Ke":
                        //Ignore
                        break;
                    case "Ni":
                        //Ignore
                        break;
                    case "d":
                        //ignore
                        break;
                    case "illum":
                        //ignore
                        break;
                    default:
                        //System.out.println("MTL: ignored line '" + line + "'");
                        break;
                }
            }
            if (name != null) {
                materials.put(name, buildMaterial(modelLoc, name, r, g, b, a, texKd, texNs, texBump));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse MTL " + mtlId, e);
        }
    }

    private static Material buildMaterial(Identifier modelLoc, String name,
                                          float r, float g, float b, float a,
                                          String texKd, String texNs, String texBump) {
        Material material = new Material(name, r, g, b, a);
        if (texKd != null) {
            Identifier texId = modelLoc.getRelative(texKd);
            material.texture(texId).defaultSpecular().defaultNormal();
        }
        if (texNs != null) {
            Identifier texId = modelLoc.getRelative(texNs);
            // Override
            material.specular(texId);
        }
        if (texBump != null) {
            Identifier texId = modelLoc.getRelative(texBump);
            // Override
            material.normal(texId);
        }
        return material;
    }
}
