package cam72cam.mod.model.common.format;

import cam72cam.mod.model.common.material.Material;
import cam72cam.mod.model.common.mesh.IModelBuilder;
import cam72cam.mod.model.common.util.MalformedModelException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple Metasequoia (MQO) {@link Parser} implementation, supports basic geometry data parsing.
 *
 * <p>Only the text format (Version 1.0 - 1.2) is handled. Chunks that don't contribute to
 * rendered geometry (Scene, vertex attributes, BVertex, Blob, ...) are skipped.</p>
 */
public class MQOParser {
    public static final String EXTENSION = "mqo";
    // Matches "key(value)" attributes on Material and face lines
    private static final Pattern ATTR = Pattern.compile("([A-Za-z_]+)\\(([^)]*)\\)");

    public static void parse(final IModelBuilder builder) throws IOException {
        List<Material> materials = new ArrayList<>();
        materials.add(new Material(builder, "undefined"));

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(builder.open(builder.getModelLoc()), StandardCharsets.UTF_8))) {
            // Header: "Metasequoia Document" followed by "Format Text Ver 1.0"
            String line = reader.readLine();
            if (line == null || !line.trim().equalsIgnoreCase("Metasequoia Document")) {
                throw new MalformedModelException(String.format("Model %s is not a Metasequoia document", builder.getModelLoc()));
            }
            line = reader.readLine();
            if (line == null || !line.trim().toLowerCase(Locale.ROOT).startsWith("format text")) {
                throw new MalformedModelException(String.format("Unsupported MQO format in %s, only the text format is supported", builder.getModelLoc()));
            }

            int vertexBase = -1;
            int objectIndex = 0;
            boolean inMaterial = false, inVertex = false, inFace = false;

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (trimmed.equals("}")) {
                    // Closes the innermost open chunk (Material, vertex, face or Object)
                    inMaterial = inVertex = inFace = false;
                    continue;
                }

                if (inVertex) {
                    String[] pos = trimmed.split("\\s+");
                    int index = builder.addIndexedVert(Float.parseFloat(pos[0]), Float.parseFloat(pos[1]), Float.parseFloat(pos[2]));
                    if (vertexBase < 0) {
                        vertexBase = index;
                    }
                    continue;
                }

                if (inFace) {
                    if (vertexBase < 0) {
                        throw new MalformedModelException(String.format("MQO model %s has faces before any vertex data", builder.getModelLoc()));
                    }
                    // "4 V(0 1 2 3) M(0) UV(0 0 1 0 1 1 0 1)"
                    Map<String, String> f = attrs(trimmed);
                    String[] verts = f.get("v").trim().split("\\s+");
                    String[] uvs = f.containsKey("uv") ? f.get("uv").trim().split("\\s+") : null;
                    int mat = f.containsKey("m") ? Integer.parseInt(f.get("m")) : -1;
                    //Index 0 is default
                    builder.setCurrentMaterial(mat >= 0 && mat < materials.size() ? materials.get(mat + 1) : materials.get(0));

                    IModelBuilder.IFaceBuilder face = builder.newFace();
                    for (int i = 0; i < verts.length; i++) {
                        int uvIdx = -1;
                        if (uvs != null && i * 2 + 1 < uvs.length) {
                            // MQO's UV origin is at the top-left, flip V into the OpenGL/OBJ convention
                            float u = Float.parseFloat(uvs[i * 2]);
                            float v = 1 - Float.parseFloat(uvs[i * 2 + 1]);
                            uvIdx = builder.addIndexedUv(u, v);
                        }
                        // MQO has no explicit normals
                        face.addVert(vertexBase + Integer.parseInt(verts[i]), uvIdx, -1);
                    }
                    face.end();
                    continue;
                }

                if (inMaterial) {
                    // "name" shader(3) col(1 1 1 1) tex("t.png") bump("b.png")
                    String rest = trimmed;
                    String name;
                    if (rest.startsWith("\"")) {
                        int end = rest.indexOf('"', 1);
                        end = end >= 0 ? end : rest.length();
                        name = rest.substring(1, end);
                        rest = rest.substring(end);
                    } else {
                        int end = 0;
                        while (end < rest.length() && !Character.isWhitespace(rest.charAt(end))) {
                            end++;
                        }
                        name = rest.substring(0, end);
                        rest = rest.substring(end);
                    }
                    Map<String, String> mat = attrs(rest);
                    float r = 1, g = 1, b = 1, a = 1;
                    if (mat.containsKey("col")) {
                        String[] col = mat.get("col").trim().split("\\s+");
                        if (col.length >= 3) {
                            r = Float.parseFloat(col[0]);
                            g = Float.parseFloat(col[1]);
                            b = Float.parseFloat(col[2]);
                            a = col.length > 3 ? Float.parseFloat(col[3]) : 1;
                        }
                    }
                    Material material = new Material(builder, name, r, g, b, a);
                    String tex = unquote(mat.getOrDefault("tex", "")).replace('\\', '/');
                    if (!tex.isEmpty()) {
                        material.setAlbedo(tex).defaultSpecular().defaultNormal();
                    }
                    String bump = unquote(mat.getOrDefault("bump", "")).replace('\\', '/');
                    if (!bump.isEmpty()) {
                        material.setNormal(bump);
                    }
                    materials.add(material);
                    continue;
                }

                String token = trimmed.split("\\s+", 2)[0];
                if (token.equalsIgnoreCase("Material")) {
                    inMaterial = true;
                } else if (token.equalsIgnoreCase("Object")) {
                    String rest = trimmed.substring("Object".length()).trim();
                    int brace = rest.lastIndexOf('{');
                    if (brace >= 0) {
                        rest = rest.substring(0, brace).trim();
                    }
                    String name = unquote(rest);
                    builder.newModelGroup(name.isEmpty() ? "object" + objectIndex++ : name);
                    vertexBase = -1;
                } else if (token.equalsIgnoreCase("vertex")) {
                    inVertex = true;
                } else if (token.equalsIgnoreCase("face")) {
                    inFace = true;
                } else if (token.equalsIgnoreCase("shading")) {
                    // 0 = flat shading, 1 = Gouraud shading
                    String[] parts = trimmed.split("\\s+");
                    if (parts.length > 1 && !parts[1].equals("0")) {
                        builder.doSmoothShading();
                    }
                } else if (token.equalsIgnoreCase("Eof")) {
                    break;
                } else if (trimmed.endsWith("{")) {
                    // Unsupported nested chunk (Scene, BVertex, vertexattr, ...), skip to its matching brace
                    int depth = 1;
                    String nested;
                    while (depth > 0 && (nested = reader.readLine()) != null) {
                        for (int i = 0; i < nested.length(); i++) {
                            char c = nested.charAt(i);
                            if (c == '{') {
                                depth++;
                            } else if (c == '}') {
                                depth--;
                            }
                        }
                    }
                }
            }
        }
    }

    /** Extracts all <code>key(value)</code> pairs from a line. */
    private static Map<String, String> attrs(String line) {
        Map<String, String> result = new HashMap<>();
        Matcher matcher = ATTR.matcher(line);
        while (matcher.find()) {
            result.put(matcher.group(1).toLowerCase(Locale.ROOT), matcher.group(2).trim());
        }
        return result;
    }

    private static String unquote(String s) {
        s = s.trim();
        if (s.length() >= 2 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
}
